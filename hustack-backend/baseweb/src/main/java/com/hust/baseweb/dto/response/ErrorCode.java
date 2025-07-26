package com.hust.baseweb.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode implements BaseResponseCode {

    ACCOUNT_NOT_GENERATED(10001, "The account has not been generated yet"),
    ACCOUNT_NOT_FOUND_IN_EXAM_CLASS(10002, "The account could not be found in this exam class"),
    ACCOUNT_ALREADY_GENERATED(10003, "The account has already been generated"),
    ACCOUNT_GENERATION_FAILED(10004, "Failed to generate account"),
    EXAM_CLASS_NOT_FOUND(10005, "Exam class not found"),
    ;

    int code;

    String message;

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

