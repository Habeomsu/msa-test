package main.test1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.Serializable;

public class SchoolResponseDto {

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class School implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private String name;
        private String email;

    }
}
