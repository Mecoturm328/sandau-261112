package com.mori.dashboard.common;

/**
 * 统一接口返回结构：{"code":200,"message":"success","data":{...}}。
 */
public record Result<T>(int code, String message, T data) {

    public static final int SUCCESS = 200;

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS, "success", data);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
