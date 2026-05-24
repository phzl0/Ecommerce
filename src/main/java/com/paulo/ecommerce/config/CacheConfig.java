package com.paulo.ecommerce.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(
                new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(
                new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public CacheManager redisCacheManager(RedisConnectionFactory factory) {
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();

        // Produtos listados — 10 minutos
        configs.put("produtos", config(Duration.ofMinutes(10)));

        // Produto individual — 30 minutos
        configs.put("produto", config(Duration.ofMinutes(30)));

        // Categorias raramente mudam — 1 hora
        configs.put("categorias", config(Duration.ofHours(1)));

        // Métricas do dashboard — 5 minutos
        configs.put("metricas", config(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(configs)
                .build();
    }

    @Bean
    @ConditionalOnProperty(
            name = "spring.cache.type",
            havingValue = "simple",
            matchIfMissing = true)
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                "produtos", "produto", "categorias", "metricas");
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    private RedisCacheConfiguration config(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(
                                        new GenericJackson2JsonRedisSerializer()));
    }
}
