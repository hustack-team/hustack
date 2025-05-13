package com.hust.baseweb.applications.programmingcontest.model;

import java.util.List;

public record CodeClassificationResponse(
    List<CodeClassificationResult> results
) {}
