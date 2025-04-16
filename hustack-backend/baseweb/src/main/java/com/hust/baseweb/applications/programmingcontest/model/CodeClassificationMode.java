package com.hust.baseweb.applications.programmingcontest.model;

import lombok.Getter;

@Getter
public enum CodeClassificationMode {
    NORMAL("normal"),
    ADVANCED("advanced");

    private final String value;

    CodeClassificationMode(String value) {
        this.value = value;
    }

}
