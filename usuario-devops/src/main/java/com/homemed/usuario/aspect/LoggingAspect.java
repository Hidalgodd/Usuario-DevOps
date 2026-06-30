package com.homemed.usuario.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspecto de logging para el microservicio usuario.
 *
 * Intercepta todas las llamadas a los métodos públicos de los controladores
 * y registra inicio, fin y duración. Respeta el traceId del MDC que el
 * controlador ya habrá inyectado, de modo que cada log queda correlacionado
 * con la traza de Grafana Tempo.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.homemed.usuario.controller..*(..))")
    public Object logEndpoint(ProceedingJoinPoint joinPoint) throws Throwable {
        String firma = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info(">>> INICIO [usuario] {} - argumentos: {}", firma, Arrays.toString(args));

        long inicio = System.currentTimeMillis();
        try {
            Object resultado = joinPoint.proceed();
            long duracion = System.currentTimeMillis() - inicio;
            MDC.put("duration_ms", String.valueOf(duracion));
            log.info("<<< FIN [usuario] {} - respuesta: {} - duracion: {} ms", firma, resultado, duracion);
            return resultado;
        } catch (Throwable ex) {
            long duracion = System.currentTimeMillis() - inicio;
            log.error("xxx ERROR [usuario] {} - excepcion: {} - duracion: {} ms", firma, ex.toString(), duracion);
            throw ex;
        }
    }
}
