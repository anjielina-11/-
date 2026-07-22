package com.yunong.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> {

    private int code;
    private String message;
    private T data;

    public static <T> R<T> ok(T data) {
        return new R<>(0, null, data);
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public static <T> R<T> fail(int code, String message, T data) {
        return new R<>(code, message, data);
    }

    public static <T> R<T> fail(String message) {
        return fail(500, message);
    }
}
