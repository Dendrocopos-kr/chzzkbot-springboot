package org.dendrocopos.guribot.core.api;

import lombok.Builder;
import org.dendrocopos.guribot.core.utils.ErrorCode;
import org.springframework.web.ErrorResponse;

public class ApiResponse<T> {
    private ApiHeader header;
    private ApiBody body;
    private static int SUCCESS = 200;

    public ApiResponse(ApiHeader header) {
        this.header = header;
    }

    public ApiResponse(ApiHeader header, ApiBody body) {
        this.header = header;
        this.body = body;
    }

    public static <T> ApiResponse<T> OK(T data) {
        return new ApiResponse<>(
                ApiHeader.builder().resultCode(SUCCESS).codeName("SUCCESS").build()
                ,ApiBody.builder().data(data).msg(null).build()
        );
    }

    public static <T> ApiResponse<T> FAIL(ErrorCode errorCode) {
        return new ApiResponse<>(
                ApiHeader.builder().resultCode(errorCode.getCode()).codeName(errorCode.name()).build()
                ,ApiBody.builder().data(null).msg(errorCode.getMessage()).build()
        );
    }
}
