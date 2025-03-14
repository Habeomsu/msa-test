package main.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import main.gateway.global.ApiResult;
import main.gateway.global.code.status.ErrorStatus;
import main.gateway.util.JWTUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.util.AntPathMatcher;
import org.springframework.http.MediaType;


import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JWTGatewayFilter implements GlobalFilter, Ordered {

    private final JWTUtil jwtUtil;
    private final ObjectMapper objectMapper;
    AntPathMatcher matcher = new AntPathMatcher();

    private static final List<String> WHITELIST_PATTERNS = List.of("/**/auth/reissue", "/**/auth/login", "/**/auth/logout");

    public JWTGatewayFilter(JWTUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String requestPath = request.getURI().getPath();

        // OPTIONS 요청은 CORS preflight로 인증 없이 허용
        if ("OPTIONS".equalsIgnoreCase(request.getMethod().name())) {
            return chain.filter(exchange);
        }

        // 화이트리스트에 포함된 엔드포인트는 JWT 검증 없이 통과
        // 화이트리스트 패턴에 매칭되면 JWT 검증 없이 통과
        boolean isWhitelisted = WHITELIST_PATTERNS.stream()
                .anyMatch(pattern -> matcher.match(pattern, requestPath));
        if (isWhitelisted) {
            return chain.filter(exchange);
        }

        HttpHeaders headers = request.getHeaders();
        String authorizationHeader = headers.getFirst("Authorization");

        // JWT 토큰이 없으면 401 응답
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return unauthorizedResponse(exchange, ErrorStatus._NOT_FOUND_JWT);
        }
        String accessToken = authorizationHeader.substring("Bearer ".length());

        System.out.println(accessToken);

        try {
            // 토큰 만료 여부 및 유효성 검증 (내부에서 Signature, 기타 예외 발생)
            jwtUtil.isExpired(accessToken);
        } catch (ExpiredJwtException e) {
            return unauthorizedResponse(exchange, ErrorStatus._EXFIRED_JWT);
        } catch (SignatureException e) {
            return unauthorizedResponse(exchange,ErrorStatus._INVALID_JWT );
        } catch (JwtException e) {
            return unauthorizedResponse(exchange,ErrorStatus._INVALID_JWT );
        }

        // 토큰 카테고리 검증 (access 토큰인지 확인)
        String category = jwtUtil.getCategory(accessToken);
        if (!"access".equals(category)) {
            return unauthorizedResponse(exchange,ErrorStatus._INVALID_ACCESS_JWT );
        }

        // (선택 사항) 유효한 토큰의 경우, 인증 정보를 ReactiveSecurityContextHolder에 설정하여 하위 서비스에서 활용할 수 있습니다.

        return chain.filter(exchange);

    }

    // 401 Unauthorized 응답 전송 (JSON 형식)
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, ErrorStatus errorStatus) {
        // 게이트웨이 응답 설정
        exchange.getResponse().setStatusCode(errorStatus.getHttpStatus());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // ApiResult 작성
        // detailMessage(예: "만료된 Access Token입니다.")를 message로 덮어쓰거나, data에 넣어도 됨
        ApiResult<?> apiResult = ApiResult.onFailure(
                errorStatus.getCode(),
                errorStatus.getMessage(), // 혹은 errorStatus.getMessage() 사용
                null
        );

        // JSON 직렬화
        String resultJson;
        try {
            resultJson = objectMapper.writeValueAsString(apiResult);
        } catch (JsonProcessingException e) {
            resultJson = "{\"success\":false,\"code\":\"COMMON500\",\"message\":\"JSON 직렬화 오류\",\"data\":null}";
            // JSON 직렬화조차 실패하면, 어쩔 수 없이 하드코딩한 기본 에러 메시지로 응답
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(resultJson.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
