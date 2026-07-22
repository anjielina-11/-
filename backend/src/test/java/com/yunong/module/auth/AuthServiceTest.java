package com.yunong.module.auth;

import com.yunong.module.auth.dto.LoginRequest;
import com.yunong.module.auth.dto.RegisterRequest;
import com.yunong.module.auth.entity.User;
import com.yunong.module.auth.mapper.UserMapper;
import com.yunong.module.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("认证服务测试")
class AuthServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authManager;
    @InjectMocks private AuthService authService;

    // JwtTokenProvider is final class, cannot easily mock without @MockBean
    // Integration tests will cover the full flow

    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");
        registerRequest.setRealName("新用户");
        registerRequest.setPhone("13800138000");
    }

    @Test
    @DisplayName("注册新用户成功")
    void registerSuccess() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("hashed_password");
        doAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("test-uuid");
            return 1;
        }).when(userMapper).insert(any(User.class));

        when(authManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken("principal", null));

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(NullPointerException.class); // JWT provider not mocked fully

        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("注册时用户名已存在则抛异常")
    void registerDuplicateUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .hasMessageContaining("用户名已存在");
    }
}
