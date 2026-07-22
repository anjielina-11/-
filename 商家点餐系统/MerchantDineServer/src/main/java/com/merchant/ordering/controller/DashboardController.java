package com.merchant.ordering.controller;

import com.merchant.ordering.common.Result;
import com.merchant.ordering.dto.DashboardVO;
import com.merchant.ordering.entity.User;
import com.merchant.ordering.service.DashboardService;
import com.merchant.ordering.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 数据大屏 Controller — 前缀 /api/dashboard
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserService userService;

    /**
     * 获取大屏数据（仅管理员）
     * GET /api/dashboard
     */
    @GetMapping
    public Result<DashboardVO> getDashboard(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        checkAdmin(userId);
        DashboardVO vo = dashboardService.getDashboardData();
        return Result.ok(vo);
    }

    private void checkAdmin(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("缺少用户认证信息");
        }
        User user = userService.getById(userId);
        if (user == null || user.getRole() == null || user.getRole() != 1) {
            throw new IllegalArgumentException("无权限，仅管理员可操作");
        }
    }
}
