package ru.itmo.zavar.brainstop.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@ConditionalOnWebApplication(type = Type.REACTIVE)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ReactiveRequestLogFilter implements WebFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long start = System.currentTimeMillis();
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
            exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
        }
        String finalRequestId = requestId;
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long durationMs = System.currentTimeMillis() - start;
                    String path = buildPath(exchange);
                    HttpStatusCode status = exchange.getResponse().getStatusCode();
                    int statusCode = status != null ? status.value() : 200;
                    HttpMethod method = exchange.getRequest().getMethod();
                    String methodValue = method != null ? method.name() : "UNKNOWN";
                    log.info("requestId={} method={} path={} status={} durationMs={}",
                            finalRequestId, methodValue, path, statusCode, durationMs);
                });
    }

    private String buildPath(ServerWebExchange exchange) {
        String path = exchange.getRequest().getURI().getRawPath();
        String query = exchange.getRequest().getURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return path;
        }
        return path + "?" + query;
    }
}
