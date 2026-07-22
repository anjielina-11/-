package com.yunong.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public static <T> R<T> ok(T data) {
        return new R<>(200, "success", data, Instant.now().toEpochMilli());
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null, Instant.now().toEpochMilli());
    }

    public static <T> R<T> fail(int code, String message, T data) {
        return new R<>(code, message, data, Instant.now().toEpochMilli());
    }

    public static <T> R<T> fail(String message) {
        return fail(500, message);
    }
}
