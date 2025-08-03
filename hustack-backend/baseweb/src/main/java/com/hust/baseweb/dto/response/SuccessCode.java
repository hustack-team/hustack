package com.hust.baseweb.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum SuccessCode implements BaseResponseCode {

    SUCCESS(200, "Success"),
    ACCOUNT_DELETED(1001, "The account was deleted successfully"),
    ACCOUNT_DISABLED(1002, "The account has been disabled and cannot be deleted because it has already been used"),
    PASSWORD_RESET_SUCCESS(1003, "The password was reset successfully"),
    ACCOUNT_STATUS_UPDATED(1004, "The account status was updated successfully"),
    ACCOUNT_STATUS_UNCHANGED(1005, "Account status is already correct"),
    ACCOUNT_GENERATED_SUCCESS(1006, "The account was generated successfully"),
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

