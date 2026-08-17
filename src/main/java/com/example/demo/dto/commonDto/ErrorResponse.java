package com.example.demo.dto.commonDto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder(toBuilder = true)   // <-- must have toBuilder = true, not just @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private List<FieldError> fieldErrors;
    private String traceId;

    @Getter
    @Builder(toBuilder = true)   // add here too if you ever need FieldError.toBuilder()
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }
}