package com.teasaas.system.controller;

import com.teasaas.common.result.Result;
import com.teasaas.system.oss.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口（MinIO）
 *
 * <p>登录即可用（SaInterceptor 统一拦截）；不记 @OpLog（高频操作避免刷日志）。
 * 当前仅图片：jpg/jpeg/png/webp/gif，≤5MB，返回永久公开 URL。</p>
 */
@Validated
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "SYSTEM-文件上传", description = "图片上传 MinIO，返回公开 URL")
public class FileController {

    private final OssService ossService;

    @Operation(summary = "上传图片（multipart 字段名 file），返回永久公开 URL")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(ossService.upload(file));
    }
}
