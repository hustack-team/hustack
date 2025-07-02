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
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.IOUtils;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ContestProblemExportService {

    ProblemRepo problemRepo;
    TestCaseRepo testCaseRepo;
    MongoContentService mongoContentService;
    private final ProblemBlockRepo problemBlockRepo;

    public ByteArrayOutputStream exportProblemDescriptionToStream(ModelCreateContestProblemResponse problem) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

        bufferedWriter.write(problem.getProblemDescription());

        bufferedWriter.close();
        outputStreamWriter.close();
        return stream;
    }

    public ByteArrayOutputStream exportProblemInfoToStream(ModelCreateContestProblemResponse problem) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

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
        outputStreamWriter.close();
        return stream;
    }

    public ByteArrayOutputStream exportSourceToStream(String filenamePrefix, String sourceCode, String language) throws IOException {
        String ext = ComputerLanguage.mapLanguageToExtension(ComputerLanguage.Languages.valueOf(language));
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

        bufferedWriter.write(sourceCode);

        bufferedWriter.close();
        outputStreamWriter.close();
        return stream;
    }

    public ByteArrayOutputStream exportProblemCorrectSolutionToStream(ModelCreateContestProblemResponse problem) throws IOException {
        return exportSourceToStream("Solution", problem.getCorrectSolutionSourceCode(), problem.getCorrectSolutionLanguage());
    }

    public ByteArrayOutputStream exportProblemCustomCheckerToStream(ModelCreateContestProblemResponse problem) throws IOException {
        return exportSourceToStream("CustomSolutionChecker", problem.getSolutionCheckerSourceCode(), problem.getSolutionCheckerSourceLanguage());
    }

    public ByteArrayOutputStream exportProblemToJsonStream(ModelCreateContestProblemResponse problem) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        Map<String, Object> problemData = new HashMap<>();
        problemData.put("problemName", problem.getProblemName());
        problemData.put("createdAt", problem.getCreatedAt());
        problemData.put("userId", problem.getUserId());
        problemData.put("isPublicProblem", problem.isPublicProblem());
        problemData.put("timeLimitCPP", problem.getTimeLimitCPP());
        problemData.put("timeLimitJAVA", problem.getTimeLimitJAVA());
        problemData.put("timeLimitPYTHON", problem.getTimeLimitPYTHON());
        problemData.put("memoryLimit", problem.getMemoryLimit());
        problemData.put("levelId", problem.getLevelId());
        problemData.put("categoryId", problem.getCategoryId());
        problemData.put("levelOrder", problem.getLevelOrder());
        problemData.put("statusId", problem.getStatusId());
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

        if (Integer.valueOf(1).equals(problem.getCategoryId())) {
            List<ProblemBlock> problemBlocks = problemBlockRepo.findByProblemId(problem.getProblemId());
            if (!problemBlocks.isEmpty()) {
                List<BlockCode> blockCodes = problemBlocks.stream()
                                                          .map(this::mapToBlockCode)
                                                          .collect(Collectors.toList());

                problemData.put("blockCodes", blockCodes);
            }
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

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        objectMapper.writeValue(stream, problemData);
        return stream;
    }

    private BlockCode mapToBlockCode(ProblemBlock block) {
        BlockCode blockCode = new BlockCode();
        blockCode.setId(String.valueOf(block.getId()));
        blockCode.setCode(block.getSourceCode());
        blockCode.setForStudent(block.getCompletedBy());
        blockCode.setSeq(block.getSeq());
        blockCode.setLanguage(block.getProgrammingLanguage());
        return blockCode;
    }

    public ByteArrayOutputStream exportProblemGeneralDescriptionToTxtStream(ModelCreateContestProblemResponse problem) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);

        String s = "Id: " + problem.getProblemId() + "\n" +
                   "Problem: " + problem.getProblemName() + "\n" +
                   "Created at: " + problem.getCreatedAt() + " by " + problem.getUserId() + "\n" +
                   "Public: " + problem.isPublicProblem() + "\n" +
                   "Time limit: " + problem.getTimeLimit() + " s\n" +
                   "Memory limit: " + problem.getMemoryLimit() + " MB\n" +
                   "Level: " + problem.getLevelId() + "\n" +
                   "Tags: " + problem.getTags().stream().map(TagEntity::getName).collect(Collectors.toList()) + "\n" +
                   "Score evaluation type: " + problem.getScoreEvaluationType() + "\n" +
                   "\nProblem Description\n" +
                   problem.getProblemDescription();

        bufferedWriter.write(s);

        bufferedWriter.close();
        outputStreamWriter.close();
        return stream;
    }

    public List<Map.Entry<String, ByteArrayOutputStream>> exportProblemAttachmentToStream(ModelCreateContestProblemResponse problem) throws IOException {
        ProblemEntity problemEntity = problemRepo.findByProblemId(problem.getProblemId());
        List<Map.Entry<String, ByteArrayOutputStream>> attachments = new ArrayList<>();

        if (!problemEntity.getAttachment().isEmpty()) {
            String[] fileIds = problemEntity.getAttachment().split(";", -1);
            if (fileIds.length != 0) {
                for (String fileId : fileIds) {
                    GridFsResource content = mongoContentService.getById(fileId);
                    if (content != null) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        InputStream inputStream = content.getInputStream();
                        IOUtils.copy(inputStream, stream);
                        inputStream.close();
                        attachments.add(new AbstractMap.SimpleEntry<>(content.getFilename(), stream));
                    }
                }
            }
        }

        return attachments;
    }

    public List<Map.Entry<String, ByteArrayOutputStream>> exportProblemTestCasesToStream(ModelCreateContestProblemResponse problem) throws IOException {
        String problemId = problem.getProblemId();
        List<TestCaseEntity> listTestCase = testCaseRepo.findAllByProblemId(problemId);
        List<Map.Entry<String, ByteArrayOutputStream>> testCaseStreams = new ArrayList<>();

        for (int i = 0; i < listTestCase.size(); i++) {
            TestCaseEntity testCase = listTestCase.get(i);

            ByteArrayOutputStream inputStream = new ByteArrayOutputStream();
            OutputStreamWriter inputWriter = new OutputStreamWriter(inputStream, StandardCharsets.UTF_8);
            BufferedWriter inputBufferedWriter = new BufferedWriter(inputWriter);
            inputBufferedWriter.write(testCase.getTestCase());
            inputBufferedWriter.close();
            inputWriter.close();
            testCaseStreams.add(new AbstractMap.SimpleEntry<>(problemId + "_testcase_" + (i + 1) + "_input.txt", inputStream));

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OutputStreamWriter outputWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            BufferedWriter outputBufferedWriter = new BufferedWriter(outputWriter);
            outputBufferedWriter.write(testCase.getCorrectAnswer());
            outputBufferedWriter.close();
            outputWriter.close();
            testCaseStreams.add(new AbstractMap.SimpleEntry<>(problemId + "_testcase_" + (i + 1) + "_output.txt", outputStream));
        }


        return testCaseStreams;
    }


}
