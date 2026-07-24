package com.yunong.module.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunong.common.PageResult;
import com.yunong.exception.BusinessException;
import com.yunong.exception.ErrorCode;
import com.yunong.module.model.entity.ModelVersion;
import com.yunong.module.model.mapper.ModelVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ModelVersionService {

    private final ModelVersionMapper mapper;

    public ModelVersion create(ModelVersion mv) {
        if (mv.getStatus() == null) mv.setStatus("draft");
        mapper.insert(mv);
        return mv;
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
        var mv = mapper.selectById(id);
        if (mv == null) throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        return mv;
    }

    public ModelVersion update(String id, ModelVersion update) {
        var mv = getById(id);
        if (update.getModelName() != null) mv.setModelName(update.getModelName());
        if (update.getModelType() != null) mv.setModelType(update.getModelType());
        if (update.getVersion() != null) mv.setVersion(update.getVersion());
        if (update.getAccuracy() != null) mv.setAccuracy(update.getAccuracy());
        if (update.getPrecisionVal() != null) mv.setPrecisionVal(update.getPrecisionVal());
        if (update.getRecallVal() != null) mv.setRecallVal(update.getRecallVal());
        if (update.getF1Score() != null) mv.setF1Score(update.getF1Score());
        if (update.getModelPath() != null) mv.setModelPath(update.getModelPath());
        if (update.getConfigJson() != null) mv.setConfigJson(update.getConfigJson());
        if (update.getDescription() != null) mv.setDescription(update.getDescription());
        mapper.updateById(mv);
        return mv;
    }

    public ModelVersion deploy(String id) {
        var mv = getById(id);
        mv.setStatus("active");
        mv.setDeployedAt(LocalDateTime.now());
        mapper.updateById(mv);
        // 将其他同模型名版本设为 inactive
        var others = mapper.selectList(new LambdaQueryWrapper<ModelVersion>()
                .eq(ModelVersion::getModelName, mv.getModelName())
                .ne(ModelVersion::getId, id)
                .eq(ModelVersion::getStatus, "active"));
        others.forEach(o -> {
            o.setStatus("inactive");
            mapper.updateById(o);
        });
        return mv;
    }

    public void delete(String id) {
        if (mapper.selectById(id) == null) throw new BusinessException(ErrorCode.MODEL_NOT_FOUND);
        mapper.deleteById(id);
    }
}
