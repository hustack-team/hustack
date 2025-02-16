package com.hust.baseweb.applications.education.model;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RegistIM {

    @NotNull
    UUID classId;
}
