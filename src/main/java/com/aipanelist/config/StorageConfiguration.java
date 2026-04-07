package com.aipanelist.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for file storage.
 * 
 * Manages the upload directory where PDF files are stored.
 * Creates the directory on startup if it doesn't exist.
 */
@Configuration
@Getter
public class StorageConfiguration {

    @Value("${app.storage.upload-dir}")
    private String uploadDir;

    private Path uploadPath;

    @PostConstruct
    public void init() throws IOException {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
    }
    
    public Path getUploadPath() {
        return uploadPath;
    }
    
    public String getUploadDir() {
        return uploadDir;
    }
}
