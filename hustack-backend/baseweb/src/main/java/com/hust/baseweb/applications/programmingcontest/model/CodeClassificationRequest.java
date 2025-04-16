package com.hust.baseweb.applications.programmingcontest.model;

public record CodeClassificationRequest(
    String code,
    String language,
    CodeClassificationMode mode
) { }
