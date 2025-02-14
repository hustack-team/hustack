package com.hust.baseweb.applications.education.model;

import com.hust.baseweb.applications.education.classmanagement.enumeration.RegistStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateRegistStatusIM extends RegistIM {

    @NotNull
    @Size(min = 1, message = "Được yêu cầu")
    Set<@NotBlank(message = "Được yêu cầu") String> studentIds;

    @NotNull
    private RegistStatus status;
}
