package com.paulo.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// Adicione ao EcommerceApplication.java
import org.springframework.scheduling.annotation.EnableAsync;

@org.springframework.scheduling.annotation.EnableScheduling
@EnableAsync
@SpringBootApplication
public class EcommerceApplication {

  public static void main(String[] args) {
    java.util.TimeZone.setDefault(
        java.util.TimeZone.getTimeZone(
            java.time.ZoneId.of(
                System.getenv().getOrDefault("APP_TIMEZONE", "America/Sao_Paulo"))));
    SpringApplication.run(EcommerceApplication.class, args);
  }
}
