package main.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import main.gateway.global.ApiResult;
import main.gateway.global.code.status.ErrorStatus;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.*;
import org.springframework.http.*;
import org.springframework.http.server.reactive.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.*;

import java.nio.charset.StandardCharsets;

@Component
public class GlobalResponseWrapperFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 원본 Response
        ServerHttpResponse originalResponse = exchange.getResponse();

        // Content-Type 설정 (JSON)
        originalResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // ServerHttpResponseDecorator 생성
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {

            @Override
            @SuppressWarnings("unchecked")
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                // Content-Length 제거 (바디 내용이 바뀌므로 재설정 필요)
                getDelegate().getHeaders().remove(HttpHeaders.CONTENT_LENGTH);

                // Spring Boot 3+ : getStatusCode() → HttpStatusCode
                HttpStatusCode statusCode = getDelegate().getStatusCode();
                if (statusCode == null) {
                    statusCode = HttpStatus.OK; // 기본 OK
                }

                // Mono 바디인지, Flux 바디인지 구분
                if (body instanceof Mono) {
                    HttpStatusCode finalStatusCode1 = statusCode;
                    return ((Mono<? extends DataBuffer>) body)
                            .flatMap(dataBuffer -> {
                                // 원본 바디 추출
                                String originalBody = dataBuffer.toString(StandardCharsets.UTF_8);
                                DataBufferUtils.release(dataBuffer);

                                // ApiResult로 감싸기
                                ApiResult<?> apiResult = buildApiResult(originalBody, finalStatusCode1);

                                // JSON 직렬화
                                String modified;
                                try {
                                    modified = objectMapper.writeValueAsString(apiResult);
                                } catch (Exception e) {
                                    return Mono.error(e);
                                }

                                // 새 바디(직렬화된 문자열)를 DataBuffer로 감싸
                                byte[] bytes = modified.getBytes(StandardCharsets.UTF_8);
                                DataBuffer newBuffer = exchange.getResponse().bufferFactory().wrap(bytes);

                                return Mono.just(newBuffer);
                            })
                            // 다시 super.writeWith(...)로 최종 응답
                            .flatMap(buf -> super.writeWith(Mono.just(buf)));

                } else if (body instanceof Flux) {
                    HttpStatusCode finalStatusCode = statusCode;
                    return Flux.from(body)
                            .collectList()
                            .flatMap(dataBuffers -> {
                                DataBuffer joinedBuffer = exchange.getResponse().bufferFactory().join(dataBuffers);
                                String originalBody = joinedBuffer.toString(StandardCharsets.UTF_8);
                                DataBufferUtils.release(joinedBuffer);

                                ApiResult<?> apiResult = buildApiResult(originalBody, finalStatusCode);

                                String modified;
                                try {
                                    modified = objectMapper.writeValueAsString(apiResult);
                                } catch (Exception e) {
                                    return Mono.error(e);
                                }

                                byte[] bytes = modified.getBytes(StandardCharsets.UTF_8);
                                DataBuffer newBuffer = exchange.getResponse().bufferFactory().wrap(bytes);

                                return super.writeWith(Mono.just(newBuffer));
                            });
                }
                // 그 외 그대로 통과
                return super.writeWith(body);
            }
        };

        // 수정된 response로 교체
        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    /**
     * 상태코드 보고, 2xx면 onSuccess, 그 외 onFailure
     * "originalBody" 문자열을 에러 코드로 보고 ErrorStatus 매핑
     */
    private ApiResult<?> buildApiResult(String originalBody, HttpStatusCode statusCode) {
        if (statusCode.is2xxSuccessful()) {
            // 성공
            Object parsed;
            try {
                parsed = objectMapper.readValue(originalBody, Object.class);
            } catch (Exception e) {
                // 만약 JSON 파싱 실패하면 그냥 문자열 그대로
                parsed = originalBody;
            }
            return ApiResult.onSuccess(parsed);
        } else {
            // 실패(4xx/5xx)
            // originalBody를 "errorCode"라고 가정 (예: "COMMON400")
            String errorCode = originalBody;

            // errorCode를 ErrorStatus로 매핑
            ErrorStatus matched = mapErrorStatus(errorCode);

            // ApiResult.onFailure
            return ApiResult.onFailure(
                    matched.getCode(),
                    matched.getMessage(),
                    null
            );
        }
    }

    /**
     * 문자열 -> ErrorStatus
     */
    private ErrorStatus mapErrorStatus(String code) {
        for (ErrorStatus es : ErrorStatus.values()) {
            if (es.getCode().equals(code)) {
                return es;
            }
        }
        // 못 찾으면 500
        return ErrorStatus._INTERNAL_SERVER_ERROR;
    }

    @Override
    public int getOrder() {
        // 우선순위 조정
        return -3;
    }
}
