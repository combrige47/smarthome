package com.smarthome.tools.result;

import lombok.Data;

/**
 * 通用API响应结果类
 * 用于统一前后端交互的数据格式
 * @param <T> 响应数据的类型
 */
@Data
public class Result<T> {
    /**
     * 状态码：
     * 200表示成功
     * 500表示服务器错误
     * 400表示客户端请求错误
     * 401表示未认证（登录失效）
     * 403表示权限不足
     */
    private int code;

    /**
     * 响应消息：描述操作结果（成功/失败原因）
     */
    private String msg;

    /**
     * 响应数据：成功时返回的业务数据，失败时可为null
     */
    private T data;

    /**
     * 私有构造方法，禁止直接创建实例
     * 必须通过静态方法创建
     */
    private Result() {}

    /**
     * 成功响应（带数据）
     * @param msg 成功消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> success(String msg, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    /**
     * 成功响应（不带数据）
     * @param msg 成功消息
     * @param <T> 数据类型（可省略）
     * @return Result对象
     */
    public static <T> Result<T> success(String msg) {
        return success(msg, null);
    }

    /**
     * 失败响应（默认500状态码）
     * @param msg 失败消息
     * @param <T> 数据类型（可省略）
     * @return Result对象
     */
    public static <T> Result<T> fail(String msg) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }

    /**
     * 失败响应（自定义状态码）
     * @param code 状态码
     * @param msg 失败消息
     * @param <T> 数据类型（可省略）
     * @return Result对象
     */
    public static <T> Result<T> fail(int code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }
}
