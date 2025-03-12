package main.test1.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Refresh Token 저장
    public void saveRefreshToken(String username, String refreshToken) {
        // 저장할 때 Redis에 username을 key로, refreshToken을 value로 저장
        redisTemplate.opsForValue().set(username, refreshToken,86400, TimeUnit.SECONDS);
    }

    // 사용자 이름으로 리프레시 토큰 가져오기
    public String getRefreshTokenByUsername(String username) {
        return redisTemplate.opsForValue().get(username);  // username을 key로 찾아서 반환
    }

    // Redis에서 해당 사용자 리프레시 토큰 삭제
    public void deleteRefreshToken(String username) {
        redisTemplate.delete(username);  // username을 key로 Redis에서 삭제
    }
}
