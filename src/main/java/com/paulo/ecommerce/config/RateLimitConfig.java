package com.paulo.ecommerce.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.springframework.stereotype.Component;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitConfig {

    private static final int MAX_TENTATIVAS = 5;
    private static final int BLOQUEIO_MINUTOS = 15;

    private final LoadingCache<String, Integer> tentativasCache =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(BLOQUEIO_MINUTOS, TimeUnit.MINUTES)
                    .build(new CacheLoader<>() {
                        @Override
                        public Integer load(String key) {
                            return 0;
                        }
                    });

    public boolean isBlockeado(String ip) {
        try {
            return tentativasCache.get(ip) >= MAX_TENTATIVAS;
        } catch (ExecutionException e) {
            return false;
        }
    }

    public void registrarTentativaFalha(String ip) {
        try {
            int tentativas = tentativasCache.get(ip);
            tentativasCache.put(ip, tentativas + 1);
        } catch (ExecutionException e) {
            tentativasCache.put(ip, 1);
        }
    }

    public void resetar(String ip) {
        tentativasCache.invalidate(ip);
    }
}