package com.teasaas.system.oss;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.InitializingBean;

/**
 * MinIO 客户端装配 + 桶自愈初始化
 *
 * <p>启动时幂等地确保桶存在并设置公共读（anonymous download）策略：
 * 商品图片 URL 需要长期可直连访问（prd_product.image 存永久地址，
 * C 端小程序/管理后台直接 img 标签加载），故不用会过期的 presigned URL。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(OssProperties.class)
public class MinioConfig implements InitializingBean {

    private final OssProperties ossProperties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(ossProperties.getEndpoint())
                .credentials(ossProperties.getAccessKey(), ossProperties.getSecretKey())
                .build();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 此处不能调 minioClient() @Bean 方法（配置类初始化期触发循环依赖），单独构建一次性客户端做桶自检
        MinioClient client = MinioClient.builder()
                .endpoint(ossProperties.getEndpoint())
                .credentials(ossProperties.getAccessKey(), ossProperties.getSecretKey())
                .build();
        String bucket = ossProperties.getBucketName();
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        // 匿名只读策略（重复设置幂等）
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
        client.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
        log.info("MinIO bucket ready: {} (public-read) at {}", bucket, ossProperties.getEndpoint());
    }
}
