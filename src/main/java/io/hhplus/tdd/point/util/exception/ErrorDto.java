package io.hhplus.tdd.point.util.exception;

import lombok.Builder;
import lombok.Data;

import org.springframework.http.ResponseEntity;

@Builder
@Data
public class ErrorDto {
    private int status;
    private String code;
    private String message;

    public static ResponseEntity<ErrorDto> toResponseEntity(ErrorCode e) {
        return ResponseEntity.status(e.getStatus())
                            .body(ErrorDto.builder()
                                        .status(e.getStatus())
                                        .code(e.getCode())
                                        .message(e.getMessage())
                                        .build());
    }
}