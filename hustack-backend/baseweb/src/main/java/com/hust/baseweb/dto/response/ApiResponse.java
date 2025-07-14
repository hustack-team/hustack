package com.hust.baseweb.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

@Builder
@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApiResponse<T> {

    int code;

    String messageCode; // multilingual

    Map<String, Object> messageParams; // multilingual

    String message;

    T data;

    public static <T> ApiResponse<T> of(BaseResponseCode code, T data) {
        return new ApiResponse<>(code.getCode(), code.getMessageCode(), null, code.getMessage(), data);
    }

    public static ApiResponse<Void> of(BaseResponseCode code) {
        return of(code, null, null);
    }

    public static ApiResponse<Void> of(BaseResponseCode code, Map<String, Object> params) {
        return of(code, params, null);
    }

    public static ApiResponse<Void> of(BaseResponseCode code, Map<String, Object> params, String overrideMessage) {
        return new ApiResponse<>(
            code.getCode(),
            code.getMessageCode(),
            params,
            StringUtils.isBlank(overrideMessage) ? code.getMessage() : overrideMessage,
            null
        );
    }
}

