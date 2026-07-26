package com.yunong.module.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.integration.ai.AiServiceClient;
import com.yunong.integration.ai.dto.ModelActivateRequest;
import com.yunong.integration.ai.dto.ModelRuntimeResponse;
import com.yunong.module.model.entity.ModelVersion;
import com.yunong.module.model.mapper.ModelVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ModelVersionService {

    private static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.6;

    private final ModelVersionMapper mapper;
    private final AiServiceClient aiServiceClient;

    public ModelVersion create(ModelVersion modelVersion) {
        modelVersion.setStatus("training");
        mapper.insert(modelVersion);
        return modelVersion;
    }

    public PageResult<ModelVersion> list(int page, int size, String modelType, String status) {
        var wrapper = new LambdaQueryWrapper<ModelVersion>();
        if (modelType != null) wrapper.eq(ModelVersion::getModelType, modelType);
        if (status != null) wrapper.eq(ModelVersion::getStatus, status);
        wrapper.orderByDesc(ModelVersion::getCreatedAt);
        var result = mapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }

    public ModelVersion getById(String id) {
        var modelVersion = mapper.selectById(id);
        if (modelVersion == null) throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        return modelVersion;
    }

    public ModelVersion update(String id, ModelVersion update) {
        var modelVersion = getById(id);
        if (update.getModelName() != null) modelVersion.setModelName(update.getModelName());
        if (update.getModelType() != null) modelVersion.setModelType(update.getModelType());
        if (update.getVersion() != null) modelVersion.setVersion(update.getVersion());
        if (update.getAccuracy() != null) modelVersion.setAccuracy(update.getAccuracy());
        if (update.getPrecisionVal() != null) modelVersion.setPrecisionVal(update.getPrecisionVal());
        if (update.getRecallVal() != null) modelVersion.setRecallVal(update.getRecallVal());
        if (update.getF1Score() != null) modelVersion.setF1Score(update.getF1Score());
        if (update.getModelPath() != null) modelVersion.setModelPath(update.getModelPath());
        if (update.getClassMappingPath() != null) modelVersion.setClassMappingPath(update.getClassMappingPath());
        if (update.getNumClasses() != null) modelVersion.setNumClasses(update.getNumClasses());
        if (update.getConfigJson() != null) modelVersion.setConfigJson(update.getConfigJson());
        if (update.getDescription() != null) modelVersion.setDescription(update.getDescription());
        mapper.updateById(modelVersion);
        return modelVersion;
    }

    @Transactional
    public ModelVersion deploy(String id) {
        var candidate = getById(id);
        var runtime = aiServiceClient.activateModel(toActivateRequest(candidate));
        if (!runtime.loaded()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI Runtime 未确认模型加载成功");
        }

        var previousVersions = mapper.selectList(new LambdaQueryWrapper<ModelVersion>()
                .eq(ModelVersion::getModelName, candidate.getModelName())
                .ne(ModelVersion::getId, id)
                .eq(ModelVersion::getStatus, "deployed"));
        previousVersions.forEach(previous -> {
            previous.setStatus("deprecated");
            mapper.updateById(previous);
        });

        candidate.setStatus("deployed");
        candidate.setDeployedAt(LocalDateTime.now());
        mapper.updateById(candidate);
        return candidate;
    }

    public ModelRuntimeResponse getRuntime() {
        return aiServiceClient.getModelRuntime();
    }

    public void delete(String id) {
        if (mapper.selectById(id) == null) throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        mapper.deleteById(id);
    }

    private ModelActivateRequest toActivateRequest(ModelVersion modelVersion) {
        return new ModelActivateRequest(
                modelVersion.getId(),
                modelVersion.getModelName(),
                modelVersion.getVersion(),
                modelVersion.getModelPath(),
                modelVersion.getClassMappingPath(),
                modelVersion.getNumClasses(),
                DEFAULT_CONFIDENCE_THRESHOLD
        );
    }
}