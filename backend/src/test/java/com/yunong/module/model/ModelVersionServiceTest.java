package com.yunong.module.model;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunong.module.model.entity.ModelVersion;
import com.yunong.module.model.mapper.ModelVersionMapper;
import com.yunong.module.model.service.ModelVersionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("模型版本服务测试")
class ModelVersionServiceTest {

    @Mock private ModelVersionMapper mapper;
    @InjectMocks private ModelVersionService service;

    @Test
    @DisplayName("创建模型版本默认状态为 draft")
    void createWithDefaultStatus() {
        when(mapper.insert(any(ModelVersion.class))).thenAnswer(inv -> {
            ModelVersion mv = inv.getArgument(0);
            mv.setId("mv-001");
            return 1;
        });

        var mv = new ModelVersion();
        mv.setModelName("yunnong-classifier");
        mv.setVersion("1.0.0");
        var result = service.create(mv);
        assertThat(result.getStatus()).isEqualTo("draft");
        assertThat(result.getId()).isEqualTo("mv-001");
    }

    @Test
    @DisplayName("部署模型并下线同型号其他版本")
    void deploy() {
        var mv = new ModelVersion();
        mv.setId("mv-001");
        mv.setModelName("classifier");
        mv.setStatus("draft");
        when(mapper.selectById("mv-001")).thenReturn(mv);

        var oldActive = new ModelVersion();
        oldActive.setId("mv-000");
        oldActive.setModelName("classifier");
        oldActive.setStatus("active");
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(oldActive));

        var result = service.deploy("mv-001");
        assertThat(result.getStatus()).isEqualTo("active");
        assertThat(result.getDeployedAt()).isNotNull();
        assertThat(oldActive.getStatus()).isEqualTo("inactive"); // 旧版本下线
    }

    @Test
    @DisplayName("指定精度指标")
    void createWithMetrics() {
        when(mapper.insert(any(ModelVersion.class))).thenReturn(1);

        var mv = new ModelVersion();
        mv.setModelName("detector");
        mv.setVersion("2.0.0");
        mv.setAccuracy(new BigDecimal("0.945"));
        mv.setF1Score(new BigDecimal("0.931"));
        var result = service.create(mv);
        assertThat(result.getAccuracy()).isEqualTo(new BigDecimal("0.945"));
    }

    @Test
    @DisplayName("更新模型版本指标")
    void updateModelVersion() {
        var existing = new ModelVersion();
        existing.setId("mv-001");
        existing.setModelName("classifier");
        existing.setVersion("1.0.0");
        existing.setStatus("draft");
        when(mapper.selectById("mv-001")).thenReturn(existing);

        var update = new ModelVersion();
        update.setVersion("1.0.1");
        update.setAccuracy(new BigDecimal("0.95"));

        var result = service.update("mv-001", update);

        assertThat(result.getModelName()).isEqualTo("classifier");
        assertThat(result.getVersion()).isEqualTo("1.0.1");
        assertThat(result.getAccuracy()).isEqualTo(new BigDecimal("0.95"));
        verify(mapper).updateById(existing);
    }

}
