package com.hust.baseweb.dto.response;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum SuccessCode implements BaseResponseCode {

    ACCOUNT_DELETED(1001, "success.account.deleted", "The account was deleted successfully"),
    ACCOUNT_DISABLED(1002, "success.account.disabled", "The account has been disabled and cannot be deleted because it has already been used"),
    PASSWORD_RESET_SUCCESS(1003, "success.account.password_reset", "The password was reset successfully"),
    ACCOUNT_STATUS_UPDATED(1004, "success.account.status_updated", "The account status was updated successfully"),
    ACCOUNT_STATUS_UNCHANGED(1005, "success.account.status_unchanged", "Account status is already correct"),
    // TODO: Nếu hệ thống hỗ trợ đa ngôn ngữ, hãy map messageCode sang file dịch tương ứng để trả về đúng message cho từng ngôn ngữ (vi, en, ...)
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

