package com.example.EnergyMix.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {

                registry.addMapping("/api/**")
                        .allowedOrigins(
                                "http://localhost",
                                "http://localhost:5173",
                                "http://127.0.0.1",
                                "https://MY_FRONTEND.onrender.com"
                        )
                        .allowedMethods("GET")
                        .allowedHeaders("*")
                        .allowCredentials(false);
            }
        };
    }
}
