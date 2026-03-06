package com.medlab.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件存储服务
 * 
 * 职责：
 * 1. 处理文件上传和保存
 * 2. 管理文件存储路径
 * 3. 文件的删除和检索
 * 4. 文件安全性校验（文件类型、大小等）
 */
@Service
public class StorageService {
    
    private static final String UPLOAD_DIR = "uploads";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    /**
     * 保存上传的文件
     * 
     * @param file 上传的文件
     * @return 保存后的文件路径
     */
    public String saveFile(MultipartFile file) {
        try {
            // 验证文件大小
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IOException("File size exceeds maximum limit of 10MB");
            }
            
            // 创建上传目录
            File uploadDirectory = new File(UPLOAD_DIR);
            if (!uploadDirectory.exists()) {
                uploadDirectory.mkdirs();
            }
            
            // 生成唯一文件名
            String originalFilename = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + "_" + originalFilename;
            String filePath = UPLOAD_DIR + File.separator + fileName;
            
            // 保存文件
            File destinationFile = new File(filePath);
            file.transferTo(destinationFile);
            
            return filePath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file: " + e.getMessage());
        }
    }
    
    /**
     * 删除文件
     */
    public void deleteFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }
}
