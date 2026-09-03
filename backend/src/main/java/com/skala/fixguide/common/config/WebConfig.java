package com.skala.fixguide.common.config;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 업로드된 제품 사진을 /api/v1/files/** 로 정적 서빙한다 (PhotoResponse.PUBLIC_PREFIX 와 짝) */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/v1/files/**")
                .addResourceLocations(Paths.get(uploadDir).toAbsolutePath().toUri().toString());
    }
}
