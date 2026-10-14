package com.teasaas.system.oss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * MinIO 对象存储配置（application.yml tea.oss 前缀）
 *
 * <p><b>endpoint 与 publicEndpoint 必须分离</b>：endpoint 供 MinioClient SDK 内部寻址
 * （须直连 MinIO，经反代改写路径会破坏 SigV4 签名）；publicEndpoint 仅供拼接返回给
 * 浏览器的公开 URL（可走 nginx 反代）。publicEndpoint 为空时回落 endpoint。</p>
 */
@Data
@ConfigurationProperties(prefix = "tea.oss")
public class OssProperties {

    /** SDK 内部 API 地址（如容器内 http://minio:9000，勿走反代） */
    private String endpoint = "http://localhost:9000";

    /** 对外公开 URL 基地址（浏览器可达，如 http://example.com/oss；空=回落 endpoint） */
    private String publicEndpoint = "";

    private String accessKey = "minioadmin";

    private String secretKey = "minioadmin";

    /** 桶名（启动时不存在则自动创建并设公共读策略） */
    private String bucketName = "tea-saas";

    /** 单文件大小上限（业务层精确校验） */
    private DataSize maxSize = DataSize.ofMegabytes(5);
}
