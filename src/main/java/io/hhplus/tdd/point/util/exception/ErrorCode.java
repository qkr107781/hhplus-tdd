package io.hhplus.tdd.point.util.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    ZERO_POINT(ErrorStatusCode.INPUT_VALID, "ZERO_POINT", "충천 포인트 0P"),
    LIMIT_ONETIME_CHARGE_AMOUNT(ErrorStatusCode.INPUT_VALID, "LIMIT_ONETIME_CHARGE_AMOUNT", "최대 충천 포인트 초과"),
    OVER_CHARGE(ErrorStatusCode.INPUT_VALID, "OVER_CHARGE", "최대 잔고 초과"),
    NOT_ENOUGH_VALANCE(ErrorStatusCode.USER_VALID, "NOT_ENOUGH_VALANCE", "잔여 포인트 부족")
    ;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    private final int status;
    private final String code;
    private final String message;

    static class ErrorStatusCode{
        private final static int INPUT_VALID = 600;
        private final static int USER_VALID = 602;
    }
}