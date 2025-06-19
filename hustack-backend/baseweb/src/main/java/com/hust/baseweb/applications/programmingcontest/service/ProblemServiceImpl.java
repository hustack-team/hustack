package com.hust.baseweb.applications.programmingcontest.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.baseweb.applications.chatgpt.ChatGPTService;
import com.hust.baseweb.applications.contentmanager.model.ContentHeaderModel;
import com.hust.baseweb.applications.contentmanager.model.ContentModel;
import com.hust.baseweb.applications.contentmanager.repo.MongoContentService;
import com.hust.baseweb.applications.education.classmanagement.utils.ZipOutputStreamUtils;
import com.hust.baseweb.applications.notifications.service.NotificationsService;
import com.hust.baseweb.applications.programmingcontest.callexternalapi.service.ApiService;
import com.hust.baseweb.applications.programmingcontest.constants.Constants;
import com.hust.baseweb.applications.programmingcontest.entity.*;
import com.hust.baseweb.applications.programmingcontest.exception.MiniLeetCodeException;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.model.externalapi.SubmissionModelResponse;
import com.hust.baseweb.applications.programmingcontest.repo.*;
import com.hust.baseweb.applications.programmingcontest.service.helper.cache.ProblemTestCaseServiceCache;
import com.hust.baseweb.entity.UserLogin;
import com.hust.baseweb.model.ProblemFilter;
import com.hust.baseweb.model.ProblemProjection;
import com.hust.baseweb.model.dto.ProblemDTO;
import com.hust.baseweb.repo.UserLoginRepo;
import com.hust.baseweb.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.hust.soict.judge0client.entity.Judge0Submission;
import vn.edu.hust.soict.judge0client.service.Judge0Service;
import vn.edu.hust.soict.judge0client.utils.Judge0Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProblemServiceImpl implements ProblemService {
    ProblemRepo problemRepo;

    TeacherGroupRelationRepository teacherGroupRelationRepository;

    TestCaseRepo testCaseRepo;

    UserLoginRepo userLoginRepo;

    ContestRepo contestRepo;

    Constants constants;

    ContestSubmissionRepo contestSubmissionRepo;

    NotificationsService notificationsService;

    ContestSubmissionPagingAndSortingRepo contestSubmissionPagingAndSortingRepo;

    ContestProblemRepo contestProblemRepo;

    UserContestProblemRoleRepo userContestProblemRoleRepo;

    TagRepo tagRepo;

    MongoContentService mongoContentService;

    ProblemCacheService problemCacheService;

    ContestService contestService;

    ContestProblemExportService exporter;

    ProblemTagRepo problemTagRepo;

    ObjectMapper objectMapper;

    IEProblemProperties iEProblemProperties;

    @Override
    @Transactional
    public ProblemEntity createContestProblem(
        String createdBy,
        ModelCreateContestProblem dto,
        MultipartFile[] files
    ) {
        String problemId = dto.getProblemId().trim();

        if (problemRepo.findByProblemId(problemId) != null) {
            throw new DuplicateKeyException("Problem ID already exist");
        }

        List<String> attachmentId = new ArrayList<>();
        String[] fileId = dto.getFileId();
        List<MultipartFile> fileArray = Optional.ofNullable(files)
                                                .map(Arrays::asList)
                                                .orElseGet(Collections::emptyList);

        fileArray.forEach((file) -> {
            ContentModel model = new ContentModel(fileId[fileArray.indexOf(file)], file);

            ObjectId id = null;
            try {
                id = mongoContentService.storeFileToGridFs(model);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (id != null) {
                ContentHeaderModel rs = new ContentHeaderModel(id.toHexString());
                attachmentId.add(rs.getId());
            }
        });

        ProblemEntity problem = ProblemEntity.builder()
                                             .problemId(problemId)
                                             .problemName(dto.getProblemName())
                                             .problemDescription(dto.getProblemDescription())
                                             .memoryLimit(dto.getMemoryLimit())
//                                                   .timeLimit(dto.getTimeLimitCPP()) //TODO: remove this after moving all to lms
                                             .timeLimitCPP(dto.getTimeLimitCPP())
                                             .timeLimitJAVA(dto.getTimeLimitJAVA())
                                             .timeLimitPYTHON(dto.getTimeLimitPYTHON())
                                             .levelId(dto.getLevelId())
                                             .correctSolutionLanguage(dto.getCorrectSolutionLanguage())
                                             .correctSolutionSourceCode(dto.getCorrectSolutionSourceCode())
                                             .solution(dto.getSolution())
                                             .isPreloadCode(dto.getIsPreloadCode())
                                             .preloadCode(dto.getPreloadCode())
                                             .solutionCheckerSourceCode(dto.getSolutionChecker())
                                             .solutionCheckerSourceLanguage(dto.getSolutionCheckerLanguage())
                                             .scoreEvaluationType(dto.getScoreEvaluationType() != null
                                                                      ? dto.getScoreEvaluationType()
                                                                      : Constants.ProblemResultEvaluationType.NORMAL.getValue())
                                             .isPublicProblem(dto.getIsPublic())
                                             .levelOrder(constants.getMapLevelOrder().get(dto.getLevelId()))
                                             .attachment(String.join(";", attachmentId))
                                             .statusId(dto.getStatus().toString())
                                             .sampleTestcase(dto.getSampleTestCase())
                                             .build();
        problem = problemCacheService.saveProblemWithCache(problem);


        List<ProblemTag> problemTags = Arrays.stream(dto.getTagIds())
                                             .map(tagId -> ProblemTag.builder()
                                                                     .id(new ProblemTagId(tagId, problemId))
                                                                     .build())
                                             .collect(Collectors.toList());
        problemTagRepo.saveAll(problemTags);

        // grant role owner, manager, view to current user and admin
        List<String> roleIds = Arrays.asList(
            UserContestProblemRole.ROLE_OWNER,
            UserContestProblemRole.ROLE_EDITOR,
            UserContestProblemRole.ROLE_VIEWER
        );
        List<UserContestProblemRole> roles = new ArrayList<>();
        List<String> users = new ArrayList<>();
        users.add(createdBy);
        if (!"admin".equals(createdBy)) {
            users.add("admin");
        }
        for (String user : users) {
            for (String roleId : roleIds) {
                UserContestProblemRole role = new UserContestProblemRole();

                role.setProblemId(problem.getProblemId());
                role.setUserId(user);
                role.setRoleId(roleId);

                roles.add(role);
            }
        }

        userContestProblemRoleRepo.saveAll(roles);

        // push notification to admin
        notificationsService.create(
            createdBy,
            "admin",
            createdBy + " has created a contest problem ID " + problem.getProblemId(),
            "");

        return problem;
    }

    @Transactional
    @Override
    public ProblemEntity updateContestProblem(
        String problemId,
        String userId,
        ModelUpdateContestProblem dto,
        MultipartFile[] files
    ) throws Exception {
        List<UserContestProblemRole> roles = userContestProblemRoleRepo.findAllByProblemIdAndUserId(
            problemId,
            userId);

        boolean hasPermission = false;
        for (UserContestProblemRole role : roles) {
            if (role.getRoleId().equals(UserContestProblemRole.ROLE_EDITOR) ||
                role.getRoleId().equals(UserContestProblemRole.ROLE_OWNER)) {
                hasPermission = true;
                break;
            }
        }

        if (!hasPermission) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission");
        }

        ProblemEntity problem = problemRepo
            .findById(problemId)
            .orElseThrow(() -> new EntityNotFoundException("Problem ID not found"));
        if (!userId.equals(problem.getCreatedBy())
            &&
            !userContestProblemRoleRepo.existsByProblemIdAndUserIdAndRoleId(
                problemId,
                userId,
                UserContestProblemRole.ROLE_EDITOR)) {
            throw new MiniLeetCodeException("permission denied", 403);
        }

        // problem há been created, admin is shared edit role, but cannot perform the edit
        //if (!userId.equals(problem.getUserId())
        //    && !problem.getStatusId().equals(ProblemEntity.PROBLEM_STATUS_OPEN)) {
        //    throw new MiniLeetCodeException("Problem is not opened for edit", 400);
        //}

        List<TagEntity> tags = new ArrayList<>();
        Integer[] tagIds = dto.getTagIds();
        for (Integer tagId : tagIds) {
            TagEntity tag = tagRepo.findByTagId(tagId);
            tags.add(tag);
        }

        List<String> attachmentId = new ArrayList<>();
        attachmentId.add(problem.getAttachment());
        String[] fileId = dto.getFileId();
        List<MultipartFile> fileArray = Optional.ofNullable(files)
                                                .map(Arrays::asList)
                                                .orElseGet(Collections::emptyList);

        List<String> removedFilesId = dto.getRemovedFilesId();
        if (problem.getAttachment() != null && !problem.getAttachment().isEmpty()) {
            String[] oldAttachmentIds = problem.getAttachment().split(";");
            for (String s : oldAttachmentIds) {
                try {
                    GridFsResource content = mongoContentService.getById(s);
                    if (content != null) {
                        if (removedFilesId.contains(content.getFilename())) {
                            mongoContentService.deleteFilesById(s);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        fileArray.forEach((file) -> {
            ContentModel model = new ContentModel(fileId[fileArray.indexOf(file)], file);

            ObjectId id = null;
            try {
                id = mongoContentService.storeFileToGridFs(model);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (id != null) {
                ContentHeaderModel rs = new ContentHeaderModel(id.toHexString());
                attachmentId.add(rs.getId());
            }
        });

        problem.setProblemName(dto.getProblemName());
        problem.setProblemDescription(dto.getProblemDescription());
        problem.setLevelId(dto.getLevelId());
        problem.setSolution(dto.getSolution());
        problem.setIsPreloadCode(dto.getIsPreloadCode());
        problem.setPreloadCode(dto.getPreloadCode());
//        problem.setTimeLimit(dto.getTimeLimit());
        problem.setTimeLimitCPP(dto.getTimeLimitCPP());
        problem.setTimeLimitJAVA(dto.getTimeLimitJAVA());
        problem.setTimeLimitPYTHON(dto.getTimeLimitPYTHON());
        problem.setMemoryLimit(dto.getMemoryLimit());
        problem.setCorrectSolutionLanguage(dto.getCorrectSolutionLanguage());
        problem.setCorrectSolutionSourceCode(dto.getCorrectSolutionSourceCode());
        problem.setSolutionCheckerSourceCode(dto.getSolutionChecker());
        problem.setSolutionCheckerSourceLanguage(dto.getSolutionCheckerLanguage());
        problem.setScoreEvaluationType(dto.getScoreEvaluationType());
        problem.setPublicProblem(dto.getIsPublic());
        problem.setAttachment(String.join(";", attachmentId));
        problem.setTags(tags);
        problem.setSampleTestcase(dto.getSampleTestCase());

        if (userId.equals(problem.getCreatedBy())) {
            problem.setStatusId(dto.getStatus().toString());
        }

        return problemCacheService.saveProblemWithCache(problem);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ModelStudentOverviewProblem> getStudentContestProblems(String userId, String contestId) {
        ContestEntity contest = contestService.findContest(contestId);
        List<ProblemEntity> problems = contest.getProblems();

        List<String> acceptedProblems = contestSubmissionRepo.findAcceptedProblemsOfUser(userId, contestId);

        List<ModelProblemMaxSubmissionPoint> submittedProblems;
        if (Integer.valueOf(1).equals(contest.getAllowParticipantPinSubmission())) {
            submittedProblems = contestSubmissionRepo.findFinalSelectedSubmittedProblemsOfUser(userId, contestId);
        } else {
            submittedProblems = contestSubmissionRepo.findSubmittedProblemsOfUser(userId, contestId);
        }

        Map<String, Long> mapProblemToMaxSubmissionPoint = submittedProblems.stream()
                                                                            .collect(Collectors.toMap(ModelProblemMaxSubmissionPoint::getProblemId, ModelProblemMaxSubmissionPoint::getMaxPoint));

        Map<String, Long> mProblem2MaxPoint = calculateMaxPointForProblems(contest, contestId);

        if (!ContestEntity.CONTEST_STATUS_RUNNING.equals(contest.getStatusId())) {
            return Collections.emptyList();
        }

        Map<String, ContestProblem> problemId2ContestProblem = contestProblemRepo
            .findByContestIdAndProblemIdIn(contestId, problems.stream().map(ProblemEntity::getProblemId).collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(ContestProblem::getProblemId, cp -> cp));

        List<ModelStudentOverviewProblem> responses = new ArrayList<>();

        for (ProblemEntity problem : problems) {
            String problemId = problem.getProblemId();
            ContestProblem contestProblem = problemId2ContestProblem.get(problemId);

            if (contestProblem == null || ContestProblem.SUBMISSION_MODE_HIDDEN.equals(contestProblem.getSubmissionMode())) {
                continue;
            }

            ModelStudentOverviewProblem response = new ModelStudentOverviewProblem();
            response.setProblemId(problemId);
            response.setProblemName(contestProblem.getProblemRename());
            response.setProblemCode(contestProblem.getProblemRecode());
            response.setLevelId(problem.getLevelId());
            response.setMaxPoint(mProblem2MaxPoint.getOrDefault(problemId, 0L));

            if ("N".equals(contest.getContestShowTag())) {
                response.setTags(new ArrayList<>());
            } else {
                List<String> tags = problem.getTags().stream().map(TagEntity::getName).collect(Collectors.toList());
                response.setTags(tags);
            }

            if (mapProblemToMaxSubmissionPoint.containsKey(problemId)) {
                response.setSubmitted(true);
                response.setMaxSubmittedPoint(mapProblemToMaxSubmissionPoint.get(problemId));
            }

            if (acceptedProblems.contains(problemId)) {
                response.setAccepted(true);
            }

            responses.add(response);
        }

        return responses;
    }

    private Map<String, Long> calculateMaxPointForProblems(ContestEntity contest, String contestId) {
        List<ContestProblem> listContestProblem = contestProblemRepo.findAllByContestIdAndSubmissionModeNot(
            contestId, ContestProblem.SUBMISSION_MODE_HIDDEN);
        List<String> listProblemId = listContestProblem.stream()
                                                       .map(ContestProblem::getProblemId)
                                                       .collect(Collectors.toList());

        List<TestCaseEntity> testCases = testCaseRepo.findAllByProblemIdIn(listProblemId);
        Map<String, List<TestCaseEntity>> testCasesByProblemId = testCases.stream()
                                                                          .collect(Collectors.groupingBy(TestCaseEntity::getProblemId));

        Map<String, Long> result = new HashMap<>();
        for (String problemId : listProblemId) {
            long totalPoint = testCasesByProblemId.getOrDefault(problemId, Collections.emptyList())
                                                  .stream()
                                                  .filter(tc -> "Y".equals(contest.getEvaluateBothPublicPrivateTestcase()) || "N".equals(tc.getIsPublic()))
                                                  .mapToLong(TestCaseEntity::getTestCasePoint)
                                                  .sum();

            result.put(problemId, totalPoint);
        }
        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ModelProblemGeneralInfo> getAllProblemsGeneralInfo() {
        //return problemRepo.getAllProblemGeneralInformation();
        return problemRepo.getAllOpenProblemGeneralInformation();

    }

    @Transactional(readOnly = true)
    @Override
    public ModelCreateContestProblemResponse getContestProblem(String problemId) throws Exception {
        ProblemEntity problemEntity;
        ModelCreateContestProblemResponse problemResponse = new ModelCreateContestProblemResponse();
        try {
            problemEntity = problemRepo.findByProblemId(problemId);
            if (problemEntity == null) {
                throw new MiniLeetCodeException("Problem not found");
            }
            problemResponse.setProblemId(problemEntity.getProblemId());
            problemResponse.setProblemName(problemEntity.getProblemName());
            problemResponse.setProblemDescription(problemEntity.getProblemDescription());
            problemResponse.setUserId(problemEntity.getCreatedBy());
//            problemResponse.setTimeLimit(problemEntity.getTimeLimit());
            problemResponse.setTimeLimitCPP(problemEntity.getTimeLimitCPP());
            problemResponse.setTimeLimitJAVA(problemEntity.getTimeLimitJAVA());
            problemResponse.setTimeLimitPYTHON(problemEntity.getTimeLimitPYTHON());
            problemResponse.setMemoryLimit(problemEntity.getMemoryLimit());
            problemResponse.setLevelId(problemEntity.getLevelId());
            problemResponse.setCorrectSolutionSourceCode(problemEntity.getCorrectSolutionSourceCode());
            problemResponse.setCorrectSolutionLanguage(problemEntity.getCorrectSolutionLanguage());
            problemResponse.setSolutionCheckerSourceCode(problemEntity.getSolutionCheckerSourceCode());
            problemResponse.setSolutionCheckerSourceLanguage(problemEntity.getSolutionCheckerSourceLanguage());
            problemResponse.setScoreEvaluationType(problemEntity.getScoreEvaluationType());
            problemResponse.setSolution(problemEntity.getSolution());
            problemResponse.setIsPreloadCode(problemEntity.getIsPreloadCode());
            problemResponse.setPreloadCode(problemEntity.getPreloadCode());
            problemResponse.setLevelOrder(problemEntity.getLevelOrder());
            problemResponse.setCreatedAt(problemEntity.getCreatedAt());
            problemResponse.setPublicProblem(problemEntity.isPublicProblem());
            problemResponse.setTags(problemEntity.getTags());
            problemResponse.setSampleTestCase(problemEntity.getSampleTestcase());
            if (problemEntity.getAttachment() != null) {
                String[] fileId = problemEntity.getAttachment().split(";", -1);
                if (fileId.length != 0) {
                    List<byte[]> fileArray = new ArrayList<>();
                    List<String> fileNames = new ArrayList<>();
                    for (String s : fileId) {
                        try {
                            GridFsResource content = mongoContentService.getById(s);
                            if (content != null) {
                                InputStream inputStream = content.getInputStream();
                                fileArray.add(IOUtils.toByteArray(inputStream));
                                fileNames.add(content.getFilename());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    problemResponse.setAttachment(fileArray);
                    problemResponse.setAttachmentNames(fileNames);
                } else {
                    problemResponse.setAttachment(null);
                    problemResponse.setAttachmentNames(null);
                }
            } else {
                problemResponse.setAttachment(null);
                problemResponse.setAttachmentNames(null);
            }

            return problemResponse;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public void exportProblem(String problemId, OutputStream outputStream) {
        try {
            ModelCreateContestProblemResponse problem = getContestProblem(problemId);

            if (problem != null) {
                handleExportProblem(problem, outputStream);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleExportProblem(
            ModelCreateContestProblemResponse problem,
            OutputStream outputStream
    ) throws IOException {
        Path exportDir = Files.createTempDirectory("problem-export-");
        List<File> files = new ArrayList<>();

        try {
            File problemGeneralInfoFile = exporter.exportProblemInfoToFile(problem, exportDir);
            File problemDescriptionFile = exporter.exportProblemDescriptionToFile(problem, exportDir);
            File exportProblemInfoAsTextToFile = exporter.exportProblemInfoAsTextToFile(problem, exportDir);
            File problemCorrectSolutionFile = exporter.exportProblemCorrectSolutionToFile(problem, exportDir);

            files.add(problemGeneralInfoFile);
            files.add(problemDescriptionFile);
            files.add(exportProblemInfoAsTextToFile);
            files.add(problemCorrectSolutionFile);

            if (Constants.ProblemResultEvaluationType.CUSTOM.getValue().equals(problem.getScoreEvaluationType())) {
                File problemCustomCheckerFile = exporter.exportProblemCustomCheckerToFile(problem, exportDir);
                files.add(problemCustomCheckerFile);
            }

            if (!problem.getAttachmentNames().isEmpty()) {
                files.addAll(exporter.exportProblemAttachmentToFile(problem, exportDir));
            }

            files.addAll(exporter.exportProblemTestCasesToFile(problem, exportDir));

            ZipOutputStreamUtils.zip(
                    outputStream,
                    files,
                    CompressionMethod.DEFLATE,
                    null,
                    EncryptionMethod.AES,
                    AesKeyStrength.KEY_STRENGTH_256
            );

        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            for (File file : files) {
                if (file != null && file.exists()) {
                    if (!file.delete()) {
                        System.err.println("Can't delete file: " + file.getAbsolutePath());
                    }
                }
            }

            try {
                Files.walk(exportDir)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(file -> {
                            if (!file.delete()) {
                                log.error("Failed to delete: " + file.getAbsolutePath());
                            }
                        });
            } catch (IOException e) {
                log.error("Failed to clean up export temp directory: " + exportDir);
            }
        }
    }

    @Transactional(readOnly = true)
    public void exportProblemJson(String problemId, OutputStream outputStream, String userId) {
        try {
            ModelCreateContestProblemResponse problem = getContestProblem(problemId);

            List<String> roles = userContestProblemRoleRepo.getRolesByProblemIdAndUserId(problem.getProblemId(), userId);
            boolean hasPermission = roles.stream().anyMatch(role ->
                    role.equals(UserContestProblemRole.ROLE_OWNER) ||
                            role.equals(UserContestProblemRole.ROLE_EDITOR) ||
                            role.equals(UserContestProblemRole.ROLE_VIEWER)
            );

            if (!hasPermission) {
                throw new AccessDeniedException("You do not have permission to export this problem.");
            }
            if (problem != null) {
                handleExportProblemJson(problem, outputStream, userId);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleExportProblemJson(
            ModelCreateContestProblemResponse problem,
            OutputStream outputStream,
            String userId
    ) throws IOException {

        long maxSizeBytes = iEProblemProperties.getExportConf().getMaxSizeBytes();
        long totalSize = 0;

        Map<String, ByteArrayOutputStream> fileStreams = new HashMap<>();

        try {
            ByteArrayOutputStream problemJsonStream = exporter.exportProblemToJsonStream(problem);
            fileStreams.put("ProblemData.json", problemJsonStream);
            totalSize += problemJsonStream.size();

            if (!problem.getAttachmentNames().isEmpty()) {
                List<Map.Entry<String, ByteArrayOutputStream>> attachmentStreams =
                        exporter.exportProblemAttachmentToStream(problem);
                for (Map.Entry<String, ByteArrayOutputStream> entry : attachmentStreams) {
                    fileStreams.put(entry.getKey(), entry.getValue());
                    totalSize += entry.getValue().size();
                }
            }

            if (totalSize > maxSizeBytes) {
                UserLogin admin = userLoginRepo.findByUserLoginId("admin");

                notificationsService.create(
                        userId,
                        admin.getUserLoginId(),
                        userId + " exported problem with ID " + problem.getProblemId() +
                                " has size " + String.format("%.2f", totalSize / (1024.0 * 1024.0)) + "MB, exceeds threshold " + "50 " + "MB",
                        ""
                );
            }

            try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                for (Map.Entry<String, ByteArrayOutputStream> entry : fileStreams.entrySet()) {
                    ZipEntry zipEntry = new ZipEntry(entry.getKey());
                    zipOut.putNextEntry(zipEntry);
                    entry.getValue().writeTo(zipOut);
                    zipOut.closeEntry();
                }
            }

        } finally {
            for (ByteArrayOutputStream stream : fileStreams.values()) {
                stream.close();
            }
        }
    }


    @Transactional
    public void importProblem(ModelImportProblem model, MultipartFile zipFile, String userId) {
        final long MAX_ZIP_SIZE = iEProblemProperties.getImportConf().getMaxSizeBytes();
        final int MAX_FILE_COUNT = iEProblemProperties.getImportConf().getFileCount();
        final long MAX_TOTAL_UNZIPPED_SIZE = iEProblemProperties.getImportConf().getMaxSizeUnzip();

        Map<String, byte[]> extractedFiles = new HashMap<>();
        long totalUnzippedSize = 0;

        try {
            if (problemRepo.existsByProblemId(model.getProblemId()) || problemRepo.existsByProblemName(model.getProblemName())) {
                throw new IllegalArgumentException("Problem ID or name already exists");
            }
            if (zipFile.getSize() > MAX_ZIP_SIZE) {
                throw new IllegalArgumentException("ZIP file size exceeds "+  MAX_ZIP_SIZE + "MB limit");
            }
            try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (extractedFiles.size() >= MAX_FILE_COUNT) {
                        throw new IllegalArgumentException("Too many files in ZIP (max: " + MAX_FILE_COUNT + ")");
                    }

                    if (entry.isDirectory()) {
                        continue;
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    long fileSize = 0;

                    while ((bytesRead = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                        fileSize += bytesRead;
                        totalUnzippedSize += bytesRead;

                        if (totalUnzippedSize > MAX_TOTAL_UNZIPPED_SIZE) {
                            throw new IllegalArgumentException("Total unzipped size exceeds 100MB limit");
                        }
                    }

                    extractedFiles.put(entry.getName(), baos.toByteArray());
                    zis.closeEntry();
                }
            }

            byte[] jsonData = extractedFiles.get("ProblemData.json");
            if (jsonData == null) {
                throw new IllegalArgumentException("ProblemData.json not found in ZIP file");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> problemData = objectMapper.readValue(jsonData, Map.class);

            ProblemEntity problemEntity = new ProblemEntity();
            problemEntity.setProblemId(model.getProblemId());
            problemEntity.setProblemName(model.getProblemName());

            if (problemData.containsKey("isPublic")) {
                problemEntity.setPublicProblem((Boolean) problemData.get("isPublic"));
            }
            if (problemData.containsKey("timeLimitCPP")) {
                problemEntity.setTimeLimitCPP(((Number) problemData.get("timeLimitCPP")).floatValue());
            }
            if (problemData.containsKey("timeLimitJAVA")) {
                problemEntity.setTimeLimitJAVA(((Number) problemData.get("timeLimitJAVA")).floatValue());
            }
            if (problemData.containsKey("timeLimitPYTHON")) {
                problemEntity.setTimeLimitPYTHON(((Number) problemData.get("timeLimitPYTHON")).floatValue());
            }
            if (problemData.containsKey("memoryLimit")) {
                problemEntity.setMemoryLimit(((Number) problemData.get("memoryLimit")).floatValue());
            }
            if (problemData.containsKey("levelId")) {
                problemEntity.setLevelId((String) problemData.get("levelId"));
            }
            if (problemData.containsKey("levelOrder")) {
                problemEntity.setLevelOrder(((Number) problemData.get("levelOrder")).intValue());
            }
            if (problemData.containsKey("status")) {
                problemEntity.setStatusId((String) problemData.get("status"));
            }
            if (problemData.containsKey("problemDescription")) {
                problemEntity.setProblemDescription((String) problemData.get("problemDescription"));
            }
            if (problemData.containsKey("correctSolutionLanguage")) {
                problemEntity.setCorrectSolutionLanguage((String) problemData.get("correctSolutionLanguage"));
            }
            if (problemData.containsKey("correctSolutionSourceCode")) {
                problemEntity.setCorrectSolutionSourceCode((String) problemData.get("correctSolutionSourceCode"));
            }
            if (problemData.containsKey("scoreEvaluationType")) {
                problemEntity.setScoreEvaluationType((String) problemData.get("scoreEvaluationType"));
            }
            if (problemData.containsKey("solution")) {
                problemEntity.setSolution((String) problemData.get("solution"));
            }
            if (problemData.containsKey("isPreloadCode")) {
                problemEntity.setIsPreloadCode((Boolean) problemData.get("isPreloadCode"));
            }
            if (problemData.containsKey("preloadCode")) {
                problemEntity.setPreloadCode((String) problemData.get("preloadCode"));
            }
            if (problemData.containsKey("sampleTestCase")) {
                problemEntity.setSampleTestcase((String) problemData.get("sampleTestCase"));
            }

            if (problemData.containsKey("tags")) {
                List<String> tagNames = (List<String>) problemData.get("tags");
                List<TagEntity> existingTags = tagRepo.findByNameInIgnoreCase(tagNames);
                Map<String, TagEntity> nameToTagMap = new HashMap<>();
                for (TagEntity tag : existingTags) {
                    nameToTagMap.put(tag.getName().toLowerCase(), tag);
                }

                List<TagEntity> finalTags = new ArrayList<>();
                List<TagEntity> newTags = new ArrayList<>();

                for (String name : tagNames) {
                    String lower = name.toLowerCase();
                    if (nameToTagMap.containsKey(lower)) {
                        finalTags.add(nameToTagMap.get(lower));
                    } else {
                        TagEntity newTag = new TagEntity();
                        newTag.setName(name);
                        newTags.add(newTag);
                    }
                }

                if (!newTags.isEmpty()) {
                    List<TagEntity> saved = tagRepo.saveAll(newTags);
                    finalTags.addAll(saved);
                }

                problemEntity.setTags(finalTags);
            }

            if ("CUSTOM_EVALUATION".equals(problemData.get("scoreEvaluationType"))) {
                if (problemData.containsKey("solutionCheckerSourceLanguage")) {
                    problemEntity.setSolutionCheckerSourceLanguage((String) problemData.get("solutionCheckerSourceLanguage"));
                }
                if (problemData.containsKey("solutionCheckerSourceCode")) {
                    problemEntity.setSolutionCheckerSourceCode((String) problemData.get("solutionCheckerSourceCode"));
                }
            }

            List<String> attachmentId = new ArrayList<>();
            for (Map.Entry<String, byte[]> entry : extractedFiles.entrySet()) {
                if (entry.getKey().equals("ProblemData.json")) {
                    continue;
                }

                String fileName = entry.getKey();
                byte[] fileContent = entry.getValue();

                MultipartFile customMultipartFile = new MultipartFile() {
                    @Override
                    public String getName() {
                        return fileName;
                    }

                    @Override
                    public String getOriginalFilename() {
                        return fileName;
                    }

                    @Override
                    public String getContentType() {
                        return "application/octet-stream";
                    }

                    @Override
                    public boolean isEmpty() {
                        return fileContent.length == 0;
                    }

                    @Override
                    public long getSize() {
                        return fileContent.length;
                    }

                    @Override
                    public byte[] getBytes() {
                        return fileContent;
                    }

                    @Override
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream(fileContent);
                    }

                    @Override
                    public void transferTo(File dest) throws IOException {
                        Files.write(dest.toPath(), fileContent);
                    }
                };

                ContentModel contentModel = new ContentModel(null, customMultipartFile);
                ObjectId id = null;
                try {
                    id = mongoContentService.storeFileToGridFs(contentModel);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to store file: " + fileName, e);
                }

                if (id != null) {
                    ContentHeaderModel rs = new ContentHeaderModel(id.toHexString());
                    attachmentId.add(rs.getId());
                }
            }

            problemEntity.setAttachment(String.join(";", attachmentId));
            problemEntity = problemRepo.save(problemEntity);

            List<String> roles = Arrays.asList(
                UserContestProblemRole.ROLE_OWNER,
                UserContestProblemRole.ROLE_EDITOR,
                UserContestProblemRole.ROLE_VIEWER
            );

            List<UserContestProblemRole> rolesToSave = new ArrayList<>();
            for (String role : roles) {
                UserContestProblemRole upr = new UserContestProblemRole();
                upr.setProblemId(problemEntity.getProblemId());
                upr.setUserId(userId);
                upr.setRoleId(role);
                rolesToSave.add(upr);
            }

            UserLogin admin = userLoginRepo.findByUserLoginId("admin");
            if (userId != null && !userId.equals("admin")) {
                for (String role : roles) {
                    UserContestProblemRole upr = new UserContestProblemRole();
                    upr.setProblemId(problemEntity.getProblemId());
                    upr.setUserId(admin.getUserLoginId());
                    upr.setRoleId(role);
                    rolesToSave.add(upr);
                }
            }

            userContestProblemRoleRepo.saveAll(rolesToSave);

            if (problemData.containsKey("testCases")) {
                List<Map<String, Object>> testCases = (List<Map<String, Object>>) problemData.get("testCases");
                for (Map<String, Object> tc : testCases) {
                    TestCaseEntity testCase = new TestCaseEntity();
                    testCase.setProblemId(model.getProblemId());
                    if (tc.containsKey("testCasePoint")) {
                        testCase.setTestCasePoint(((Number) tc.get("testCasePoint")).intValue());
                    }
                    if (tc.containsKey("isPublic")) {
                        testCase.setIsPublic((String) tc.get("isPublic"));
                    }
                    if (tc.containsKey("statusId")) {
                        testCase.setStatusId((String) tc.get("statusId"));
                    }
                    if (tc.containsKey("description")) {
                        testCase.setDescription((String) tc.get("description"));
                    }
                    if (tc.containsKey("testCase")) {
                        testCase.setTestCase((String) tc.get("testCase"));
                    }
                    if (tc.containsKey("correctAnswer")) {
                        testCase.setCorrectAnswer((String) tc.get("correctAnswer"));
                    }
                    testCaseRepo.save(testCase);
                }
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to import problem: " + e.getMessage(), e);
        } finally {
            extractedFiles.clear();
        }
    }

    @Transactional(readOnly = true)
    @Override
    public ModelCreateContestProblemResponse getContestProblemDetailByIdAndTeacher(String problemId, String teacherId)
        throws Exception {

//        TODO: re-open this later
//        boolean hasPermission =
//                this.userContestProblemRoleRepo.existsByProblemIdAndUserIdAndRoleId(problemId,
//                        teacherId, UserContestProblemRole.ROLE_OWNER)
//                        || this.userContestProblemRoleRepo.existsByProblemIdAndUserIdAndRoleId(problemId,
//                                teacherId, UserContestProblemRole.ROLE_EDITOR)
//                        || this.userContestProblemRoleRepo.existsByProblemIdAndUserIdAndRoleId(problemId,
//                                teacherId, UserContestProblemRole.ROLE_VIEWER);
//        if (!hasPermission) {
//            throw new MiniLeetCodeException("You don't have permission to view this problem", 403);
//        }

        ProblemEntity problemEntity = problemRepo.findByProblemId(problemId);
        if (problemEntity == null) {
            throw new MiniLeetCodeException("Problem not found", 404);
        }

        if (problemEntity.isPublicProblem() != true) {

            List<UserContestProblemRole> ucpr = userContestProblemRoleRepo
                .findAllByProblemIdAndUserId(problemEntity.getProblemId(), teacherId);

            boolean ok = true;
            if (!problemEntity.getCreatedBy().equals(teacherId)) {
                if (ucpr == null || ucpr.size() == 0) {
                    ok = false;
                } else {
                    boolean owner = false;
                    for (UserContestProblemRole e : ucpr) {
                        if (e.getRoleId().equals(UserContestProblemRole.ROLE_OWNER)) {
                            owner = true;
                            break;
                        }
                    }
                    if (!owner && problemEntity.getStatusId() != null &&
                        !problemEntity.getStatusId().equals(ProblemEntity.PROBLEM_STATUS_OPEN)) {
                        ok = false;
                    }
                }
            }

            if (!ok) {
                throw new MiniLeetCodeException("Problem is not open or you do not have permission", 400);
            }
        }
        /*
        if (!problemEntity.getUserId().equals(teacherId) &&
            !problemEntity.getStatusId().equals(ProblemEntity.PROBLEM_STATUS_OPEN)) {
            throw new MiniLeetCodeException("Problem is not open", 400);
        }
        */

        ModelCreateContestProblemResponse problemResponse = new ModelCreateContestProblemResponse();
        problemResponse.setProblemId(problemEntity.getProblemId());
        problemResponse.setProblemName(problemEntity.getProblemName());
        problemResponse.setProblemDescription(problemEntity.getProblemDescription());
        problemResponse.setUserId(problemEntity.getCreatedBy());
//        problemResponse.setTimeLimit(problemEntity.getTimeLimit());
        problemResponse.setTimeLimitCPP(problemEntity.getTimeLimitCPP());
        problemResponse.setTimeLimitJAVA(problemEntity.getTimeLimitJAVA());
        problemResponse.setTimeLimitPYTHON(problemEntity.getTimeLimitPYTHON());
        problemResponse.setMemoryLimit(problemEntity.getMemoryLimit());
        problemResponse.setLevelId(problemEntity.getLevelId());
        problemResponse.setCorrectSolutionSourceCode(problemEntity.getCorrectSolutionSourceCode());
        problemResponse.setCorrectSolutionLanguage(problemEntity.getCorrectSolutionLanguage());
        problemResponse.setSolutionCheckerSourceCode(problemEntity.getSolutionCheckerSourceCode());
        problemResponse.setSolutionCheckerSourceLanguage(problemEntity.getSolutionCheckerSourceLanguage());
        problemResponse.setScoreEvaluationType(problemEntity.getScoreEvaluationType());
        problemResponse.setSolution(problemEntity.getSolution());
        problemResponse.setIsPreloadCode(problemEntity.getIsPreloadCode());
        problemResponse.setPreloadCode(problemEntity.getPreloadCode());
        problemResponse.setLevelOrder(problemEntity.getLevelOrder());
        problemResponse.setCreatedAt(problemEntity.getCreatedAt());
        problemResponse.setPublicProblem(problemEntity.isPublicProblem());
        problemResponse.setTags(problemEntity.getTags());
        problemResponse.setStatus(problemEntity.getStatusId());
        problemResponse.setSampleTestCase(problemEntity.getSampleTestcase());

        if (problemEntity.getAttachment() != null) {
            String[] fileId = problemEntity.getAttachment().split(";", -1);
            if (fileId.length != 0) {
                List<byte[]> fileArray = new ArrayList<>();
                List<String> fileNames = new ArrayList<>();
                for (String s : fileId) {
                    try {
                        GridFsResource content = mongoContentService.getById(s);
                        if (content != null) {
                            InputStream inputStream = content.getInputStream();
                            fileArray.add(IOUtils.toByteArray(inputStream));
                            fileNames.add(content.getFilename());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                problemResponse.setAttachment(fileArray);
                problemResponse.setAttachmentNames(fileNames);
            } else {
                problemResponse.setAttachment(null);
                problemResponse.setAttachmentNames(null);
            }
        } else {
            problemResponse.setAttachment(null);
            problemResponse.setAttachmentNames(null);
        }

        problemResponse.setRoles(userContestProblemRoleRepo.getRolesByProblemIdAndUserId(problemId, teacherId));

        return problemResponse;
    }

    @Transactional
    @Override
    public List<ModelImportProblemFromContestResponse> importProblemsFromAContest(ModelImportProblemsFromAContestInput I) {
        ContestEntity contest = contestRepo.findContestByContestId(I.getFromContestId());
        if (contest == null) {
            throw new IllegalArgumentException("Contest ID " + I.getFromContestId() + " not found");
        }

        List<ModelImportProblemFromContestResponse> responseList = new ArrayList<>();

        for (ProblemEntity p : contest.getProblems()) {
            ModelImportProblemFromContestResponse response = new ModelImportProblemFromContestResponse();
            response.setProblemId(p.getProblemId());
            ContestProblem ocp = contestProblemRepo.findByContestIdAndProblemId(I.getFromContestId(), p.getProblemId());
            ContestProblem cp = contestProblemRepo.findByContestIdAndProblemId(I.getContestId(), p.getProblemId());
            if (cp != null) {
                response.setStatus("Problem already existed");
                responseList.add(response);
                continue;
            }
            cp = new ContestProblem();
            cp.setContestId(I.getContestId());
            cp.setProblemId(p.getProblemId());
            cp.setSubmissionMode(ocp.getSubmissionMode());
            cp.setProblemRename(ocp.getProblemRename());
            cp.setProblemRecode(ocp.getProblemRecode());
            contestProblemRepo.save(cp);
            response.setStatus("SUCCESSFUL");
            responseList.add(response);
        }

        return responseList;
    }

    /**
     * @param userId
     * @param filter
     * @param isPublic
     * @return
     */
    @Transactional(readOnly = true)
    public Page<ProblemDTO> getProblems(String userId, ProblemFilter filter, Boolean isPublic) {
        return fetchProblems(userId, filter, isPublic, false);
    }

    /**
     * @param userId
     * @param filter
     * @return
     */
    @Transactional(readOnly = true)
    public Page<ProblemDTO> getSharedProblems(String userId, ProblemFilter filter) {
        return fetchProblems(userId, filter, null, true);
    }

    /**
     * @param userId
     * @param filter
     * @return
     */
    public Page<ProblemDTO> getPublicProblems(String userId, ProblemFilter filter) {
        return this.getProblems(null, filter, true);
    }

    private Page<ProblemDTO> fetchProblems(
        String userId,
        ProblemFilter filter,
        Boolean isPublic,
        boolean isSharedProblems
    ) {
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize());

        String name = StringUtils.isNotBlank(filter.getName()) ? filter.getName().trim() : null;

        normalizeFilter(filter);

        Page<ProblemProjection> problems = isSharedProblems
            ? problemRepo.findAllSharedProblemsBy(
            userId,
            name,
            filter.getLevelIds(),
            filter.getTagIds(),
            filter.getStatusIds(),
            pageable)
            : problemRepo.findAllBy(
                userId,
                name,
                filter.getLevelIds(),
                filter.getTagIds(),
                filter.getStatusIds(),
                isPublic,
                pageable);

        return problems.map(this::convertToProblemDTO);
    }

    private void normalizeFilter(ProblemFilter filter) {
        if (StringUtils.isBlank(filter.getLevelIds())) {
            filter.setLevelIds(null);
        }
        if (StringUtils.isBlank(filter.getStatusIds())) {
            filter.setStatusIds(null);
        }
        if (StringUtils.isBlank(filter.getTagIds())) {
            filter.setTagIds(null);
        }
    }

    private ProblemDTO convertToProblemDTO(ProblemProjection item) {
        ProblemDTO dto = objectMapper.convertValue(item, ProblemDTO.class);
        try {
            dto.setTags(objectMapper.readValue(
                item.getJsonTags(), new TypeReference<List<TagEntity>>() {
                }));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dto;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ProblemEntity> getAllProblems(String userId) {
        List<ProblemEntity> problems = problemRepo.findAll();
        return problems;
    }

    @Transactional(readOnly = true)
    @Override
    public List<SubmissionModelResponse> extApiGetSubmissions(String participantId) {
        List<ContestSubmissionEntity> sub = contestSubmissionPagingAndSortingRepo.findAllByUserId(participantId);
        List<SubmissionModelResponse> res = new ArrayList<SubmissionModelResponse>();
        for (ContestSubmissionEntity s : sub) {
            SubmissionModelResponse r = new SubmissionModelResponse(
                s.getUserId(),
                s.getProblemId(),
                s.getContestId(),
                s.getPoint(),
                s.getCreatedAt());
            res.add(r);
        }
        return res;
    }

    @Transactional
    @Override
    public ProblemEntity cloneProblem(String userId, ModelCloneProblem cloneRequest) throws MiniLeetCodeException {

        ProblemEntity originalProblem = problemRepo.findById(cloneRequest.getOldProblemId())
                                                   .orElseThrow(() -> new MiniLeetCodeException(
                                                       "Original problem not found",
                                                       HttpStatus.NOT_FOUND.value()));

        if (problemRepo.existsByProblemId(cloneRequest.getNewProblemId())) {
            throw new MiniLeetCodeException("New problem ID already exists", HttpStatus.CONFLICT.value());
        }

        if (problemRepo.existsByProblemName(cloneRequest.getNewProblemName())) {
            throw new MiniLeetCodeException("New problem name already exists", HttpStatus.CONFLICT.value());
        }

        ProblemEntity newProblem = new ProblemEntity();

        newProblem.setProblemId(cloneRequest.getNewProblemId());
        newProblem.setProblemName(cloneRequest.getNewProblemName());
        newProblem.setProblemDescription(originalProblem.getProblemDescription());
        newProblem.setTimeLimitCPP(originalProblem.getTimeLimitCPP());
        newProblem.setTimeLimitJAVA(originalProblem.getTimeLimitJAVA());
        newProblem.setTimeLimitPYTHON(originalProblem.getTimeLimitPYTHON());
        newProblem.setMemoryLimit(originalProblem.getMemoryLimit());
        newProblem.setCorrectSolutionSourceCode(originalProblem.getCorrectSolutionSourceCode());
        newProblem.setCorrectSolutionLanguage(originalProblem.getCorrectSolutionLanguage());
        newProblem.setPublicProblem(originalProblem.isPublicProblem());
        newProblem.setTags(originalProblem.getTags());
        //newProblem.setCreatedBy(originalProblem.getUserId());
        newProblem.setCreatedBy(userId);
//        newProblem.setTimeLimit(originalProblem.getTimeLimit());
        newProblem.setLevelId(originalProblem.getLevelId());
        newProblem.setCategoryId(originalProblem.getCategoryId());
        newProblem.setSolutionCheckerSourceCode(originalProblem.getSolutionCheckerSourceCode());
        newProblem.setSolutionCheckerSourceLanguage(originalProblem.getSolutionCheckerSourceLanguage());
        newProblem.setSolution(originalProblem.getSolution());
        newProblem.setLevelOrder(originalProblem.getLevelOrder());
        newProblem.setAttachment(originalProblem.getAttachment());
        newProblem.setScoreEvaluationType(originalProblem.getScoreEvaluationType());
        newProblem.setPreloadCode(originalProblem.getPreloadCode());
        newProblem.setIsPreloadCode(originalProblem.getIsPreloadCode());
        newProblem.setStatusId(originalProblem.getStatusId());
        newProblem.setSampleTestcase(originalProblem.getSampleTestcase());

        newProblem = problemRepo.save(newProblem);

        List<TestCaseEntity> originalTestCases = testCaseRepo.findAllByProblemId(cloneRequest.getOldProblemId());
        for (TestCaseEntity originalTestCase : originalTestCases) {
            TestCaseEntity newTestCase = new TestCaseEntity();
            newTestCase.setTestCasePoint(originalTestCase.getTestCasePoint());
            newTestCase.setTestCase(originalTestCase.getTestCase());
            newTestCase.setCorrectAnswer(originalTestCase.getCorrectAnswer());
            newTestCase.setProblemId(newProblem.getProblemId());
            newTestCase.setIsPublic(originalTestCase.getIsPublic());
            newTestCase.setDescription(originalTestCase.getDescription());
            newTestCase.setStatusId(originalTestCase.getStatusId());

            testCaseRepo.save(newTestCase);
        }
        // grant role owner, manager, view to current user
        UserContestProblemRole upr = new UserContestProblemRole();
        upr.setProblemId(newProblem.getProblemId());
        upr.setUserId(userId);
        upr.setRoleId(UserContestProblemRole.ROLE_OWNER);
        upr.setUpdateByUserId(userId);
        upr.setCreatedStamp(new Date());
        upr.setLastUpdated(new Date());
        upr = userContestProblemRoleRepo.save(upr);

        upr = new UserContestProblemRole();
        upr.setProblemId(newProblem.getProblemId());
        upr.setUserId(userId);
        upr.setRoleId(UserContestProblemRole.ROLE_EDITOR);
        upr.setUpdateByUserId(userId);
        upr.setCreatedStamp(new Date());
        upr.setLastUpdated(new Date());
        upr = userContestProblemRoleRepo.save(upr);

        upr = new UserContestProblemRole();
        upr.setProblemId(newProblem.getProblemId());
        upr.setUserId(userId);
        upr.setRoleId(UserContestProblemRole.ROLE_VIEWER);
        upr.setUpdateByUserId(userId);
        upr.setCreatedStamp(new Date());
        upr.setLastUpdated(new Date());
        upr = userContestProblemRoleRepo.save(upr);


        // grant manager role to user admin
        UserLogin admin = userLoginRepo.findByUserLoginId("admin");
        if (admin != null) {
            upr = new UserContestProblemRole();
            upr.setProblemId(newProblem.getProblemId());
            upr.setUserId(admin.getUserLoginId());
            upr.setRoleId(UserContestProblemRole.ROLE_OWNER);
            upr.setUpdateByUserId(userId);
            upr.setCreatedStamp(new Date());
            upr.setLastUpdated(new Date());
            upr = userContestProblemRoleRepo.save(upr);

            upr = new UserContestProblemRole();
            upr.setProblemId(newProblem.getProblemId());
            upr.setUserId(admin.getUserLoginId());
            upr.setRoleId(UserContestProblemRole.ROLE_EDITOR);
            upr.setUpdateByUserId(userId);
            upr.setCreatedStamp(new Date());
            upr.setLastUpdated(new Date());
            upr = userContestProblemRoleRepo.save(upr);

            upr = new UserContestProblemRole();
            upr.setProblemId(newProblem.getProblemId());
            upr.setUserId(admin.getUserLoginId());
            upr.setRoleId(UserContestProblemRole.ROLE_VIEWER);
            upr.setUpdateByUserId(userId);
            upr.setCreatedStamp(new Date());
            upr.setLastUpdated(new Date());
            upr = userContestProblemRoleRepo.save(upr);

            // push notification to admin
            notificationsService.create(
                userId, admin.getUserLoginId(),
                userId + " has cloned a contest problem ID " +
                newProblem.getProblemId()
                , "");
        }
        return newProblem;
    }

    @Transactional(readOnly = true)
    @Override
    public List<ModelResponseUserProblemRole> getUserProblemRoles(String problemId) {
        return userContestProblemRoleRepo.findAllByProblemIdWithFullName(problemId);
    }

    @Transactional
    @Override
    public Map<String, Object> addUserProblemRole(String userName, ModelUserProblemRoleInput input) throws Exception {
        boolean isOwner = this.userContestProblemRoleRepo.existsByProblemIdAndUserIdAndRoleId(
            input.getProblemId(),
            userName,
            UserContestProblemRole.ROLE_OWNER);
        if (!isOwner) {
            throw new MiniLeetCodeException("You are not owner of this problem.", 403);
        }

        List<String> userIds = input.getUserIds() != null ? input.getUserIds() : new ArrayList<>();
        List<String> groupUserIds = new ArrayList<>();
        if (input.getGroupIds() != null && !input.getGroupIds().isEmpty()) {
            groupUserIds = teacherGroupRelationRepository.findUserIdsByGroupIds(input.getGroupIds());
        }

        Set<String> allUserIds = new HashSet<>();
        allUserIds.addAll(userIds);
        allUserIds.addAll(groupUserIds);

        boolean success = true;
        List<String> addedUsers = new ArrayList<>();
        List<String> skippedUsers = new ArrayList<>();
        for (String userId : allUserIds) {
            List<UserContestProblemRole> L = userContestProblemRoleRepo.findAllByProblemIdAndUserIdAndRoleId(
                input.getProblemId(),
                userId,
                input.getRoleId());
            if (L != null && L.size() > 0) {
                success = false;
                skippedUsers.add(userId);
                continue;
            }
            UserContestProblemRole e = new UserContestProblemRole();
            e.setUserId(userId);
            e.setProblemId(input.getProblemId());
            e.setRoleId(input.getRoleId());
            e.setUpdateByUserId(userName);
            userContestProblemRoleRepo.save(e);
            addedUsers.add(userId);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("addedUsers", addedUsers);
        response.put("skippedUsers", skippedUsers);
        return response;
    }

    @Transactional
    @Override
    public boolean removeUserProblemRole(String userName, ModelUserProblemRole input) throws Exception {
        boolean isOwner = this.userContestProblemRoleRepo.existsByProblemIdAndUserIdAndRoleId(
            input.getProblemId(),
            userName,
            UserContestProblemRole.ROLE_OWNER);
        if (!isOwner) {
            throw new MiniLeetCodeException("You are not owner of this problem.", 403);
        }
        List<UserContestProblemRole> L = userContestProblemRoleRepo.findAllByProblemIdAndUserIdAndRoleId(
            input.getProblemId(),
            input.getUserId(),
            input.getRoleId());
        //if (L != null && L.size() > 0) {
        if (L == null || L.size() == 0) {
            return false;
        }
        log.info("removeUserProblemRole(" + input.getUserId() + "," +
                 input.getProblemId() + "," + input.getRoleId() + ", got L.sz = " + L.size());
        for (UserContestProblemRole e : L) {
            userContestProblemRoleRepo.delete(e);
            //userContestProblemRoleRepo.remove(e);
        }
        return true;
    }


}
