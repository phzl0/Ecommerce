package com.paulo.ecommerce.config;

import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @org.springframework.beans.factory.annotation.Value("${app.storage-dir:./storage}")
  private String storage;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/media/products/**")
        .addResourceLocations(
            java.nio.file.Paths.get(storage, "products").toAbsolutePath().toUri().toString() + "/");
    // Stable URLs must revalidate; admin URLs additionally carry a content fingerprint.
    registry.addResourceHandler("/css/**")
        .addResourceLocations("classpath:/static/css/")
        .setCacheControl(CacheControl.noCache());
    registry.addResourceHandler("/js/**")
        .addResourceLocations("classpath:/static/js/")
        .setCacheControl(CacheControl.noCache());

    // Imagens — cache de 30 dias
    registry
        .addResourceHandler("/images/**")
        .addResourceLocations("classpath:/static/images/")
        .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS));
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new PerformanceInterceptor());
  }
}
