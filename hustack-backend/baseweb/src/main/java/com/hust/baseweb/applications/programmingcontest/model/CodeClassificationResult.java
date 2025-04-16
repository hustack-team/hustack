package com.hust.baseweb.applications.programmingcontest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CodeClassificationResult(
        String source,
        @JsonProperty("ai_model") String aiModel // nullable nếu không có
) {}
