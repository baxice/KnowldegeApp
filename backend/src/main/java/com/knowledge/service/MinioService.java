package com.knowledge.service;

import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class MinioService {
    
    private final MinioClient minioClient;
    
    @Value("${minio.bucket.documents:documents}")
    private String documentBucket;
    
    /**
     * 初始化存储桶
     */
    public void initBucket() {
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(documentBucket)
                    .build());
            
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(documentBucket)
                        .build());
            }
        } catch (Exception e) {
            throw new RuntimeException("初始化文档存储桶失败", e);
        }
    }
    
    /**
     * 上传文档
     */
    public String uploadDocument(MultipartFile file, Long userId) {
        try {
            String fileName = userId + "/" + System.currentTimeMillis() + "_" + 
                             file.getOriginalFilename();
            
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(documentBucket)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("上传文档失败", e);
        }
    }
    
    /**
     * 获取文档URL
     */
    public String getDocumentUrl(String fileName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(documentBucket)
                    .object(fileName)
                    .method(Method.GET)
                    .expiry(2, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("获取文档URL失败", e);
        }
    }
    
    /**
     * 删除文档
     */
    public void deleteDocument(String fileName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(documentBucket)
                    .object(fileName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("删除文档失败", e);
        }
    }
    
    /**
     * 获取文档流
     */
    public InputStream getDocumentStream(String fileName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(documentBucket)
                    .object(fileName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("获取文档流失败", e);
        }
    }
    
    /**
     * 列出用户文档
     */
    public List<String> listUserDocuments(Long userId) {
        try {
            String prefix = userId + "/";
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(documentBucket)
                            .prefix(prefix)
                            .recursive(true)
                            .build());
            
            return StreamSupport.stream(results.spliterator(), false)
                    .map(itemResult -> {
                        try {
                            return itemResult.get().objectName();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(name -> name != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("列出用户文档失败", e);
        }
    }
} 