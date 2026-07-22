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
    CONFLICT(409, "资源冲突"),
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

    // 作物相关 4xxx
    CROP_NOT_FOUND(4001, "作物不存在"),
    PLANTING_CYCLE_NOT_FOUND(4002, "种植周期不存在"),
    CYCLE_ALREADY_COMPLETED(4003, "种植周期已完成"),

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
