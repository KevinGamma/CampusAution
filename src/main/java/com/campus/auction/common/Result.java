package com.campus.auction.common;

/**
 * Unified JSON response envelope: { "code": <http-status>, "msg": "...", "data": ... }
 */
public record Result<T>(int code, String msg, T data) {

    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "OK", data);
    }

    public static <T> Result<T> created(T data) {
        return new Result<>(201, "Created", data);
    }

    /** Convenience factory for void operations (e.g. DELETE) that succeed with no body. */
    public static Result<Void> empty() {
        return new Result<>(200, "OK", null);
    }

    public static Result<Void> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}
