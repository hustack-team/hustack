package com.hust.baseweb.applications.programmingcontest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hust.baseweb.applications.contentmanager.repo.MongoContentService;
import com.hust.baseweb.applications.programmingcontest.constants.Constants;
import com.hust.baseweb.applications.programmingcontest.entity.ProblemEntity;
import com.hust.baseweb.applications.programmingcontest.entity.TagEntity;
import com.hust.baseweb.applications.programmingcontest.entity.TestCaseEntity;
import com.hust.baseweb.applications.programmingcontest.model.ModelCreateContestProblemResponse;
import com.hust.baseweb.applications.programmingcontest.repo.ProblemRepo;
import com.hust.baseweb.applications.programmingcontest.repo.TestCaseRepo;
import com.hust.baseweb.applications.programmingcontest.utils.ComputerLanguage;
import lombok.AllArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor(onConstructor = @__(@Autowired))
public class ContestProblemExportService {

    private final ProblemRepo problemRepo;
    private final TestCaseRepo testCaseRepo;
    private MongoContentService mongoContentService;

    public File exportProblemDescriptionToFile(ModelCreateContestProblemResponse problem, Path exportDir) throws IOException {
        File file = exportDir.resolve("ProblemDescription.html").toFile();

        try (FileWriter fileWriter = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(problem.getProblemDescription());
        }

        return file;
    }

