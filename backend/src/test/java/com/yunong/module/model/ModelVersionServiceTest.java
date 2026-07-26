package com.yunong.module.model;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.integration.ai.AiServiceClient;
import com.yunong.integration.ai.dto.ModelActivateRequest;
import com.yunong.integration.ai.dto.ModelRuntimeResponse;
import com.yunong.module.model.entity.ModelVersion;
import com.yunong.module.model.mapper.ModelVersionMapper;
import com.yunong.module.model.service.ModelVersionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("模型版本服务测试")
class ModelVersionServiceTest {

    @Mock private ModelVersionMapper mapper;
    @Mock private AiServiceClient aiServiceClient;
    @InjectMocks private ModelVersionService service;

    @Test
    @DisplayName("创建模型版本默认状态为 training")
    void createWithDefaultStatus() {
        when(mapper.insert(any(ModelVersion.class))).thenAnswer(invocation -> {
            ModelVersion model = invocation.getArgument(0);
            model.setId("mv-001");
            return 1;
        });

        var model = new ModelVersion();
        model.setModelName("yunnong-classifier");
        model.setVersion("1.0.0");

        var result = service.create(model);

        assertThat(result.getStatus()).isEqualTo("training");
        assertThat(result.getId()).isEqualTo("mv-001");
    }

    @Test
    @DisplayName("AI Runtime 激活成功后再切换数据库部署状态")
    void deployActivatesRuntimeBeforeUpdatingDatabase() {
        var candidate = deployableModel("mv-001", "v2.0.0", "training");
        var previous = deployableModel("mv-000", "v1.0.0", "deployed");
        when(mapper.selectById("mv-001")).thenReturn(candidate);
        when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(previous));
        when(aiServiceClient.activateModel(any(ModelActivateRequest.class)))
                .thenReturn(runtime("mv-001", "v2.0.0"));

        var result = service.deploy("mv-001");

        var requestCaptor = ArgumentCaptor.forClass(ModelActivateRequest.class);
        InOrder inOrder = inOrder(mapper, aiServiceClient);
        inOrder.verify(mapper).selectById("mv-001");
        inOrder.verify(aiServiceClient).activateModel(requestCaptor.capture());
        inOrder.verify(mapper).selectList(any(LambdaQueryWrapper.class));
        inOrder.verify(mapper).updateById(previous);
        inOrder.verify(mapper).updateById(candidate);

        assertThat(requestCaptor.getValue().modelPath()).isEqualTo("/app/best_model.pth");
        assertThat(requestCaptor.getValue().classToIdxPath()).isEqualTo("/app/class_to_idx.pth");
        assertThat(requestCaptor.getValue().numClasses()).isEqualTo(18);
        assertThat(result.getStatus()).isEqualTo("deployed");
        assertThat(result.getDeployedAt()).isNotNull();
        assertThat(previous.getStatus()).isEqualTo("deprecated");
    }

    @Test
    @DisplayName("AI Runtime 激活失败时数据库状态保持不变")
    void deployFailureDoesNotUpdateDatabase() {
        var candidate = deployableModel("mv-001", "v2.0.0", "training");
        when(mapper.selectById("mv-001")).thenReturn(candidate);
        when(aiServiceClient.activateModel(any(ModelActivateRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "AI 模型加载失败"));

        assertThatThrownBy(() -> service.deploy("mv-001"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AI 模型加载失败");

        assertThat(candidate.getStatus()).isEqualTo("training");
        verify(mapper, never()).selectList(any(LambdaQueryWrapper.class));
        verify(mapper, never()).updateById(any(ModelVersion.class));
    }

    @Test
    @DisplayName("查询 Runtime 状态委托 AI 服务")
    void getRuntimeDelegatesToAiService() {
        var expected = runtime("mv-001", "v2.0.0");
        when(aiServiceClient.getModelRuntime()).thenReturn(expected);

        assertThat(service.getRuntime()).isSameAs(expected);
        verify(aiServiceClient).getModelRuntime();
    }

    @Test
    @DisplayName("更新模型版本指标与运行文件配置")
    void updateModelVersion() {
        var existing = deployableModel("mv-001", "v1.0.0", "training");
        when(mapper.selectById("mv-001")).thenReturn(existing);

        var update = new ModelVersion();
        update.setVersion("v1.0.1");
        update.setAccuracy(new BigDecimal("0.95"));
        update.setClassMappingPath("/app/classes-v2.pth");
        update.setNumClasses(20);

        var result = service.update("mv-001", update);

        assertThat(result.getVersion()).isEqualTo("v1.0.1");
        assertThat(result.getAccuracy()).isEqualTo(new BigDecimal("0.95"));
        assertThat(result.getClassMappingPath()).isEqualTo("/app/classes-v2.pth");
        assertThat(result.getNumClasses()).isEqualTo(20);
        verify(mapper).updateById(existing);
    }

    private ModelVersion deployableModel(String id, String version, String status) {
        var model = new ModelVersion();
        model.setId(id);
        model.setModelName("云农病害识别 ResNet50");
        model.setVersion(version);
        model.setStatus(status);
        model.setModelPath("/app/best_model.pth");
        model.setClassMappingPath("/app/class_to_idx.pth");
        model.setNumClasses(18);
        return model;
    }

    private ModelRuntimeResponse runtime(String id, String version) {
        return new ModelRuntimeResponse(
                id,
                "云农病害识别 ResNet50",
                version,
                "/app/best_model.pth",
                "/app/class_to_idx.pth",
                18,
                0.6,
                true
        );
    }
}