package com.hust.baseweb.applications.notifications.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.util.Date;

/**
 * @author Le Anh Tuan
 */
@Getter
@Setter
@Value
public class UpdateMultipleNotificationStatusBody {

    @NotBlank
    String status;

    Date beforeOrAt;
}
