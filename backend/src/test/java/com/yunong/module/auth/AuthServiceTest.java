package com.yunong.module.auth;

import com.yunong.module.auth.dto.RegisterRequest;
import com.yunong.module.auth.entity.User;
import com.yunong.module.auth.mapper.UserMapper;
import com.yunong.module.auth.service.AuthService;
import com.yunong.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock AuthenticationManager authenticationManager;

    @Test
    void publicRegistrationMustAlwaysCreateFarmer() {
        var request = new RegisterRequest();
        request.setUsername("attacker");
        request.setPassword("secret123");
        request.setRealName("测试用户");
        request.setRole("ROLE_ADMIN");

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("u-1");
            return 1;
        });
        var authentication = new UsernamePasswordAuthenticationToken("attacker", "secret123");
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtTokenProvider.generateAccessToken(authentication)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken("u-1")).thenReturn("refresh-token");

        new AuthService(userMapper, passwordEncoder, jwtTokenProvider, authenticationManager).register(request);

        var captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("ROLE_FARMER", captor.getValue().getRole());
    }
}
