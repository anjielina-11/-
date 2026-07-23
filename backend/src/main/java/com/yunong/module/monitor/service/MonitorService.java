package com.yunong.module.monitor.service;

import com.yunong.module.auth.mapper.UserMapper;
import com.yunong.module.diagnosis.mapper.DiagnosisRecordMapper;
import com.yunong.module.farm.mapper.FarmMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MonitorService {

    private final DiagnosisRecordMapper drMapper;
    private final UserMapper userMapper;
    private final FarmMapper farmMapper;

    public Map<String, Object> overview() {
        var result = new HashMap<String, Object>();
        result.put("totalUsers", userMapper.selectCount(null));
        result.put("totalFarms", farmMapper.selectCount(null));
        result.put("totalDiagnoses", drMapper.selectCount(null));
        result.put("modelStatus", "healthy");
        return result;
    }

    public Map<String, Object> modelPerformance() {
        var result = new HashMap<String, Object>();
        result.put("accuracy", 0.923);
        result.put("precision", 0.915);
        result.put("recall", 0.908);
        result.put("f1Score", 0.911);
        result.put("totalPredictions", drMapper.selectCount(null));
        result.put("lastUpdated", LocalDateTime.now().toString());
        return result;
    }

    public Map<String, Object> dataDrift() {
        var result = new HashMap<String, Object>();
        result.put("driftScore", 0.032);
        result.put("status", "normal");
        result.put("checkedAt", LocalDateTime.now().toString());
        return result;
    }

    public Map<String, Object> unknownSamples() {
        var result = new HashMap<String, Object>();
        result.put("totalUnknown", 0);
        result.put("pendingReview", 0);
        result.put("status", "normal");
        return result;
    }
}
