package main.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import main.gateway.global.ApiResult;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class GlobalResponseWrapperFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();

        // Content-Type 헤더에 UTF-8 charset을 명시적으로 설정
        originalResponse.getHeaders().set("Content-Type", "application/json; charset=UTF-8");

        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                // 수정된 응답과 실제 바이트 수가 맞도록 Content-Length 헤더 제거
                getDelegate().getHeaders().remove("Content-Length");

                // Mono 처리
                if (body instanceof Mono) {
                    return ((Mono<? extends DataBuffer>) body)
                            .flatMap(dataBuffer -> {
                                String originalBody = dataBuffer.toString(StandardCharsets.UTF_8);
                                System.out.println("Mono - Original body: " + originalBody);
                                DataBufferUtils.release(dataBuffer);

                                // JSON 파싱 시도. 실패하면 원본 문자열 그대로 사용.
                                Object resultObj;
                                try {
                                    resultObj = objectMapper.readValue(originalBody, Object.class);
                                } catch (JsonProcessingException e) {
                                    resultObj = originalBody;
                                }

                                ApiResult<Object> apiResult = ApiResult.onSuccess(resultObj);
                                try {
                                    String modifiedBody = objectMapper.writeValueAsString(apiResult);
                                    byte[] bytes = modifiedBody.getBytes(StandardCharsets.UTF_8);
                                    DataBuffer newBuffer = bufferFactory.wrap(bytes);
                                    return Mono.just(newBuffer);
                                } catch (JsonProcessingException e) {
                                    return Mono.error(e);
                                }
                            })
                            .flatMap(buffer -> super.writeWith(Mono.just(buffer)));
                }
                // Flux 처리
                else if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
                    return super.writeWith(
                            fluxBody.collectList().flatMap(dataBuffers -> {
                                DataBuffer joinedBuffer = bufferFactory.join(dataBuffers);
                                String originalBody = joinedBuffer.toString(StandardCharsets.UTF_8);
                                System.out.println("Flux - Original body: " + originalBody);
                                DataBufferUtils.release(joinedBuffer);

                                Object resultObj;
                                try {
                                    resultObj = objectMapper.readValue(originalBody, Object.class);
                                } catch (JsonProcessingException e) {
                                    resultObj = originalBody;
                                }

                                ApiResult<Object> apiResult = ApiResult.onSuccess(resultObj);
                                try {
                                    String modifiedBody = objectMapper.writeValueAsString(apiResult);
                                    byte[] bytes = modifiedBody.getBytes(StandardCharsets.UTF_8);
                                    DataBuffer newBuffer = bufferFactory.wrap(bytes);
                                    return Mono.just(newBuffer);
                                } catch (JsonProcessingException e) {
                                    return Mono.error(e);
                                }
                            })
                    );
                }
                // 그 외의 경우 그대로 반환
                return super.writeWith(body);
            }
        };

        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    @Override
    public int getOrder() {
        // 다른 필터들보다 우선 적용하도록 (-3)
        return -3;
    }
}
