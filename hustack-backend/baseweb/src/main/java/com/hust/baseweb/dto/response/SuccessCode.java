package com.hust.baseweb.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum SuccessCode implements BaseResponseCode {

    ACCOUNT_DELETED(1001, "success.account.deleted", "Account deleted successfully"),
    ACCOUNT_DISABLED(1002, "success.account.disabled", "Account disabled because it was in use"),
    PASSWORD_RESET_SUCCESS(1003, "success.account.password_reset", "Password reset successfully"),
    ACCOUNT_STATUS_UPDATED(1004, "success.account.status_updated", "Account status updated successfully"),
    ACCOUNT_STATUS_UNCHANGED(1005, "success.account.status_unchanged", "Account status is already correct"),
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

