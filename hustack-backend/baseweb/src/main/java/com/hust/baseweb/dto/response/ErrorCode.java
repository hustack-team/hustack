package com.hust.baseweb.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode implements BaseResponseCode {

    ACCOUNT_NOT_GENERATED(10001, "error.account.not_generated", "Account has not been generated"),
    ACCOUNT_NOT_FOUND_IN_EXAM_CLASS(10002, "error.account.not_found_in_exam_class", "Account not found in exam class"),
    ;

    int code;

    String messageCode; // multilingual

    String message;

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessageCode() {
        return messageCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

