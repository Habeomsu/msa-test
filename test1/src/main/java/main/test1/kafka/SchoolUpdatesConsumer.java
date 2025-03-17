package main.test1.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import main.test1.dto.SchoolResponseDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SchoolUpdatesConsumer {

    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, SchoolResponseDto.School> redisTemplate;

    public SchoolUpdatesConsumer(ObjectMapper objectMapper, RedisTemplate<String, SchoolResponseDto.School> redisTemplate) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "schoolUpdates",groupId = "school-updates-group")
    public void consume(String message) {
        log.info("Received school update message: {}", message);
        try {
            // JSON 메시지를 SchoolResponseDto.School 객체로 역직렬화
            SchoolResponseDto.School school = objectMapper.readValue(message, SchoolResponseDto.School.class);
            log.info("Deserialized School update: {}", school);
            String cacheKey = "school:" + school.getId();
            redisTemplate.opsForValue().set(cacheKey, school);
            log.info("Updated Redis cache for key: {}", cacheKey);
        } catch (Exception e) {
            log.error("Error deserializing school update message", e);
        }
    }
}
