package main.test1.dto;

import lombok.Getter;

public class StudentRequestDto {

    @Getter
    public static class Student{

        private String name;
        private String email;
        private Long school_id;


    }
}
