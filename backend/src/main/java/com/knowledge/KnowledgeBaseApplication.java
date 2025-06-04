package com.knowledge;

import com.knowledge.service.MinioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class KnowledgeBaseApplication {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeBaseApplication.class, args);
    }
    
    /**
     * 应用启动时初始化MinIO存储桶
     */
    @Bean
    public CommandLineRunner initMinioStorage(MinioService minioService) {
        return args -> {
            try {
                // 初始化文档存储桶
                minioService.initBucket();
                logger.info("MinIO存储桶初始化成功");
            } catch (Exception e) {
                logger.error("MinIO存储桶初始化失败，文档上传功能可能不可用: {}", e.getMessage());
                // 不抛出异常，让应用继续启动
            }
        };
    }
} 