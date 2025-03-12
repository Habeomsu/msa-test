package main.test1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserRequestDto {


    @Getter
    @AllArgsConstructor
    @Builder
    @NoArgsConstructor
    public static class JoinDto{

        private String username;
        private String password;
        private String email;

    }

    @Getter
    @AllArgsConstructor
    @Builder
    @NoArgsConstructor
    public static class LoginDto{

        private String username;
        private String password;

    }

}
