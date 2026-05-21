/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.fdmpackloader.config;


import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
class MyWebMvcConfigurer implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

}