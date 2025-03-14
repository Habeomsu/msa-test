package main.test1.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import main.test1.service.RedisService;
import org.springframework.http.MediaType;
import org.springframework.web.filter.GenericFilterBean;
import jakarta.servlet.http.Cookie;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Slf4j
public class CustomLogoutFilter extends GenericFilterBean {

    private JWTUtil jwtUtil;
    private RedisService redisService;

    public CustomLogoutFilter(JWTUtil jwtUtil,RedisService redisService) {
        this.jwtUtil = jwtUtil;
        this.redisService = redisService;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        doFilter((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);

    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        // 요청 url 이 로그아웃인지 확인
        String requestUri = request.getRequestURI();
        if (!requestUri.matches("^\\/auth\\/logout$")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 요청이 post 인지 확인
        String requestMethod = request.getMethod();
        if (!requestMethod.equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 쿠키에서 refresh 토큰 가져오기
        String refresh = null;
        Cookie[] cookies = request.getCookies();

        // 쿠키가 null인지 확인
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refresh")) {
                    refresh = cookie.getValue();
                    break; // 쿠키를 찾으면 루프 종료
                }
            }
        }

        // refresh null check
        if (refresh == null) {
            // 상태 코드와 응답 메시지만 설정
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401 상태 코드 설정
            response.setContentType(MediaType.APPLICATION_JSON_VALUE); // JSON 응답 형식
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            // 응답 바디에 메시지 추가
            response.getWriter().write("{\"message\": \"Refresh token not found\"}");
            return;
        }

        // expired check
        try {
            jwtUtil.isExpired(refresh);
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401 상태 코드 설정
            response.setContentType(MediaType.APPLICATION_JSON_VALUE); // JSON 응답 형식
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"message\": \"Refresh token expired\"}");  // 응답 메시지
            return;
        } catch (SignatureException e) { // 서명 오류 처리
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401 상태 코드 설정
            response.setContentType(MediaType.APPLICATION_JSON_VALUE); // JSON 응답 형식
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"message\": \"Invalid refresh token signature\"}");  // 응답 메시지
            return;
        } catch (JwtException e) { // 기타 JWT 관련 오류 처리
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401 상태 코드 설정
            response.setContentType(MediaType.APPLICATION_JSON_VALUE); // JSON 응답 형식
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"message\": \"Invalid refresh token\"}");  // 응답 메시지
            return;
        }

        String category = jwtUtil.getCategory(refresh);
        if (!category.equals("refresh")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            try (PrintWriter out = response.getWriter()) {
                out.write("{\"message\": \"Invalid refresh token\"}");
            }
            return;
        }

        String username = jwtUtil.getUsername(refresh);

        String storedRefreshToken = redisService.getRefreshTokenByUsername(username);
        log.info("Refresh token: {}", storedRefreshToken);
        log.info("refresh token: {}", refresh);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refresh)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            try (PrintWriter out = response.getWriter()) {
                out.write("{\"message\": \"Refresh token not found or invalid\"}");
            }
            return;
        }

        // 로그아웃 진행: Redis에서 해당 리프레시 토큰 삭제
        redisService.deleteRefreshToken(username);


        // Refresh 토큰 Cookie 값 0
        Cookie cookie = new Cookie("refresh", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        // 로그아웃 성공 응답 설정
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\": \"Logout successful\"}");



    }
}
