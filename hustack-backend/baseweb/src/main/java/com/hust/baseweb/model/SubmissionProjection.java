package com.hust.baseweb.model;

import java.util.Date;

public interface SubmissionProjection {

    String getContestSubmissionId();

    String getProblemId();

    String getContestId();

    String getUserId();

    String getFullName();

    String getTestCasePass();

    String getSourceCodeLanguage();

    String getStatus();

    Date getCreatedAt();

    String getCreatedByIp();

}
