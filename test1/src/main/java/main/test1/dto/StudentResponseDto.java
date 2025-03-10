package main.test1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StudentResponseDto {


    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentDto {

        private Long id;
        private String name;
        private String email;
        private String school_name;
        private String school_email;


    }
}
