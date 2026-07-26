package com.yunong.module.crop;

import com.yunong.module.crop.controller.CropController;
import com.yunong.module.crop.entity.Crop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CropControllerMetadataTest {

    @Test
    @DisplayName("新增作物品种接口应仅允许管理员调用")
    void createCropShouldRequireAdminRole() throws NoSuchMethodException {
        var method = CropController.class.getMethod("createCrop", Crop.class);
        var authorization = method.getAnnotation(PreAuthorize.class);

        assertNotNull(authorization);
        assertEquals("hasRole('ADMIN')", authorization.value());
    }
}