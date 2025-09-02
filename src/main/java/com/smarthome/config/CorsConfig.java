package com.smarthome.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 1. 允许的前端域名（必须具体，不能写 *！否则无法携带 Cookie）
        // 例：若前端部署在 http://192.168.1.100:8081 或 https://your-frontend.com
        config.addAllowedOrigin("https://localhost:8001");
        config.addAllowedOrigin("https://your-frontend.com");

        // 2. 允许的请求方法（GET/POST/PUT/DELETE 等）
        config.addAllowedMethod("*");

        // 3. 允许的请求头（如 Content-Type、Authorization 等）
        config.addAllowedHeader("*");

        // 4. 关键：允许跨域携带 Cookie（必须设为 true）
        config.setAllowCredentials(true);

        // 5. 配置哪些接口生效（/* 表示所有接口）
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}