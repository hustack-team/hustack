package vn.edu.hust.soict.judge0client.service;


import vn.edu.hust.soict.judge0client.config.Judge0Config;
import vn.edu.hust.soict.judge0client.entity.*;

import java.util.List;

public interface Judge0Service {

    Judge0Submission createASubmission(Judge0Config.ServerConfig serverConfig, Judge0Submission submission, Boolean base64Encoded, Boolean wait);

    Judge0Submission getASubmission(Judge0Config.ServerConfig serverConfig, String token, Boolean base64Encoded, List<Judge0SubmissionFields> fields);

    Judge0SubmissionsPage getSubmissions(Judge0Config.ServerConfig serverConfig, Boolean base64Encoded, Integer page, Integer perPage, List<Judge0SubmissionFields> fields);

    Judge0Submission deleteASubmission(Judge0Config.ServerConfig serverConfig, String token, List<Judge0SubmissionFields> fields);

    List<Judge0Submission> createASubmissionBatch(Judge0Config.ServerConfig serverConfig, Boolean base64Encoded, Judge0Submission... submissions);

    Judge0SubmissionBatch getASubmissionBatch(Judge0Config.ServerConfig serverConfig, List<String> tokens, Boolean base64Encoded, List<Judge0SubmissionFields> fields);

    List<Judge0Language> getLanguages(Judge0Config.ServerConfig serverConfig);

    Judge0Language getALanguages(Judge0Config.ServerConfig serverConfig, Integer id);

    List<Judge0Language> getActiveAndArchivedLanguages(Judge0Config.ServerConfig serverConfig);

    List<Judge0Status> getStatuses(Judge0Config.ServerConfig serverConfig);
}
