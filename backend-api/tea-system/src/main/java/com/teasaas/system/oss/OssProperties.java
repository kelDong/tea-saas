package com.teasaas.system.oss;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * MinIO 对象存储配置（application.yml tea.oss 前缀）
 *
 * <p>endpoint 即对外返回 URL 的基地址：本地为 MinIO 容器地址，
 * 部署迁移时用 OSS_ENDPOINT 环境变量覆盖为公网/内网可达地址。</p>
 */
@Data
@ConfigurationProperties(prefix = "tea.oss")
public class OssProperties {

    /** API 地址（如 http://localhost:9000） */
    private String endpoint = "http://localhost:9000";

    private String accessKey = "minioadmin";

    private String secretKey = "minioadmin";

    /** 桶名（启动时不存在则自动创建并设公共读策略） */
    private String bucketName = "tea-saas";

    /** 单文件大小上限（业务层精确校验） */
    private DataSize maxSize = DataSize.ofMegabytes(5);
}
