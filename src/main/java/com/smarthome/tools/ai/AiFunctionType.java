package com.smarthome.tools.ai;

// 枚举：明确 AI 可处理的功能类型，避免硬编码
public enum AiFunctionType {
    DATA_QUERY("数据查询", "用户需获取特定数据（如设备状态、环境温度）"),
    DEVICE_CONTROL("设备控制", "用户需生成设备开关指令（如开灯、关风扇）"),
    UNKNOWN("未知功能", "无法识别的指令类型");

    private final String typeName; // AI 输出的类型标识（需与提示词一致）
    private final String desc;     // 功能描述

    AiFunctionType(String typeName, String desc) {
        this.typeName = typeName;
        this.desc = desc;
    }

    // 根据 AI 输出的类型字符串匹配枚举（如 AI 输出“数据查询”→匹配 DATA_QUERY）
    public static AiFunctionType matchByTypeName(String typeName) {
        for (AiFunctionType type : values()) {
            if (type.typeName.equals(typeName)) {
                return type;
            }
        }
        return UNKNOWN;
    }

    // Getter
    public String getTypeName() { return typeName; }
    public String getDesc() { return desc; }
}