package main.gateway.global.code.status;

import lombok.AllArgsConstructor;
import lombok.Getter;

import main.gateway.global.code.BaseErrorCode;
import main.gateway.global.code.ErrorReasonDto;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorStatus implements BaseErrorCode {

    // 일반 상태
    _INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"COMMON500","서버 에러"),
    _BAD_REQUEST(HttpStatus.BAD_REQUEST,"COMMON400","잘못된 요청입니다."),
    _UNAUTHORIZED(HttpStatus.UNAUTHORIZED,"COMMON401","인증이 필요합니다"),
    _FORBIDDEN(HttpStatus.FORBIDDEN,"COMMON402","금지된 요청입니다."),
    _NOT_FOUND(HttpStatus.NOT_FOUND,"COMMON403","데이터를 찾지 못했습니다."),

    // jwt 관련 오류
    _NOT_FOUND_JWT(HttpStatus.NOT_FOUND,"JWT400_1","JWT 토큰이 없습니다."),
    _EXFIRED_JWT(HttpStatus.BAD_REQUEST,"JWT400_2","만료된 ACCESS 토큰입니다."),
    _INVALID_JWT(HttpStatus.BAD_REQUEST,"JWT400_3","유효하지 않은 JWT 입니다."),
    _INVALID_ACCESS_JWT(HttpStatus.BAD_REQUEST,"JWT400_4","유효하지 않은 ACCEESS 토큰입니다.")
    ;
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDto getReason() {
        return ErrorReasonDto.builder()
                .code(code)
                .message(message)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDto getReasonHttpStatus() {
        return ErrorReasonDto.builder()
                .code(code)
                .message(message)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }
}
