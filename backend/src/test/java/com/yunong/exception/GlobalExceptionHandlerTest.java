package com.yunong.exception;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void badCredentialsReturnBusinessAuthenticationError() {
        var response = new GlobalExceptionHandler()
                .handleAuthentication(new BadCredentialsException("Bad credentials"));

        assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getCode(), response.getCode());
        assertEquals(ErrorCode.USERNAME_OR_PASSWORD_ERROR.getMessage(), response.getMessage());
    }
}