    public File exportProblemInfoAsTextToFile(ModelCreateContestProblemResponse problem, Path exportDir) throws IOException {
        File file = exportDir.resolve("ProblemGeneralInformation.txt").toFile();

        try (FileWriter fileWriter = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {

            StringBuilder sb = new StringBuilder();
            sb.append("Id: ").append(problem.getProblemId()).append("\n");
            sb.append("Problem: ").append(problem.getProblemName()).append("\n");
            sb.append("Created at: ").append(problem.getCreatedAt())
              .append(" by ").append(problem.getUserId()).append("\n");
            sb.append("Public: ").append(problem.isPublicProblem()).append("\n");
            sb.append("Time limit: ").append(problem.getTimeLimit()).append(" s\n");
            sb.append("Memory limit: ").append(problem.getMemoryLimit()).append(" MB\n");
            sb.append("Level: ").append(problem.getLevelId()).append("\n");
            sb.append("Tags: ").append(problem.getTags().stream()
                                              .map(TagEntity::getName)
                                              .collect(Collectors.joining(", "))).append("\n");
            sb.append("Score evaluation type: ").append(problem.getScoreEvaluationType()).append("\n\n");

            sb.append("==== Problem Description ====\n");
            String plainTextDescription = problem.getProblemDescription()
                                                 .replaceAll("\\<.*?\\>", "")
                                                 .replaceAll(" ", " ")
                                                 .replaceAll("<", "<")
                                                 .replaceAll(">", ">")
                                                 .replaceAll("&", "&");

            sb.append(plainTextDescription).append("\n");

            bufferedWriter.write(sb.toString());
        }

        return file;
    }

    public File exportProblemInfoToFile(ModelCreateContestProblemResponse problem, Path exportDir) throws IOException {
        File file = exportDir.resolve("ProblemGeneralInformation.html").toFile();

        try (FileWriter fileWriter = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {

            String s = "<p>Id: <em>" +
                       problem.getProblemId() +
                       "</em></p>" +
                       "<p><strong>Problem: " +
                       problem.getProblemName() +
                       "</strong></p>" +
                       "<p>Created at: <em>" +
                       problem.getCreatedAt() +
                       "</em> by <em>" +
                       problem.getUserId() +
                       "</em></p>" +
                       "<p>Public: <em>" +
                       problem.isPublicProblem() +
                       "</em></p>" +
                       "<p>Time limit: <em>" +
                       problem.getTimeLimit() +
                       "</em> s</p>" +
                       "<p>Memory limit: <em>" +
                       problem.getMemoryLimit() +
                       "</em> MB</p>" +
                       "<p>Level: <em>" +
                       problem.getLevelId() +
                       "</em></p>" +
                       "<p>Tags: <em>" +
                       problem.getTags().stream().map(TagEntity::getName).collect(Collectors.toList()) +
                       "</em></p>" +
                       "<p>Score evaluation type: <em>" +
                       problem.getScoreEvaluationType() +
                       "</em></p>" +
                       "<br/>" +
                       "<div style = \"padding: 12px; border: 2px gray solid\">" +
                       "<p><strong><em>Problem Description</em></strong></p>" +
                       problem.getProblemDescription() +
                       "</div>";

            bufferedWriter.write(s);
        }

        return file;
    }

    public File exportProblemCorrectSolutionToFile(ModelCreateContestProblemResponse problem, Path exportDir) throws IOException {
        String ext = ComputerLanguage.mapLanguageToExtension(ComputerLanguage.Languages.valueOf(problem.getCorrectSolutionLanguage()));
        File file = exportDir.resolve("Solution" + ext).toFile();

        try (FileWriter fileWriter = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(problem.getCorrectSolutionSourceCode());
        }

        return file;
    }

    public File exportProblemCustomCheckerToFile(ModelCreateContestProblemResponse problem, Path exportDir) throws IOException {
        String ext = ComputerLanguage.mapLanguageToExtension(ComputerLanguage.Languages.valueOf(problem.getSolutionCheckerSourceLanguage()));
        File file = exportDir.resolve("CustomSolutionChecker" + ext).toFile();

        try (FileWriter fileWriter = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(problem.getSolutionCheckerSourceCode());
        }

        return file;
    }

    public File exportProblemToJsonFile(ModelCreateContestProblemResponse problem, Path exportDir) throws IOException {
        File file = exportDir.resolve("ProblemData.json").toFile();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        Map<String, Object> problemData = new HashMap<>();
        problemData.put("problemName", problem.getProblemName());
        problemData.put("createdAt", problem.getCreatedAt());
        problemData.put("userId", problem.getUserId());
        problemData.put("isPublic", problem.isPublicProblem());
        problemData.put("timeLimitCPP", problem.getTimeLimitCPP());
        problemData.put("timeLimitJAVA", problem.getTimeLimitJAVA());
        problemData.put("timeLimitPYTHON", problem.getTimeLimitPYTHON());
        problemData.put("memoryLimit", problem.getMemoryLimit());
        problemData.put("levelId", problem.getLevelId());
        problemData.put("categoryId", problem.getCategoryId());
        problemData.put("levelOrder", problem.getLevelOrder());
        problemData.put("status", problem.getStatus());
        problemData.put("sampleTestCase", problem.getSampleTestCase());
        problemData.put("problemDescription", problem.getProblemDescription());
        problemData.put("correctSolutionLanguage", problem.getCorrectSolutionLanguage());
        problemData.put("correctSolutionSourceCode", problem.getCorrectSolutionSourceCode());
        problemData.put("scoreEvaluationType", problem.getScoreEvaluationType());
        problemData.put("solution", problem.getSolution());
        problemData.put("isPreloadCode", problem.getIsPreloadCode());
        problemData.put("preloadCode", problem.getPreloadCode());

        if (problem.getTags() != null) {
            List<String> tagNames = problem.getTags().stream()
                                           .map(TagEntity::getName)
                                           .collect(Collectors.toList());
            problemData.put("tags", tagNames);
        } else {
            problemData.put("tags", new ArrayList<>());
        }

        if ("CUSTOM_EVALUATION".equals(problem.getScoreEvaluationType())) {
            problemData.put("solutionCheckerSourceLanguage", problem.getSolutionCheckerSourceLanguage());
            problemData.put("solutionCheckerSourceCode", problem.getSolutionCheckerSourceCode());
        }

        List<TestCaseEntity> testCases = testCaseRepo.findAllByProblemId(problem.getProblemId());
        List<Map<String, Object>> testCaseData = new ArrayList<>();
        for (TestCaseEntity testCase : testCases) {
            Map<String, Object> tc = new HashMap<>();
            tc.put("testCasePoint", testCase.getTestCasePoint());
            tc.put("isPublic", testCase.getIsPublic());
            tc.put("statusId", testCase.getStatusId());
            tc.put("description", testCase.getDescription());
            tc.put("testCase", testCase.getTestCase());
            tc.put("correctAnswer", testCase.getCorrectAnswer());
            testCaseData.add(tc);
        }
        problemData.put("testCases", testCaseData);

        try (FileWriter fileWriter = new FileWriter(file);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            String jsonString = objectMapper.writeValueAsString(problemData);
            bufferedWriter.write(jsonString);
        }

        return file;
    }

    public List<File> exportProblemAttachmentToFile(ModelCreateContestProblemResponse problem, Path exportDir) throws IOException {
        ProblemEntity problemEntity = problemRepo.findByProblemId(problem.getProblemId());
        List<File> attachments = new ArrayList<>();

        if (!problemEntity.getAttachment().isEmpty()) {
            String[] fileIds = problemEntity.getAttachment().split(";", -1);
            if (fileIds.length != 0) {
                for (String fileId : fileIds) {
                    GridFsResource content = mongoContentService.getById(fileId);
                    if (content != null) {
                        File file = exportDir.resolve(content.getFilename()).toFile();
                        try (InputStream inputStream = content.getInputStream();
                             OutputStream outputFileStream = new FileOutputStream(file)) {
                            IOUtils.copy(inputStream, outputFileStream);
                        }
                        attachments.add(file);
                    }
                }
            }
        }

        return attachments;
    }

    public List<File> exportProblemTestCasesToFile(ModelCreateContestProblemResponse problem, Path exportDir) throws IOException {
        String problemId = problem.getProblemId();
        List<TestCaseEntity> listTestCase = testCaseRepo.findAllByProblemId(problemId);
        List<File> listTestCaseFile = new ArrayList<>();

        for (int i = 0; i < listTestCase.size(); i++) {
            TestCaseEntity testCase = listTestCase.get(i);

            File inputFile = exportDir.resolve(problemId + "_testcase_" + (i + 1) + "_input.txt").toFile();
            try (FileWriter inputWriter = new FileWriter(inputFile);
                 BufferedWriter inputBufferedWriter = new BufferedWriter(inputWriter)) {
                inputBufferedWriter.write(testCase.getTestCase());
            }

            File outputFile = exportDir.resolve(problemId + "_testcase_" + (i + 1) + "_output.txt").toFile();
            try (FileWriter outputWriter = new FileWriter(outputFile);
                 BufferedWriter outputBufferedWriter = new BufferedWriter(outputWriter)) {
                outputBufferedWriter.write(testCase.getCorrectAnswer());
            }

            listTestCaseFile.add(inputFile);
            listTestCaseFile.add(outputFile);
        }

        return listTestCaseFile;
    }
}
