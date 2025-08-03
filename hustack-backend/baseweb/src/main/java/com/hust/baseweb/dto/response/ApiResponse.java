package com.hust.baseweb.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

@Builder
@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApiResponse<T> {

    int code;

    String message;

    T data;

    public static <T> ApiResponse<T> of(BaseResponseCode code, T data) {
        return new ApiResponse<>(code.getCode(), code.getMessage(), data);
    }

    public static ApiResponse<Void> of(BaseResponseCode code) {
        return of(code, null);
    }

    public static ApiResponse<Void> of(BaseResponseCode code, String overrideMessage) {
        return new ApiResponse<>(
            code.getCode(),
            StringUtils.isBlank(overrideMessage) ? code.getMessage() : overrideMessage,
            null
        );
    }
}

