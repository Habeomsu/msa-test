package main.test1.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import main.test1.security.JWTUtil;
import main.test1.service.RedisService;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class ReissueController {

    private final JWTUtil jwtUtil;
    private final RedisService redisService;

    public ReissueController(JWTUtil jwtUtil, RedisService redisService) {
        this.jwtUtil = jwtUtil;
        this.redisService = redisService;
    }


    @PostMapping("/reissue")
    public ResponseEntity<?>reissue(HttpServletRequest request, HttpServletResponse response) {
        String refresh = null;
        Cookie[] cookies = request.getCookies();
        // 쿠키가 null인지 확인
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("refresh")) {
                    refresh = cookie.getValue();
                }
            }
        }
        if (refresh == null) {
            // Refresh 토큰이 없으면 Unauthorized 응답
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Refresh token not found\"}");
        }
        try {
            // Refresh 토큰 만료 검사
            jwtUtil.isExpired(refresh);
        } catch (Exception e) {
            // 토큰 만료 오류
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Refresh token expired\"}");
        }

        // Refresh 토큰 검증 및 재발급 로직 추가
        String category = jwtUtil.getCategory(refresh);
        if (!category.equals("refresh")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"message\": \"Invalid refresh token\"}");
        }

        String username = jwtUtil.getUsername(refresh);
        String newAccess = jwtUtil.createJwt("access", username, jwtUtil.getRole(refresh), 600000L);
        String newRefresh = jwtUtil.createJwt("refresh", username, jwtUtil.getRole(refresh), 86400000L);

        // Redis에서 기존 리프레시 토큰 삭제 후 새로운 리프레시 토큰 저장
        redisService.deleteRefreshToken(username);
        redisService.saveRefreshToken(username, newRefresh);

        // 새로운 JWT를 응답에 설정
        response.setHeader("Authorization", "Bearer " + newAccess);
        response.addCookie(createCookie("refresh", newRefresh));

        // 성공 메시지 응답
        return ResponseEntity.status(HttpStatus.OK)
                .body("{\"message\": \"Tokens successfully reissued\"}");

    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60); // 1 day expiration
        cookie.setHttpOnly(true);
        return cookie;
    }
}
