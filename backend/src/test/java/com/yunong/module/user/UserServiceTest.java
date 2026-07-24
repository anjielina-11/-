package com.yunong.module.user;

import com.yunong.exception.BusinessException;
import com.yunong.module.auth.entity.User;
import com.yunong.module.auth.mapper.UserMapper;
import com.yunong.module.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.yunong.module.user.dto.AdminUserRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UserServiceTest {
    @Test
    void mapsFrontendRoleAndRejectsUnknownRole() {
        UserMapper mapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        var user = new User();
        user.setId("u-1");
        user.setRole("ROLE_FARMER");
        when(mapper.selectById("u-1")).thenReturn(user);
        var service = new UserService(mapper, passwordEncoder);

        service.updateRole("u-1", "tech", "admin-1");
        assertEquals("ROLE_TECHNICIAN", user.getRole());
        verify(mapper).updateById(user);

        assertThrows(BusinessException.class, () -> service.updateRole("u-1", "super_admin", "admin-1"));
    }

    @Test
    void adminCreatesUserWithEncodedPassword() {
        UserMapper mapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(mapper.selectCount(any())).thenReturn(0L);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        var service = new UserService(mapper, passwordEncoder);
        var request = new AdminUserRequest("tech01", "secret123", "李农技",
                "13800000000", "tech@example.com", "tech", 1);

        var created = service.create(request);

        assertEquals("encoded", created.getPasswordHash());
        assertEquals("ROLE_TECHNICIAN", created.getRole());
        verify(mapper).insert(created);
    }

    @Test
    void rejectsChangingOrDisablingOwnAdminAccount() {
        UserMapper mapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        var service = new UserService(mapper, passwordEncoder);

        assertThrows(BusinessException.class,
                () -> service.updateRole("admin-1", "farmer", "admin-1"));
        assertThrows(BusinessException.class,
                () -> service.updateStatus("admin-1", 0, "admin-1"));
    }
}
