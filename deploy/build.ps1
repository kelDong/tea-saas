# ============================================================
# tea-saas 生产构建脚本（本机演练与云服务器同套流程）
# 用法：powershell -ExecutionPolicy Bypass -File build.ps1 [-SkipBackend] [-SkipWeb] [-SkipSql]
# 产物：
#   deploy/build/backend/   app.jar + Dockerfile     （backend.Dockerfile 的构建上下文）
#   deploy/build/web/       dist/ + Dockerfile + nginx.conf（web.Dockerfile 的构建上下文）
#   deploy/sql/             01_schema.sql + 02_data.sql（compose 挂载为首启初始化脚本）
# 之后：cp .env.example .env 填密码 → docker compose up -d --build
# ============================================================
param(
    [switch]$SkipBackend,
    [switch]$SkipWeb,
    [switch]$SkipSql
)

$ErrorActionPreference = 'Stop'

$deployDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoDir   = Split-Path -Parent $deployDir
$buildDir  = Join-Path $deployDir 'build'

Write-Host "==> 仓库根目录: $repoDir"

# ---------- 1. 后端 jar ----------
if (-not $SkipBackend) {
    Write-Host '==> [1/3] 构建后端 jar（mvn -DskipTests package，多模块较慢）...'
    Push-Location (Join-Path $repoDir 'backend-api')
    try {
        mvn -q -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw 'mvn package 失败' }
    } finally { Pop-Location }

    $backendOut = Join-Path $buildDir 'backend'
    New-Item -ItemType Directory -Force -Path $backendOut | Out-Null
    $jar = Join-Path $repoDir 'backend-api/tea-server/target/tea-server-1.0.0-SNAPSHOT.jar'
    if (-not (Test-Path $jar)) { throw "找不到 jar：$jar" }
    Copy-Item $jar (Join-Path $backendOut 'app.jar') -Force
    Copy-Item (Join-Path $deployDir 'backend.Dockerfile') (Join-Path $backendOut 'Dockerfile') -Force
    Write-Host "    -> $backendOut"
}

# ---------- 2. 前端 dist ----------
if (-not $SkipWeb) {
    Write-Host '==> [2/3] 构建前端 dist（npm run build：vue-tsc + vite build）...'
    Push-Location (Join-Path $repoDir 'admin-web')
    try {
        npm run build
        if ($LASTEXITCODE -ne 0) { throw 'npm run build 失败' }
    } finally { Pop-Location }

    $webOut = Join-Path $buildDir 'web'
    New-Item -ItemType Directory -Force -Path $webOut | Out-Null
    Copy-Item (Join-Path $repoDir 'admin-web/dist') $webOut -Recurse -Force
    Copy-Item (Join-Path $deployDir 'web.Dockerfile') (Join-Path $webOut 'Dockerfile') -Force
    Copy-Item (Join-Path $deployDir 'nginx/nginx.conf') (Join-Path $webOut 'nginx.conf') -Force
    Write-Host "    -> $webOut"
}

# ---------- 3. 首启 SQL（挂载到 mysql 的 docker-entrypoint-initdb.d） ----------
# 唯一事实源是 backend-api/sql/init/，此处仅复制（勿手改 deploy/sql/）；
# 库由 compose 的 MYSQL_DATABASE 自动创建，entrypoint 会自动选中该库执行，无需 CREATE DATABASE
if (-not $SkipSql) {
    Write-Host '==> [3/3] 复制首启 SQL（backend-api/sql/init -> deploy/sql）...'
    $sqlOut = Join-Path $deployDir 'sql'
    New-Item -ItemType Directory -Force -Path $sqlOut | Out-Null
    Copy-Item (Join-Path $repoDir 'backend-api/sql/init/*.sql') $sqlOut -Force
    Write-Host "    -> $sqlOut"
}

Write-Host '==> 构建完成。下一步：'
Write-Host '    1) 若无 .env：copy .env.example .env 并填写全部密码（纯字母数字）'
Write-Host '    2) docker compose up -d --build'
Write-Host '    3) 首次启动在 .env 配 TEA_ADMIN_PASSWORD（admin 首启密码初始化，登录一次后可移除）'
