package com.yunong.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // 通用错误
    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "不支持的请求方法"),
    CONFLICT(409, "资源冲突"),
    UNSUPPORTED_MEDIA_TYPE(415, "不支持的媒体类型"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // 认证相关 1xxx
    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
    USERNAME_ALREADY_EXISTS(1002, "用户名已存在"),
    TOKEN_EXPIRED(1003, "令牌已过期"),
    TOKEN_INVALID(1004, "令牌无效"),
    PHONE_ALREADY_EXISTS(1005, "手机号已注册"),

    // 用户相关 2xxx
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_DISABLED(2002, "用户已禁用"),
    ROLE_CANNOT_CHANGE_OWN(2003, "不能修改自己的角色"),

    // 农场/地块相关 3xxx
    FARM_NOT_FOUND(3001, "农场不存在"),
    FIELD_NOT_FOUND(3002, "地块不存在"),
    NOT_FARM_OWNER(3003, "不是农场所有者"),
    FARM_STATUS_INVALID(3004, "\u519c\u573a\u72b6\u6001\u4e0d\u5408\u6cd5"),
    FARM_ARCHIVED(3005, "\u519c\u573a\u5df2\u5f52\u6863\uff0c\u8bf7\u5148\u6062\u590d\u519c\u573a"),
    FIELD_HAS_PLANTING_HISTORY(3006, "地块存在种植记录，不能删除"),
    FARM_NAME_REQUIRED(3007, "农场名称不能为空"),
    FARM_AREA_INVALID(3008, "农场面积必须大于0"),
    FIELD_NAME_REQUIRED(3009, "地块名称不能为空"),
    FIELD_AREA_INVALID(3010, "地块面积必须大于0"),
    FIELD_AREA_EXCEEDS_FARM(3011, "地块总面积不能超过农场面积"),
    FARM_AREA_BELOW_FIELDS(3012, "农场面积不能小于已有地块总面积"),
    FIELD_AREA_BELOW_ACTIVE_PLANTING(3013, "地块面积不能小于当前未收获种植面积"),
    FARM_HAS_ACTIVE_PLANTING(3014, "农场存在未收获的种植记录，不能归档"),

    // 作物相关 4xxx
    CROP_NOT_FOUND(4001, "作物不存在"),
    PLANTING_CYCLE_NOT_FOUND(4002, "种植周期不存在"),
    CYCLE_ALREADY_COMPLETED(4003, "种植周期已完成"),
    CROP_STATUS_INVALID(4004, "\u4f5c\u7269\u72b6\u6001\u4e0d\u5408\u6cd5"),
    CYCLE_HAS_BUSINESS_HISTORY(4005, "\u79cd\u690d\u5468\u671f\u5b58\u5728\u89c2\u6d4b\u6216\u519c\u4e8b\u4efb\u52a1\uff0c\u4e0d\u80fd\u5220\u9664"),
    CROP_INACTIVE(4006, "作物已停用，请选择启用中的作物"),
    PLANTING_REQUIRED_FIELDS(4007, "种植记录缺少必填信息"),
    PLANTING_DATE_INVALID(4008, "种植和收获日期不符合先后顺序"),
    PLANTING_AREA_INVALID(4009, "种植面积必须大于0"),
    PLANTING_AREA_EXCEEDS_FIELD(4010, "当前未收获种植总面积不能超过地块面积"),
    PLANTING_STATUS_INVALID(4011, "种植周期状态不合法"),
    PLANTING_COMPLETION_INVALID(4012, "结束种植周期时必须填写有效的实际收获日期"),
    CROP_DATA_INVALID(4013, "\u4f5c\u7269\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a\uff0c\u751f\u957f\u5468\u671f\u548c\u9002\u5b9c\u6e29\u5ea6\u8303\u56f4\u5fc5\u987b\u6709\u6548"),

    // 诊断相关 5xxx
    DIAGNOSIS_NOT_FOUND(5001, "诊断记录不存在"),
    IMAGE_UPLOAD_FAILED(5002, "图片上传失败"),
    IMAGE_HASH_DUPLICATE(5003, "图片已存在(重复上传)"),
    DIAGNOSIS_ALREADY_REVIEWED(5004, "诊断已审核"),
    ONLY_TECHNICIAN_CAN_REVIEW(5005, "仅农技人员可审核"),

    // 任务相关 6xxx
    TASK_NOT_FOUND(6001, "任务不存在"),
    TASK_STATUS_INVALID(6002, "任务状态不合法"),
    NOT_TASK_ASSIGNEE(6003, "不是任务执行人"),
    TASK_CANNOT_CANCEL(6004, "只有待执行任务可以取消"),
    TASK_DATA_INVALID(6005, "农事任务缺少必填信息或优先级不合法"),
    TASK_DATE_INVALID(6006, "任务日期不合法"),
    TASK_CANNOT_EDIT(6007, "已完成或已取消任务不能修改业务信息"),
    TASK_STATUS_TRANSITION_INVALID(6008, "任务状态不能按此顺序流转"),
    TASK_CYCLE_INVALID(6009, "关联的种植周期不存在"),

    // 知识库相关 7xxx
    DOCUMENT_NOT_FOUND(7001, "文档不存在"),
    DOCUMENT_VERSION_CONFLICT(7002, "文档版本冲突"),

    // 文件相关 8xxx
    FILE_TOO_LARGE(8001, "文件太大"),
    FILE_TYPE_NOT_SUPPORTED(8002, "文件类型不支持"),
    MINIO_ERROR(8003, "文件存储服务异常"),

    // 模型相关 9xxx
    MODEL_NOT_FOUND(9001, "模型版本不存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
