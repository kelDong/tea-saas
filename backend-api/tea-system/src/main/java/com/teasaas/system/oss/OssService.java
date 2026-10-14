package com.teasaas.system.oss;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.teasaas.common.exception.BizException;
import com.teasaas.common.result.ErrorCode;
import com.teasaas.framework.security.context.SecurityUtils;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Date;
import java.util.Set;

/**
 * 对象存储上传服务
 *
 * <p>objectKey = {tenantId}/{yyyyMM}/{uuid32}.{ext}：首段为会话租户 ID，
 * 桶内按租户隔离；不含原始文件名（规避中文/特殊字符/路径问题），uuid 防碰撞。
 * 返回永久公开 URL：{endpoint}/{bucket}/{objectKey}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssService {

    /** 扩展名白名单（contentType 须同时以 image/ 开头，双条件防伪造） */
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final MinioClient minioClient;
    private final OssProperties ossProperties;

    /**
     * 上传图片，返回永久公开 URL
     *
     * @param file multipart 文件（后端层 max-file-size 兜底，此处 5MB 精确校验）
     */
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "上传文件为空");
        }
        String ext = resolveExtension(file.getOriginalFilename());
        String contentType = file.getContentType();
        if (!ALLOWED_EXT.contains(ext) || contentType == null || !contentType.startsWith("image/")) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
        if (file.getSize() > ossProperties.getMaxSize().toBytes()) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE);
        }

        String objectKey = SecurityUtils.getTenantIdFromSession()
                + "/" + DateUtil.format(new Date(), "yyyyMM")
                + "/" + IdUtil.fastSimpleUUID() + "." + ext;
        try (InputStream in = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(ossProperties.getBucketName())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            log.error("MinIO putObject failed, objectKey={}", objectKey, e);
            throw new BizException(ErrorCode.FILE_UPLOAD_FAIL);
        }
        return publicBaseUrl() + "/" + ossProperties.getBucketName() + "/" + objectKey;
    }

    /** 对外 URL 基地址：publicEndpoint 非空优先（浏览器可达的反代地址），否则回落 endpoint */
    private String publicBaseUrl() {
        String pub = ossProperties.getPublicEndpoint();
        if (pub != null && !pub.isBlank()) {
            // 去掉误配的尾斜杠，避免拼出 //bucket
            return pub.endsWith("/") ? pub.substring(0, pub.length() - 1) : pub;
        }
        return ossProperties.getEndpoint();
    }

    private String resolveExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dot + 1).toLowerCase();
    }
}
