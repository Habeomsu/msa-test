package main.gateway.global.code;

public interface BaseErrorCode {
    public ErrorReasonDto getReason();
    public ErrorReasonDto getReasonHttpStatus();
}
