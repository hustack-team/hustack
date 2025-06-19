package com.hust.baseweb.applications.education.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DownloadSummissionsIM {

    @NotNull(message = "Được yêu cầu")
    @Size(min = 1, message = "Phải chứa ít nhất một phần tử")
    private List<String> studentIds;
}
