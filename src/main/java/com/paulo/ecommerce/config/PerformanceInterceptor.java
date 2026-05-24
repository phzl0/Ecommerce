package com.paulo.ecommerce.config;

import jakarta.servlet.http.*;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class PerformanceInterceptor implements HandlerInterceptor {

    private static final long LIMIAR_MS = 500;
    private static final String ATTR_INICIO = "reqInicio";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        request.setAttribute(ATTR_INICIO, System.currentTimeMillis());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {
        Long inicio = (Long) request.getAttribute(ATTR_INICIO);
        if (inicio != null) {
            long duracao = System.currentTimeMillis() - inicio;
            if (duracao > LIMIAR_MS) {
                System.out.printf(
                        "[LENTO] %s %s levou %dms%n",
                        request.getMethod(),
                        request.getRequestURI(),
                        duracao);
            }
        }
    }
}