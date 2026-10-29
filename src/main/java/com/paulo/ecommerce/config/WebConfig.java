package com.paulo.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.*;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @org.springframework.beans.factory.annotation.Value("${app.storage-dir:./storage}") private String storage;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/products/**").addResourceLocations(java.nio.file.Paths.get(storage,"products").toAbsolutePath().toUri().toString()+"/");
        // CSS e JS — cache de 7 dias no navegador
        registry.addResourceHandler("/css/**", "/js/**")
                .addResourceLocations(
                        "classpath:/static/css/",
                        "classpath:/static/js/")
                .setCacheControl(
                        CacheControl.maxAge(7, TimeUnit.DAYS)
                                .mustRevalidate());

        // Imagens — cache de 30 dias
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCacheControl(
                        CacheControl.maxAge(30, TimeUnit.DAYS));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PerformanceInterceptor());
    }
}
