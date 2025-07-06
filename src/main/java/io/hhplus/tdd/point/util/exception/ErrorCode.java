package io.hhplus.tdd.point.util.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    ZERO_POINT(ErrorStatusCode.ZEROPOINT, "ZERO_POINT", "충천 포인트 0P");

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    private final int status;
    private final String code;
    private final String message;

    static class ErrorStatusCode{
        private final static int ZEROPOINT = 600;
    }
}