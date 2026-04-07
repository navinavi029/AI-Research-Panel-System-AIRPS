package com.aipanelist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application class for the AI Panelist System.
 * 
 * This Spring Boot application orchestrates multi-agent AI analysis of research documents
 * using NVIDIA models. The system accepts PDF uploads, extracts and chunks text content,
 * distributes analysis tasks to specialized AI agents, and synthesizes individual analyses
 * into a consensus report.
 */
@SpringBootApplication
@EnableAsync
public class AIPanelistApplication {

    public static void main(String[] args) {
        SpringApplication.run(AIPanelistApplication.class, args);
    }
}
