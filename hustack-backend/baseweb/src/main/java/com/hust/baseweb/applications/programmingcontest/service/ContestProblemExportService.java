package com.hust.baseweb.applications.programmingcontest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hust.baseweb.applications.contentmanager.repo.MongoContentService;
import com.hust.baseweb.applications.programmingcontest.constants.Constants;
import com.hust.baseweb.applications.programmingcontest.entity.ProblemBlock;
import com.hust.baseweb.applications.programmingcontest.entity.ProblemEntity;
import com.hust.baseweb.applications.programmingcontest.entity.TagEntity;
import com.hust.baseweb.applications.programmingcontest.entity.TestCaseEntity;
import com.hust.baseweb.applications.programmingcontest.model.BlockCode;
import com.hust.baseweb.applications.programmingcontest.model.ModelCreateContestProblemResponse;
import com.hust.baseweb.applications.programmingcontest.repo.ProblemBlockRepo;
import com.hust.baseweb.applications.programmingcontest.repo.ProblemRepo;
import com.hust.baseweb.applications.programmingcontest.repo.TestCaseRepo;
import com.hust.baseweb.applications.programmingcontest.utils.ComputerLanguage;
import lombok.AllArgsConstructor;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;

import java.io.*;
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
    private final ProblemBlockRepo problemBlockRepo;
    private MongoContentService mongoContentService;

    public File exportProblemDescriptionToFile(ModelCreateContestProblemResponse problem) throws IOException {
        File file = new File("ProblemDescription.html");

        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

        bufferedWriter.write(problem.getProblemDescription());

        bufferedWriter.close();
        fileWriter.close();

        return file;
    }


    public File exportProblemToJsonFile(ModelCreateContestProblemResponse problem) throws IOException {
        File file = new File("ProblemData.json");
        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
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

        if (problem.getCategoryId() != null && problem.getCategoryId() == 1) {
            List<ProblemBlock> problemBlocks = problemBlockRepo.findByProblemId(problem.getProblemId());
            List<Map<String, Object>> blockCodeData = new ArrayList<>();
            for (ProblemBlock block : problemBlocks) {
                Map<String, Object> blockData = new HashMap<>();
                blockData.put("seq", block.getSeq());
                blockData.put("completedBy", block.getCompletedBy() == 0 ? "teacher" : "student");
                blockData.put("sourceCode", block.getSourceCode());
                blockData.put("programmingLanguage", block.getProgrammingLanguage());
                blockCodeData.add(blockData);
            }
            problemData.put("blockCodes", blockCodeData);
        }

        String jsonString = objectMapper.writeValueAsString(problemData);
        bufferedWriter.write(jsonString);
        bufferedWriter.close();
        fileWriter.close();

        return file;
    }

    public List<File> exportProblemBlockToFile(ModelCreateContestProblemResponse problem) throws IOException {
        String problemId = problem.getProblemId();
        String problemName = problem.getProblemName();
        List<ProblemBlock> problemBlocks = problemBlockRepo.findByProblemId(problemId);
        Map<String, List<ProblemBlock>> blocksByLanguage = new HashMap<>();

        for (ProblemBlock block : problemBlocks) {
            String language = block.getProgrammingLanguage();
            blocksByLanguage.computeIfAbsent(language, k -> new ArrayList<>()).add(block);
        }

        List<File> files = new ArrayList<>();
        for (Map.Entry<String, List<ProblemBlock>> entry : blocksByLanguage.entrySet()) {
            String language = entry.getKey();
            List<ProblemBlock> blocks = entry.getValue();

            String fileName = problemName + "_" + language + "_BlockCode";
            File file = new File(fileName);

            try (FileWriter fileWriter = new FileWriter(file);
                 BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                for (ProblemBlock block : blocks) {
                    String target = block.getCompletedBy() == 0 ? "-----forTeacher-----" : "-----forStudent-----";
                    bufferedWriter.write(target);
                    bufferedWriter.newLine();
                    bufferedWriter.write(block.getSourceCode());
                    bufferedWriter.newLine();
                    bufferedWriter.newLine();
                }
            }
            files.add(file);
        }

        return files;
    }

    public File exportProblemInfoToFile(ModelCreateContestProblemResponse problem) throws IOException {
        File file = new File("ProblemGeneralInformation.html");

        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

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

        bufferedWriter.close();
        fileWriter.close();

        return file;
    }

    public File exportProblemCorrectSolutionToFile(ModelCreateContestProblemResponse problem) throws IOException {
        String ext = ComputerLanguage.mapLanguageToExtension(ComputerLanguage.Languages.valueOf(problem.getCorrectSolutionLanguage()));
        File file = new File("Solution" + ext);

        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

        bufferedWriter.write(problem.getCorrectSolutionSourceCode());

        bufferedWriter.close();
        fileWriter.close();

        return file;
    }

    public File exportProblemCustomCheckerToFile(ModelCreateContestProblemResponse problem) throws IOException {
        String ext = ComputerLanguage.mapLanguageToExtension(ComputerLanguage.Languages.valueOf(problem.getSolutionCheckerSourceLanguage()));
        File file = new File("CustomSolutionChecker" + ext);

        FileWriter fileWriter = new FileWriter(file);
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

        bufferedWriter.write(problem.getSolutionCheckerSourceCode());

        bufferedWriter.close();
        fileWriter.close();

        return file;
    }

    public List<File> exportProblemAttachmentToFile(ModelCreateContestProblemResponse problem) throws IOException {
        ProblemEntity problemEntity = problemRepo.findByProblemId(problem.getProblemId());
        List<File> attachments = new ArrayList<>();

        if (!problemEntity.getAttachment().isEmpty()) {
            String[] fileIds = problemEntity.getAttachment().split(";", -1);
            if (fileIds.length != 0) {
                for (String fileId : fileIds) {
                    GridFsResource content = mongoContentService.getById(fileId);
                    if (content != null) {
                        InputStream inputStream = content.getInputStream();

                        File file = new File(content.getFilename());
                        OutputStream outputFileStream = new FileOutputStream(file);
                        IOUtils.copy(inputStream, outputFileStream);

                        attachments.add(file);
                    }
                }
            }
        }

        return attachments;
    }

    public List<File> exportProblemTestCasesToFile(ModelCreateContestProblemResponse problem) throws IOException {
        String problemId = problem.getProblemId();

        List<TestCaseEntity> listTestCase = testCaseRepo.findAllByProblemId(problemId);
        List<File> listTestCaseFile = new ArrayList<>();

        for (int i = 0; i < listTestCase.size(); i++) {
            TestCaseEntity testCase = listTestCase.get(i);
            File file = new File(problemId + "_testcase_" + (i + 1) + ".txt");

            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            String s = "- Problem: " + problem.getProblemName() + "\n" +
                       "- Testcase point: " + testCase.getTestCasePoint() + "\n" +
                       "- Public: " + testCase.getIsPublic() + "\n" +
                       "- Status: " + testCase.getStatusId() + "\n" +
                       "- Description: " + testCase.getDescription() + "\n\n" +
                       "- Input: \n" + testCase.getTestCase() + "\n\n" +
                       "- Output: \n" + testCase.getCorrectAnswer() + "\n";

            bufferedWriter.write(s);

            bufferedWriter.close();
            fileWriter.close();

            listTestCaseFile.add(file);
        }

        return listTestCaseFile;
    }

}
