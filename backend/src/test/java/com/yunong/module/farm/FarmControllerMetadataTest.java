package com.yunong.module.farm;

import com.yunong.module.farm.controller.FarmController;
import io.swagger.v3.oas.annotations.Operation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FarmControllerMetadataTest {

    @Test
    @DisplayName("可访问农场接口应提供可读的 Swagger 描述")
    void accessibleFarmEndpointShouldHaveReadableSummary() throws NoSuchMethodException {
        var method = FarmController.class.getMethod("listAccessible", int.class, int.class, boolean.class);

        assertEquals("可访问农场列表", method.getAnnotation(Operation.class).summary());
    }
}
