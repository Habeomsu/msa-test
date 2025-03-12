package main.test1.service;

import lombok.AccessLevel;
import lombok.Setter;
import main.test1.entity.RefreshToken;
import main.test1.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

@Service
public class RedisService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RedisService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    // Refresh Token 저장
    public void saveRefreshToken(String username, String refreshToken) {
        RefreshToken token = new RefreshToken(username, refreshToken);
        refreshTokenRepository.save(token);  // Redis에 저장
    }


}
