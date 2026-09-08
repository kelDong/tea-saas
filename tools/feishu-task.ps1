# =============================================================
# 飞书任务同步脚本 v2（tea-saas 项目管理）
# 用法：
#   .\feishu-task.ps1 init                          # 测试连接（验证凭据）
#   .\feishu-task.ps1 create -Title "M2:SQL初始化脚本" [-Desc "36张表+种子数据"]
#   .\feishu-task.ps1 complete -Title "M2:SQL初始化脚本"
#   .\feishu-task.ps1 list                          # 查看本地已同步任务
# API：飞书任务 v2（/open-apis/task/v2/tasks）
# 依赖：tools/feishu.config.json（app_id/app_secret/user_id）
# =============================================================

param(
    [Parameter(Position = 0)][ValidateSet('init', 'create', 'complete', 'list')][string]$Action = 'list',
    [string]$Title,
    [string]$Desc = ''
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$configPath = Join-Path $scriptDir 'feishu.config.json'
$mapPath = Join-Path $scriptDir 'feishu-tasks.json'

# ---------- 配置 ----------
if (-not (Test-Path $configPath)) {
    Write-Host "❌ 未找到 $configPath" -ForegroundColor Red
    Write-Host "   请复制 feishu.config.template.json 为 feishu.config.json 并填入 app_id/app_secret/user_id"
    exit 1
}
$cfg = Get-Content $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
if ($cfg.app_id -match 'YOUR_' -or $cfg.app_secret -match 'YOUR_' -or $cfg.user_id -match 'YOUR_') {
    Write-Host "❌ feishu.config.json 仍是占位符，请填入真实凭据" -ForegroundColor Red
    exit 1
}

# ---------- 本地任务映射 ----------
function Load-Map {
    if (Test-Path $mapPath) {
        $obj = Get-Content $mapPath -Raw -Encoding UTF8 | ConvertFrom-Json
        $ht = @{}
        $obj.PSObject.Properties | ForEach-Object { $ht[$_.Name] = $_.Value }
        return $ht
    }
    return @{}
}
function Save-Map($map) {
    $json = $map | ConvertTo-Json -Depth 5
    [System.IO.File]::WriteAllText($mapPath, $json, (New-Object System.Text.UTF8Encoding($false)))
}

# ---------- 获取 tenant_access_token ----------
function Get-Token {
    $body = @{ app_id = $cfg.app_id; app_secret = $cfg.app_secret } | ConvertTo-Json
    try {
        $resp = Invoke-RestMethod -Method Post -Uri 'https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal' `
            -ContentType 'application/json; charset=utf-8' -Body $body
    } catch {
        Write-Host "❌ 网络请求失败：$($_.Exception.Message)" -ForegroundColor Red; exit 1
    }
    if ($resp.code -ne 0) {
        Write-Host "❌ 获取 token 失败：code=$($resp.code) msg=$($resp.msg)（检查 app_id/app_secret 与应用发布状态）" -ForegroundColor Red; exit 1
    }
    return $resp.tenant_access_token
}

$token = Get-Token
$authHeader = @{ Authorization = "Bearer $token" }
$apiBase = 'https://open.feishu.cn/open-apis/task/v2/tasks'

# ---------- 动作分发 ----------
switch ($Action) {

    'init' {
        Write-Host "✅ 凭据有效，tenant_access_token 获取成功（应用连通正常）"
        Write-Host "   user_id: $($cfg.user_id)"
        # 读取一个不存在 guid 验证任务读权限（预期报错但说明权限通路）
        try {
            Invoke-RestMethod -Method Get -Uri "$apiBase/00000000-0000-0000-0000-000000000000" -Headers $authHeader | Out-Null
        } catch {
            if ($_.ErrorDetails.Message -match 'not exist|不存在') {
                Write-Host "✅ 任务 API（v2）权限正常"
            } else {
                Write-Host "⚠️ 任务 API 读取返回：" $_.ErrorDetails.Message
            }
        }
    }

    'create' {
        if (-not $Title) { Write-Host "❌ 缺少 -Title" -ForegroundColor Red; exit 1 }
        $map = Load-Map
        if ($map.ContainsKey($Title)) {
            Write-Host "⚠️ 任务「$Title」已存在（guid: $($map[$Title].guid)），跳过创建"; exit 0
        }
        $body = @{
            summary     = $Title
            description = $Desc
            members     = @(@{ id = $cfg.user_id; type = 'user'; role = 'assignee' })
        } | ConvertTo-Json -Depth 5
        try {
            $resp = Invoke-RestMethod -Method Post -Uri "$apiBase`?user_id_type=user_id" `
                -Headers $authHeader -ContentType 'application/json; charset=utf-8' -Body $body
        } catch {
            Write-Host "❌ 创建失败：" $_.ErrorDetails.Message -ForegroundColor Red; exit 1
        }
        if ($resp.code -ne 0) {
            Write-Host "❌ 创建失败：code=$($resp.code) msg=$($resp.msg)" -ForegroundColor Red
            if ($resp.msg -match 'permission|权限') { Write-Host "   提示：应用缺任务写权限，或未发布版本" }
            exit 1
        }
        $guid = $resp.data.task.guid
        $taskId = $resp.data.task.task_id
        $map[$Title] = @{ guid = $guid; task_id = $taskId; status = 'open'; created = (Get-Date -Format 'yyyy-MM-dd HH:mm') }
        Save-Map $map
        Write-Host "✅ 飞书任务已创建：$Title（task_id: $taskId）"
    }

    'complete' {
        if (-not $Title) { Write-Host "❌ 缺少 -Title" -ForegroundColor Red; exit 1 }
        $map = Load-Map
        if (-not $map.ContainsKey($Title)) {
            Write-Host "❌ 本地映射中找不到任务「$Title」（先 create 过才能 complete）" -ForegroundColor Red; exit 1
        }
        $guid = $map[$Title].guid
        $nowMs = [string][DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        $body = @{
            update_fields = @('completed_at')
            task          = @{ completed_at = $nowMs }
        } | ConvertTo-Json -Depth 5
        $idempotent = $false
        try {
            $resp = Invoke-RestMethod -Method Patch -Uri "$apiBase/$guid" `
                -Headers $authHeader -ContentType 'application/json; charset=utf-8' -Body $body
        } catch {
            $em = [string]$_.ErrorDetails.Message
            if ($em -match 'completed task') { $idempotent = $true }
            else { Write-Host "❌ 更新失败：" $em -ForegroundColor Red; exit 1 }
        }
        if (-not $idempotent -and $resp.code -ne 0) {
            $msg = [string]$resp.msg
            if ($msg -match 'completed task') { $idempotent = $true }
            else { Write-Host "❌ 更新失败：code=$($resp.code) msg=$($resp.msg)" -ForegroundColor Red; exit 1 }
        }
        if ($idempotent) { Write-Host "⚠️ 任务「$Title」在飞书侧已是完成态（幂等处理，视为成功）" }
        $entry = $map[$Title]
        $entry | Add-Member -NotePropertyName status -NotePropertyValue 'completed' -Force
        $entry | Add-Member -NotePropertyName completed -NotePropertyValue (Get-Date -Format 'yyyy-MM-dd HH:mm') -Force
        Save-Map $map
        Write-Host "✅ 飞书任务已完成：$Title"
    }

    'list' {
        $map = Load-Map
        if ($map.Count -eq 0) { Write-Host "（本地无已同步任务）"; return }
        Write-Host "📋 已同步的飞书任务（$($map.Count) 项）"
        foreach ($k in $map.Keys) {
            $icon = if ($map[$k].status -eq 'completed') { '[完成]' } else { '[进行中]' }
            Write-Host "  $icon $k  ($($map[$k].task_id))"
        }
    }
}
