package ru.itmo.zavar.brainstop.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.lang.reflect.Array;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
@Slf4j
public class ServiceLoggingAspect {

    @Around("within(@org.springframework.stereotype.Service *)")
    public Object logServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        String method = joinPoint.getSignature().toShortString();
        String args = formatArgs(joinPoint.getArgs());

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            logWarn(method, args, start, ex);
            throw ex;
        }

        if (result instanceof Mono<?> mono) {
            return mono
                    .doOnSuccess(value -> logInfo(method, args, start))
                    .doOnError(ex -> logWarn(method, args, start, ex));
        }
        if (result instanceof Flux<?> flux) {
            return flux
                    .doOnComplete(() -> logInfo(method, args, start))
                    .doOnError(ex -> logWarn(method, args, start, ex));
        }

        logInfo(method, args, start);
        return result;
    }

    private void logInfo(String method, String args, long start) {
        log.info("service={} args={} durationMs={}", method, args, elapsedMs(start));
    }

    private void logWarn(String method, String args, long start, Throwable ex) {
        log.warn("service={} args={} durationMs={} error={}",
                method, args, elapsedMs(start), ex == null ? "unknown" : ex.toString());
    }

    private long elapsedMs(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(formatArg(args[i]));
        }
        out.append(']');
        return out.toString();
    }

    private String formatArg(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof String str) {
            return quote(truncate(str, 80));
        }
        if (arg instanceof Number || arg instanceof Boolean || arg instanceof Enum<?> || arg instanceof UUID) {
            return String.valueOf(arg);
        }
        if (arg instanceof Collection<?> collection) {
            return arg.getClass().getSimpleName() + "(size=" + collection.size() + ")";
        }
        if (arg.getClass().isArray()) {
            return arg.getClass().getComponentType().getSimpleName()
                    + "[](size=" + Array.getLength(arg) + ")";
        }
        return arg.getClass().getSimpleName();
    }

    private String quote(String value) {
        return "\"" + value + "\"";
    }

    private String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max) + "...";
    }
}
