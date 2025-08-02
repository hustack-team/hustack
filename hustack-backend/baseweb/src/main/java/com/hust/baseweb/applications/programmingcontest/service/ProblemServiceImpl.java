package com.hust.baseweb.applications.programmingcontest.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.baseweb.applications.contentmanager.model.ContentModel;
import com.hust.baseweb.applications.contentmanager.repo.MongoContentService;
import com.hust.baseweb.applications.education.classmanagement.utils.ZipOutputStreamUtils;
import com.hust.baseweb.applications.notifications.service.NotificationsService;
import com.hust.baseweb.applications.programmingcontest.constants.Constants;
import com.hust.baseweb.applications.programmingcontest.entity.*;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.model.externalapi.SubmissionModelResponse;
import com.hust.baseweb.applications.programmingcontest.repo.*;
import com.hust.baseweb.applications.programmingcontest.utils.ContestProblemPermissionUtil;
import com.hust.baseweb.applications.programmingcontest.utils.ProblemPermissionUtil;
import com.hust.baseweb.model.ProblemFilter;
import com.hust.baseweb.model.ProblemProjection;
import com.hust.baseweb.model.TestCaseFilter;
import com.hust.baseweb.model.dto.ProblemDTO;
import com.hust.baseweb.service.UserService;
import com.hust.baseweb.utils.CommonUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.hust.baseweb.applications.programmingcontest.entity.UserRegistrationContestEntity.ROLE_MANAGER;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProblemServiceImpl implements ProblemService {

    ProblemRepo problemRepo;

    TeacherGroupRelationRepository teacherGroupRelationRepository;

    TestCaseRepo testCaseRepo;

    ContestRepo contestRepo;

    ContestSubmissionRepo contestSubmissionRepo;

    NotificationsService notificationsService;

    UserService userService;

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

    ProblemBlockRepo problemBlockRepo;

    ContestProblemPermissionUtil contestProblemPermissionUtil;

    ProblemPermissionUtil problemPermissionUtil;

    @Override
    @Transactional
    public ProblemEntity createProblem(
        String createdBy,
        CreateProblemDTO dto,
        MultipartFile[] files
    ) {
        String problemId = dto.getProblemId().trim();
        if (problemRepo.findByProblemId(problemId) != null) {
            throw new DuplicateKeyException("Problem ID already exist");
        }

        List<String> attachmentId = new ArrayList<>();
        List<MultipartFile> fileArray = Optional.ofNullable(files)
                                                .map(Arrays::asList)
                                                .orElseGet(Collections::emptyList);

        for (int i = 0; i < fileArray.size(); i++) {
            MultipartFile file = fileArray.get(i);
            ContentModel model = new ContentModel(null, file);

            ObjectId id = null;
            try {
                id = mongoContentService.storeFileToGridFs(model);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (id != null) {
                attachmentId.add(id.toHexString());
            }
        }

        ProblemEntity problem = ProblemEntity.builder()
                                             .problemId(problemId)
                                             .problemName(dto.getProblemName())
                                             .problemDescription(dto.getProblemDescription())
                                             .memoryLimit(dto.getMemoryLimit())
                                             .timeLimitCPP(dto.getTimeLimitCPP())
                                             .timeLimitJAVA(dto.getTimeLimitJAVA())
                                             .timeLimitPYTHON(dto.getTimeLimitPYTHON())
                                             .levelId(dto.getLevelId())
                                             .categoryId(dto.getCategoryId())
                                             .correctSolutionLanguage(dto.getCorrectSolutionLanguage())
                                             .correctSolutionSourceCode(dto.getCorrectSolutionSourceCode())
                                             // .isPreloadCode(dto.getIsPreloadCode()) // Preload Code functionality - DISABLED
                                             // .preloadCode(dto.getPreloadCode()) // Preload Code functionality - DISABLED
                                             .solutionCheckerSourceCode(dto.getSolutionChecker())
                                             .solutionCheckerSourceLanguage(dto.getSolutionCheckerLanguage())
                                             .scoreEvaluationType(dto.getScoreEvaluationType() != null
                                                                      ? dto.getScoreEvaluationType()
                                                                      : Constants.ProblemResultEvaluationType.NORMAL.getValue())
                                             .isPublicProblem(dto.getIsPublic())
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

        if (Integer.valueOf(1).equals(dto.getCategoryId()) && !CollectionUtils.isEmpty(dto.getBlockCodes())) {
            createProblemBlocks(problemId, dto.getBlockCodes());
        }

        grantRoles(problem.getProblemId(), createdBy, List.of(UserContestProblemRole.ROLE_OWNER));
        grantRoles(problem.getProblemId(), "admin", List.of(UserContestProblemRole.ROLE_EDITOR));

        notificationsService.create(
            createdBy,
            "admin",
            createdBy + " has created a contest problem ID " + problem.getProblemId(),
            "/programming-contest/manager-view-problem-detail/" + problem.getProblemId());

        return problem;
    }

    @Transactional
    @Override
    public void editProblem(
        String problemId,
        String userId,
        EditProblemDTO dto,
        MultipartFile[] files
    ) {
        boolean hasEditPermission = problemPermissionUtil.checkEditProblemPermission(problemId, userId);
        if (!hasEditPermission) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You need to be granted permission to perform this action");
        }

        ProblemEntity problem = problemRepo.findById(problemId)
                                           .orElseThrow(() -> new EntityNotFoundException("Problem not found"));

        Integer oldCategoryId = problem.getCategoryId();
        Integer newCategoryId = dto.getCategoryId();

        boolean canEditCategoryAndBlocks = true;
        if (Objects.equals(0, oldCategoryId) && Objects.equals(0, newCategoryId)) {
        } else {
            canEditCategoryAndBlocks = canEditBlockProblem(problemId);
        }

        List<TagEntity> tags = new ArrayList<>();
        Integer[] tagIds = dto.getTagIds();
        if (tagIds != null && tagIds.length > 0) {
            List<Integer> tagIdList = new ArrayList<>(List.of(tagIds));
            tags = tagRepo.findAllByTagIdIn(tagIdList);
        }

        List<String> attachmentId = new ArrayList<>();
        List<MultipartFile> fileArray = Optional.ofNullable(files)
                                                .map(Arrays::asList)
                                                .orElseGet(Collections::emptyList);

        List<String> removedFilesId = dto.getRemovedFilesId();
        if (!StringUtils.isBlank(problem.getAttachment())) {
            String[] oldAttachmentIds = problem.getAttachment().split(";");
            for (String s : oldAttachmentIds) {
                try {
                    if (removedFilesId.contains(s)) {
                        mongoContentService.deleteFilesById(s);
                    } else {
                        attachmentId.add(s);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        for (int i = 0; i < fileArray.size(); i++) {
            MultipartFile file = fileArray.get(i);
            ContentModel model = new ContentModel(null, file);

            ObjectId id = null;
            try {
                id = mongoContentService.storeFileToGridFs(model);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (id != null) {
                attachmentId.add(id.toHexString());
            }
        }

        if (canEditCategoryAndBlocks) {
            if (Objects.equals(1, oldCategoryId)
                && Objects.equals(0, newCategoryId)) {
                problemBlockRepo.deleteByProblemId(problemId);
            } else if (Objects.equals(0, oldCategoryId)
                       && Objects.equals(1, newCategoryId)) {
                if (!CollectionUtils.isEmpty(dto.getBlockCodes())) {
                    createProblemBlocks(problemId, dto.getBlockCodes());
                }
            } else if (Objects.equals(1, oldCategoryId)
                       && Objects.equals(1, newCategoryId)) {
                if (!CollectionUtils.isEmpty(dto.getBlockCodes())) {
                    problemBlockRepo.deleteByProblemId(problemId);
                    createProblemBlocks(problemId, dto.getBlockCodes());
                }
            }
        } else {
            newCategoryId = oldCategoryId;
        }

        problem.setProblemName(dto.getProblemName());
        problem.setProblemDescription(dto.getProblemDescription());
        problem.setTimeLimitCPP(dto.getTimeLimitCPP());
        problem.setTimeLimitJAVA(dto.getTimeLimitJAVA());
        problem.setTimeLimitPYTHON(dto.getTimeLimitPYTHON());
        problem.setMemoryLimit(dto.getMemoryLimit());
        problem.setLevelId(dto.getLevelId());
        problem.setCategoryId(newCategoryId);
        problem.setCorrectSolutionSourceCode(dto.getCorrectSolutionSourceCode());
        problem.setCorrectSolutionLanguage(dto.getCorrectSolutionLanguage());
        problem.setSolutionCheckerSourceCode(dto.getSolutionChecker());
        problem.setSolutionCheckerSourceLanguage(dto.getSolutionCheckerLanguage());
        problem.setAttachment(String.join(";", attachmentId));
        problem.setScoreEvaluationType(dto.getScoreEvaluationType());
        // problem.setIsPreloadCode(dto.getIsPreloadCode()); // Preload Code functionality - DISABLED
        // problem.setPreloadCode(dto.getPreloadCode()); // Preload Code functionality - DISABLED
        problem.setTags(tags);
        problem.setSampleTestcase(dto.getSampleTestCase());

        boolean isCreator = userId.equals(problem.getCreatedBy());
        boolean isOwner = userContestProblemRoleRepo.existsByProblemIdAndUserIdAndRoleId(
            problemId, userId, UserContestProblemRole.ROLE_OWNER);

        if (isCreator || isOwner) {
            problem.setStatusId(dto.getStatus().toString());
            problem.setPublicProblem(dto.getIsPublic());
        }

        problemCacheService.saveProblemWithCache(problem);
    }

    private Map<String, Long> calculateMaxPointForProblemsInContest(String contestId) {
        List<ContestProblem> contestProblems = contestProblemRepo.findAllByContestIdAndSubmissionModeNot(
            contestId,
            ContestProblem.SUBMISSION_MODE_HIDDEN);
        List<String> problemIds = contestProblems.stream()
                                                 .map(ContestProblem::getProblemId)
                                                 .collect(Collectors.toList());

        List<TestCaseEntity> testCases = testCaseRepo.findAllByProblemIdIn(problemIds);
        Map<String, List<TestCaseEntity>> testCasesByProblemId = testCases.stream()
                                                                          .collect(Collectors.groupingBy(TestCaseEntity::getProblemId));

        ContestEntity contest = contestRepo.findContestByContestId(contestId);
        Map<String, Long> result = new HashMap<>();
        for (String problemId : problemIds) {
            long totalPoint = testCasesByProblemId.getOrDefault(problemId, Collections.emptyList())
                                                  .stream()
                                                  .filter(tc -> "Y".equals(contest.getEvaluateBothPublicPrivateTestcase()) ||
                                                                "N".equals(tc.getIsPublic()))
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
    protected ModelCreateContestProblemResponse getProblemDetail(String problemId) {
        ProblemEntity problem = problemRepo.findByProblemId(problemId);
        if (problem == null) {
            return null;
        }

        ModelCreateContestProblemResponse response = new ModelCreateContestProblemResponse();

        response.setProblemId(problem.getProblemId());
        response.setProblemName(problem.getProblemName());
        response.setProblemDescription(problem.getProblemDescription());
        response.setUserId(problem.getCreatedBy());
//            response.setTimeLimit(problem.getTimeLimit());
        response.setTimeLimitCPP(problem.getTimeLimitCPP());
        response.setTimeLimitJAVA(problem.getTimeLimitJAVA());
        response.setTimeLimitPYTHON(problem.getTimeLimitPYTHON());
        response.setMemoryLimit(problem.getMemoryLimit());
        response.setLevelId(problem.getLevelId());
        response.setCategoryId(problem.getCategoryId());
        response.setCorrectSolutionSourceCode(problem.getCorrectSolutionSourceCode());
        response.setCorrectSolutionLanguage(problem.getCorrectSolutionLanguage());
        response.setSolutionCheckerSourceCode(problem.getSolutionCheckerSourceCode());
        response.setSolutionCheckerSourceLanguage(problem.getSolutionCheckerSourceLanguage());
        response.setScoreEvaluationType(problem.getScoreEvaluationType());
        response.setIsPreloadCode(problem.getIsPreloadCode());
        response.setPreloadCode(problem.getPreloadCode());
        response.setCreatedAt(problem.getCreatedAt());
        response.setPublicProblem(problem.isPublicProblem());
        response.setTags(problem.getTags());
        response.setStatus(problem.getStatusId());
        response.setSampleTestCase(problem.getSampleTestcase());

        if (problem.getAttachment() != null) {
            String[] fileIds = problem.getAttachment().split(";", -1);
            if (fileIds.length != 0) {
                List<AttachmentMetadata> attachments = new ArrayList<>();
                for (String s : fileIds) {
                    GridFsResource content = mongoContentService.getById(s);
                    if (content != null) {
                        attachments.add(new AttachmentMetadata(s, mongoContentService.getOriginalFileName(content)));
                    }
                }
                response.setAttachments(attachments);
            } else {
                response.setAttachments(null);
            }
        } else {
            response.setAttachments(null);
        }

        if (Objects.equals(1, problem.getCategoryId())) {
            List<ProblemBlock> problemBlocks = problemBlockRepo.findByProblemId(problemId);
            if (!CollectionUtils.isEmpty(problemBlocks)) {
                List<BlockCode> blockCodes = problemBlocks.stream()
                                                          .map(this::mapToBlockCode)
                                                          .collect(Collectors.toList());
                response.setBlockCodes(blockCodes);
            } else {
                response.setBlockCodes(new ArrayList<>());
            }
        } else {
            response.setBlockCodes(null);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public void exportProblem(String problemId, String userId, OutputStream outputStream) {
        boolean hasViewPermission = problemPermissionUtil.checkViewProblemPermission(problemId, userId);
        if (!hasViewPermission) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You need to be granted permission to perform this action");
        }

        try {
            ModelCreateContestProblemResponse problem = getProblemDetail(problemId);
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
        List<File> files = new ArrayList<>();

        try {
            File problemGeneralInfoFile = exporter.exportProblemInfoToFile(problem);
            File problemDescriptionFile = exporter.exportProblemDescriptionToFile(problem);
            File problemCorrectSolutionFile = exporter.exportProblemCorrectSolutionToFile(problem);

            files.add(problemGeneralInfoFile);
            files.add(problemCorrectSolutionFile);

            if (problem.getScoreEvaluationType().equals(Constants.ProblemResultEvaluationType.CUSTOM.getValue())) {
                File problemCustomCheckerFile = exporter.exportProblemCustomCheckerToFile(problem);
                files.add(problemCustomCheckerFile);
            }

            if (!CollectionUtils.isEmpty(problem.getAttachments())) {
                files.addAll(exporter.exportProblemAttachmentToFile(problem));
            }

            files.addAll(exporter.exportProblemTestCasesToFile(problem));

        } catch (IOException e) {
            log.error("Export problem failed", e);
        }

        // Zip files.
        ZipOutputStreamUtils.zip(
            outputStream,
            files,
            CompressionMethod.DEFLATE,
            null,
            EncryptionMethod.AES,
            AesKeyStrength.KEY_STRENGTH_256);

        //delete files
        for (File file : files) {
            file.delete();
        }
    }

    @Transactional(readOnly = true)
    @Override
    public ModelCreateContestProblemResponse getProblemDetailForManager(String problemId, String userId) {
        boolean hasViewPermission = problemPermissionUtil.checkViewProblemPermission(problemId, userId);
        if (!hasViewPermission) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You need to be granted permission to perform this action");
        }

        List<String> userRoles = userContestProblemRoleRepo.getRolesByProblemIdAndUserId(problemId, userId);
        ModelCreateContestProblemResponse problemDetail = getProblemDetail(problemId);

        problemDetail.setRoles(userRoles);
        problemDetail.setCanEditBlocks(canEditBlocksOfProblem(problemId));

        return problemDetail;
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

    /**
     * Filter block codes based on contest allowed languages
     *
     * @param blockCodes       List of all block codes from problem
     * @param allowedLanguages List of languages allowed in contest (null = allow all, empty = allow none)
     * @return Filtered list of block codes that match contest allowed languages
     */
    private List<BlockCode> filterBlockCodesByContestLanguages(
        List<BlockCode> blockCodes,
        List<String> allowedLanguages
    ) {
        if (CollectionUtils.isEmpty(blockCodes)) {
            return new ArrayList<>();
        }

        // If allowedLanguages is null, allow all languages (return all block codes)
        if (allowedLanguages == null) {
            return blockCodes;
        }

        // If allowedLanguages is empty, allow no languages (return an empty list)
        if (allowedLanguages.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> allowedLanguagesSet = new HashSet<>(allowedLanguages);

        return blockCodes.stream()
                         .filter(block -> allowedLanguagesSet.contains(block.getLanguage()))
                         .collect(Collectors.toList());
    }

    private boolean canEditBlockProblem(String problemId) {
        if (contestProblemRepo.existsByProblemIdAndContestStatusNot(
            problemId,
            ContestEntity.CONTEST_STATUS_CREATED)) {
            return false;
        }

        if (contestSubmissionRepo.existsByProblemId(problemId)) {
            return false;
        }

        return true;
    }

    private boolean canEditBlocksOfProblem(String problemId) {
        ProblemEntity problem = problemRepo.findByProblemId(problemId);
        if (problem == null) {
            return false;
        }

        if (Objects.equals(0, problem.getCategoryId())) {
            return false;
        }

        return canEditBlockProblem(problemId);
    }

    @Transactional
    @Override
    public List<ModelImportProblemFromContestResponse> importProblemsFromAContest(
        String userId,
        ImportProblemsFromAContestDTO dto
    ) {
        String fromContestId = dto.getFromContestId();
        String toContestId = dto.getContestId();
        contestProblemPermissionUtil.checkContestAccessAndHasAnyRole(
            userId,
            fromContestId,
            Collections.singleton(ROLE_MANAGER));
        contestProblemPermissionUtil.checkContestAccessAndHasAnyRole(
            userId,
            toContestId,
            Collections.singleton(ROLE_MANAGER));

        List<ContestProblem> existingProblems = contestProblemRepo.findAllByContestId(toContestId);
        Set<String> existingProblemIds = existingProblems.stream()
                                                         .map(ContestProblem::getProblemId)
                                                         .collect(Collectors.toSet());

        List<ContestProblem> sourceProblems = contestProblemRepo.findAllByContestId(fromContestId);
        List<ContestProblem> toSave = new ArrayList<>();
        List<ModelImportProblemFromContestResponse> responseList = new ArrayList<>();
        for (ContestProblem sourceProblem : sourceProblems) {
            String problemId = sourceProblem.getProblemId();
            ModelImportProblemFromContestResponse response = new ModelImportProblemFromContestResponse();
            response.setProblemId(problemId);

            if (existingProblemIds.contains(problemId)) {
                response.setStatus("Problem already existed");
                responseList.add(response);
                continue;
            }

            ContestProblem newContestProblem = new ContestProblem();

            newContestProblem.setContestId(toContestId);
            newContestProblem.setProblemId(problemId);
            newContestProblem.setSubmissionMode(sourceProblem.getSubmissionMode());
            newContestProblem.setProblemRename(sourceProblem.getProblemRename());
            newContestProblem.setProblemRecode(sourceProblem.getProblemRecode());

            toSave.add(newContestProblem);
            response.setStatus("SUCCESSFUL");
            responseList.add(response);
        }

        if (!toSave.isEmpty()) {
            contestProblemRepo.saveAll(toSave);
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

        List<String> problemIds = problems.getContent().stream()
                                          .map(ProblemProjection::getProblemId)
                                          .collect(Collectors.toList());

        final Map<String, List<String>> problemRolesMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(problemIds)) {
            List<Object[]> rolesData = userContestProblemRoleRepo.findRolesByProblemIdsAndUserId(problemIds, userId);
            problemRolesMap.putAll(rolesData.stream()
                                            .collect(Collectors.groupingBy(
                                                row -> (String) row[0],
                                                Collectors.mapping(row -> (String) row[1], Collectors.toList())
                                            )));
        }

        List<ProblemDTO> dtos = problems.getContent().stream()
                                        .map(item -> convertToProblemDTO(
                                            item,
                                            problemRolesMap.getOrDefault(item.getProblemId(), new ArrayList<>())))
                                        .collect(Collectors.toList());

        return new PageImpl<>(dtos, problems.getPageable(), problems.getTotalElements());
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

    private ProblemDTO convertToProblemDTO(ProblemProjection item, List<String> userRoles) {
        ProblemDTO dto = objectMapper.convertValue(item, ProblemDTO.class);
        try {
            dto.setTags(objectMapper.readValue(
                item.getJsonTags(), new TypeReference<>() {
                }));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Set canEdit based on user roles
        dto.setCanEdit(userRoles.contains(UserContestProblemRole.ROLE_OWNER) ||
                       userRoles.contains(UserContestProblemRole.ROLE_EDITOR));

        return dto;
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
    public void cloneProblem(String userId, CloneProblemDTO cloneRequest) {
        boolean hasClonePermission = problemPermissionUtil.checkViewProblemPermission(
            cloneRequest.getOldProblemId(),
            userId);
        if (!hasClonePermission) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You need to be granted permission to perform this action");
        }

        ProblemEntity originalProblem = problemRepo.findById(cloneRequest.getOldProblemId())
                                                   .orElseThrow(() -> new EntityNotFoundException("Problem not found"));

        if (problemRepo.existsByProblemIdOrProblemName(
            cloneRequest.getNewProblemId(),
            cloneRequest.getNewProblemName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Problem ID or name already exists");
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
        newProblem.setCreatedBy(userId);
        newProblem.setLevelId(originalProblem.getLevelId());
        newProblem.setCategoryId(originalProblem.getCategoryId());
        newProblem.setSolutionCheckerSourceCode(originalProblem.getSolutionCheckerSourceCode());
        newProblem.setSolutionCheckerSourceLanguage(originalProblem.getSolutionCheckerSourceLanguage());
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

        grantRoles(newProblem.getProblemId(), userId, List.of(UserContestProblemRole.ROLE_OWNER));
        grantRoles(newProblem.getProblemId(), "admin", List.of(UserContestProblemRole.ROLE_EDITOR));

        notificationsService.create(
            userId, "admin",
            userId + " has cloned a contest problem ID " + newProblem.getProblemId(),
            "/programming-contest/manager-view-problem-detail/" + newProblem.getProblemId()
        );
    }

    private void grantRoles(String problemId, String userId, List<String> roleIds) {
        List<UserContestProblemRole> rolesToSave = new ArrayList<>();
        for (String roleId : roleIds) {
            UserContestProblemRole role = new UserContestProblemRole();

            role.setProblemId(problemId);
            role.setUserId(userId);
            role.setRoleId(roleId);

            rolesToSave.add(role);
        }

        userContestProblemRoleRepo.saveAll(rolesToSave);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProblemRoleDTO> getProblemPermissions(String problemId, String userId) {
        problemPermissionUtil.checkProblemOwnerPermission(problemId, userId);
        List<UserProblemRoleDTO> allRoles = userContestProblemRoleRepo.findAllByProblemIdWithFullName(problemId);

        Map<String, List<UserProblemRoleDTO>> userRolesMap = allRoles.stream()
                                                                     .collect(Collectors.groupingBy(UserProblemRoleDTO::getUserLoginId));

        List<UserProblemRoleDTO> result = new ArrayList<>();
        for (List<UserProblemRoleDTO> userRoles : userRolesMap.values()) {
            UserProblemRoleDTO highestRoleRecord = findHighestRole(userRoles);
            result.add(highestRoleRecord);
        }

        return result;
    }

    /**
     * Find the record with the highest role from a list of user role records
     * Priority: OWNER > EDITOR > VIEWER
     *
     * @param userRoles List of user role records
     * @return The record with the highest role
     */
    private UserProblemRoleDTO findHighestRole(List<UserProblemRoleDTO> userRoles) {
        if (CollectionUtils.isEmpty(userRoles)) {
            return null;
        }

        Map<String, Integer> rolePriority = Map.of(
            UserContestProblemRole.ROLE_OWNER, 3,
            UserContestProblemRole.ROLE_EDITOR, 2,
            UserContestProblemRole.ROLE_VIEWER, 1
        );

        // Find record with the highest priority
        return userRoles.stream()
                        .max(Comparator.comparing(role -> rolePriority.getOrDefault(role.getRoleId(), 0)))
                        .orElse(userRoles.get(0));
    }

    @Override
    @Transactional
    public Map<String, Object> grantProblemPermission(String userName, GrantProblemPermissionDTO dto) {
        validateRoleId(dto.getRoleId());
        problemPermissionUtil.checkProblemOwnerPermission(dto.getProblemId(), userName);

        List<String> userIds = CollectionUtils.isEmpty(dto.getUserIds()) ? new ArrayList<>() : dto.getUserIds();
        Set<String> groupUserIds = new HashSet<>();
        if (!CollectionUtils.isEmpty(dto.getGroupIds())) {
            groupUserIds = teacherGroupRelationRepository.findUserIdsByGroupIds(dto.getGroupIds());
        }

        Set<String> allUserIds = new HashSet<>();
        allUserIds.addAll(userIds);
        allUserIds.addAll(groupUserIds);

        if (CollectionUtils.isEmpty(allUserIds)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("addedUsers", new ArrayList<>());
            response.put("skippedUsers", new ArrayList<>());

            return response;
        }

        List<UserContestProblemRole> allExistingRoles = userContestProblemRoleRepo
            .findAllByProblemIdAndUserIdsIn(dto.getProblemId(), new ArrayList<>(allUserIds));
        Map<String, List<String>> userRolesMap = allExistingRoles.stream()
                                                                 .collect(Collectors.groupingBy(
                                                                     UserContestProblemRole::getUserId,
                                                                     Collectors.mapping(
                                                                         UserContestProblemRole::getRoleId,
                                                                         Collectors.toList())
                                                                 ));
        Map<String, List<UserContestProblemRole>> userRoleEntitiesMap = allExistingRoles.stream()
                                                                                        .collect(Collectors.groupingBy(
                                                                                            UserContestProblemRole::getUserId));

        List<String> addedUsers = new ArrayList<>();
        List<String> skippedUsers = new ArrayList<>();
        List<UserContestProblemRole> toSave = new ArrayList<>();
        List<UserContestProblemRole> toDelete = new ArrayList<>();

        for (String userId : allUserIds) {
            List<String> existingRoles = userRolesMap.getOrDefault(userId, new ArrayList<>());

            // User already has the OWNER role
            if (existingRoles.contains(UserContestProblemRole.ROLE_OWNER)) {
                skippedUsers.add(userId);
                continue;
            }

            // User already has the role to be granted
            if (existingRoles.contains(dto.getRoleId())) {
                skippedUsers.add(userId);
                continue;
            }

            // User has different roles (excluding OWNER) -> delete existing roles and grant new role
            if (!existingRoles.isEmpty()) {
                List<UserContestProblemRole> existingRoleEntities = userRoleEntitiesMap.getOrDefault(
                    userId,
                    new ArrayList<>());
                List<UserContestProblemRole> nonOwnerRoles = existingRoleEntities.stream()
                                                                                 .filter(role -> !UserContestProblemRole.ROLE_OWNER.equals(
                                                                                     role.getRoleId()))
                                                                                 .toList();
                toDelete.addAll(nonOwnerRoles);
            }

            UserContestProblemRole newRole = new UserContestProblemRole();

            newRole.setUserId(userId);
            newRole.setProblemId(dto.getProblemId());
            newRole.setRoleId(dto.getRoleId());

            toSave.add(newRole);
            addedUsers.add(userId);
        }

        if (!CollectionUtils.isEmpty(toDelete)) {
            userContestProblemRoleRepo.deleteAll(toDelete);
        }

        if (!CollectionUtils.isEmpty(toSave)) {
            userContestProblemRoleRepo.saveAll(toSave);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", CollectionUtils.isEmpty(skippedUsers));
        response.put("addedUsers", addedUsers);
        response.put("skippedUsers", skippedUsers);

        // Send notifications asynchronously after transaction commits
        if (!CollectionUtils.isEmpty(addedUsers)) {
            CompletableFuture.runAsync(() -> {
                try {
                    sendPermissionGrantNotifications(userName, dto.getProblemId(), dto.getRoleId(), addedUsers);
                } catch (Exception e) {
                    log.error("Failed to send permission grant notifications: {}", e.getMessage(), e);
                }
            });
        }

        return response;
    }

    /**
     * Validate that roleId is one of the allowed values (excluding OWNER)
     *
     * @param roleId Role ID to validate
     * @throws ResponseStatusException if roleId is invalid
     */
    private void validateRoleId(String roleId) {
        Set<String> validRoles = Set.of(
            UserContestProblemRole.ROLE_EDITOR,
            UserContestProblemRole.ROLE_VIEWER
        );

        if (!validRoles.contains(roleId)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid roleId"
            );
        }
    }

    @Override
    @Transactional
    public boolean revokeProblemPermission(String userId, RevokeProblemPermissionDTO input) {
        validateRoleId(input.getRoleId());
        problemPermissionUtil.checkProblemOwnerPermission(input.getProblemId(), userId);

        // Check if the current user is trying to remove himself
        if (userId.equals(input.getUserId())) {
            return false;
        }

        userContestProblemRoleRepo.deleteAllByProblemIdAndUserIdAndRoleId(
            input.getProblemId(),
            input.getUserId(),
            input.getRoleId());

        return true;
    }

    private List<ProblemBlock> createProblemBlocks(String problemId, List<BlockCode> blockCodes) {
        if (CollectionUtils.isEmpty(blockCodes)) {
            return new ArrayList<>();
        }

        // Filter out empty creator blocks
        List<BlockCode> filteredBlockCodes = blockCodes.stream()
                                                       .filter(bc -> Integer.valueOf(1).equals(bc.getForStudent()) ||
                                                                     !StringUtils.isBlank(bc.getCode()))
                                                       .toList();

        List<ProblemBlock> problemBlocks = new ArrayList<>();
        Map<String, List<BlockCode>> blocksByLanguage = filteredBlockCodes.stream()
                                                                          .collect(Collectors.groupingBy(BlockCode::getLanguage));
        for (List<BlockCode> blocks : blocksByLanguage.values()) {
            for (int i = 0; i < blocks.size(); i++) {
                BlockCode blockCode = blocks.get(i);
                ProblemBlock problemBlock = ProblemBlock.builder()
                                                        .problemId(problemId)
                                                        .seq(i + 1)
                                                        .sourceCode(blockCode.getCode())
                                                        .programmingLanguage(blockCode.getLanguage())
                                                        .completedBy(Integer
                                                                         .valueOf(1)
                                                                         .equals(blockCode.getForStudent()) ? 1 : 0)
                                                        .build();
                problemBlocks.add(problemBlock);
            }
        }

        return problemBlockRepo.saveAll(problemBlocks);
    }

    @Override
    @Transactional(readOnly = true)
    public ProblemDetailForParticipantDTO getProblemDetailForParticipant(
        String userId,
        String contestId,
        String problemId
    ) {
        contestProblemPermissionUtil.checkContestProblemAccess(
            userId,
            contestId,
            problemId);

        ContestEntity contest = contestRepo.findContestByContestId(contestId);
        if (ContestEntity.CONTEST_STATUS_OPEN.equals(contest.getStatusId())) {
            return null;
        }

        ContestProblem contestProblem = contestProblemRepo.findByContestIdAndProblemId(contestId, problemId);
        ModelCreateContestProblemResponse problemDetail = getProblemDetail(problemId);
        ProblemDetailForParticipantDTO response = new ProblemDetailForParticipantDTO();

        response.setSubmissionMode(contestProblem.getSubmissionMode());
        response.setProblemName(contestProblem.getProblemRename());
        response.setProblemCode(contestProblem.getProblemRecode());
        response.setIsPreloadCode(problemDetail.getIsPreloadCode());
        response.setPreloadCode(problemDetail.getPreloadCode());
        response.setAttachments(problemDetail.getAttachments());
        response.setSampleTestCase(problemDetail.getSampleTestCase());
        response.setCategoryId(problemDetail.getCategoryId());
        response.setListLanguagesAllowed(contest.getListLanguagesAllowedInContest());

        // Filter block codes based on contest allowed languages
        List<BlockCode> filteredBlockCodes = filterBlockCodesByContestLanguages(
            problemDetail.getBlockCodes(),
            contest.getListLanguagesAllowedInContest()
        );
        response.setBlockCodes(filteredBlockCodes);

        if (ContestEntity.CONTEST_PROBLEM_DESCRIPTION_VIEW_TYPE_HIDDEN.equals(contest.getProblemDescriptionViewType())) {
            response.setProblemStatement(null);
        } else {
            response.setProblemStatement(problemDetail.getProblemDescription());
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<ModelStudentOverviewProblem> getListProblemsInContestForParticipant(String userId, String contestId) {
        contestProblemPermissionUtil.checkContestAccess(userId, contestId);
        ContestEntity contest = contestService.findContest(contestId);
        if (ContestEntity.CONTEST_STATUS_OPEN.equals(contest.getStatusId())) {
            return new ArrayList<>();
        }

        List<ProblemEntity> problems = contest.getProblems();
        List<String> acceptedProblems = contestSubmissionRepo.findAcceptedProblemsInContestOfUser(contestId, userId);

        List<ModelProblemMaxSubmissionPoint> submittedProblems;
        if (Integer.valueOf(1).equals(contest.getAllowParticipantPinSubmission())) {
            submittedProblems = contestSubmissionRepo.findProblemsInContestHasFinalSelectedSubmissionOfUser(
                contestId,
                userId);
        } else {
            submittedProblems = contestSubmissionRepo.findProblemsInContestHasSubmissionOfUser(contestId, userId);
        }

        Map<String, Long> mapProblemToMaxSubmissionPoint = submittedProblems.stream()
                                                                            .collect(Collectors.toMap(
                                                                                ModelProblemMaxSubmissionPoint::getProblemId,
                                                                                ModelProblemMaxSubmissionPoint::getMaxPoint));

        Map<String, Long> mProblem2MaxPoint = calculateMaxPointForProblemsInContest(contestId);

        Map<String, ContestProblem> problemId2ContestProblem = contestProblemRepo.findByContestIdAndProblemIdIn(
                                                                                     contestId,
                                                                                     problems.stream()
                                                                                             .map(ProblemEntity::getProblemId)
                                                                                             .collect(Collectors.toSet()))
                                                                                 .stream()
                                                                                 .collect(Collectors.toMap(
                                                                                     ContestProblem::getProblemId,
                                                                                     cp -> cp));

        List<ModelStudentOverviewProblem> responses = new ArrayList<>();
        for (ProblemEntity problem : problems) {
            String problemId = problem.getProblemId();
            ContestProblem contestProblem = problemId2ContestProblem.get(problemId);

            if (contestProblem == null
                || ContestProblem.SUBMISSION_MODE_HIDDEN.equals(contestProblem.getSubmissionMode())) {
                continue;
            }

            ModelStudentOverviewProblem response = new ModelStudentOverviewProblem();
            response.setProblemId(problemId);
            response.setProblemName(contestProblem.getProblemRename());
            response.setProblemCode(contestProblem.getProblemRecode());
            response.setLevelId(problem.getLevelId());
            response.setMaxPoint(mProblem2MaxPoint.getOrDefault(problemId, 0L));

            boolean isBlockProblem = Objects.equals(problem.getCategoryId(), 1);
            if (isBlockProblem) {
                response.setBlockProblem(1);

//                List<ProblemBlock> problemBlocks = problemBlockRepo.findByProblemId(problemId);
//                if (!CollectionUtils.isEmpty(problemBlocks)) {
//                    List<BlockCode> blockCodes = problemBlocks.stream()
//                                                              .map(this::mapToBlockCode)
//                                                              .collect(Collectors.toList());
//                    response.setBlockCodes(blockCodes);
//                } else {
//                    response.setBlockCodes(new ArrayList<>());
//                }
            } else {
                response.setBlockProblem(0);
//                response.setBlockCodes(null);
            }

            if ("N".equals(contest.getContestShowTag())) {
                response.setTags(null);
            } else {
                response.setTags(problem.getTags().stream().map(TagEntity::getName).collect(Collectors.toList()));
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

    @Override
    @Transactional(readOnly = true)
    public AttachmentMetadata downloadProblemAttachment(
        String userId,
        String contestId,
        String problemId,
        String fileId
    ) {
        contestProblemPermissionUtil.checkContestProblemAccess(userId, contestId, problemId);

        ProblemEntity problem = problemRepo.findByProblemId(problemId);
        if (StringUtils.isBlank(problem.getAttachment())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        String[] fileIds = problem.getAttachment().split(";");
        boolean found = false;
        for (String id : fileIds) {
            if (id.equals(fileId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        GridFsResource content = mongoContentService.getById(fileId);
        if (content == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        try {
            byte[] data = content.getInputStream().readAllBytes();
            String fileName = mongoContentService.getOriginalFileName(content);
            return new AttachmentMetadata(fileId, fileName, data);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot read file");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentMetadata downloadProblemAttachmentForTeacher(
        String userId,
        String problemId,
        String fileId
    ) {
        boolean hasViewPermission = problemPermissionUtil.checkViewProblemPermission(problemId, userId);
        if (!hasViewPermission) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You need to be granted permission to perform this action");
        }

        ProblemEntity problem = problemRepo.findByProblemId(problemId);

        // Check if the file exists in problem attachments
        if (StringUtils.isBlank(problem.getAttachment())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        String[] fileIds = problem.getAttachment().split(";");
        boolean found = false;
        for (String id : fileIds) {
            if (id.equals(fileId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        GridFsResource content = mongoContentService.getById(fileId);
        if (content == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }

        try {
            byte[] data = content.getInputStream().readAllBytes();
            String fileName = mongoContentService.getOriginalFileName(content);
            return new AttachmentMetadata(fileId, fileName, data);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot read file");
        }
    }

    @Override
    public Page<ModelGetTestCaseDetail> getTestCaseByProblem(String problemId, String userId, TestCaseFilter filter) {
        boolean hasViewPermission = problemPermissionUtil.checkViewProblemPermission(problemId, userId);
        if (!hasViewPermission) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You need to be granted permission to perform this action");
        }

        Pageable pageable = CommonUtils.getPageable(
            filter.getPage(),
            filter.getSize(),
            Sort.by("lastUpdatedStamp").descending());

        if (filter.getFullView() != null && filter.getFullView()) {
            return testCaseRepo.getFullByProblemId(problemId, filter.getPublicOnly(), pageable);
        } else {
            return testCaseRepo.getPreviewByProblemId(problemId, pageable);
        }
    }

    /**
     * Send notifications to users who were granted problem permissions
     *
     * @param ownerUserId Name of the user who granted the permission
     * @param problemId   Problem ID
     * @param roleId      Role that was granted (EDITOR or VIEWER)
     * @param userIds     List of user IDs who received the permission
     */
    @Transactional
    protected void sendPermissionGrantNotifications(
        String ownerUserId,
        String problemId,
        String roleId,
        List<String> userIds
    ) {
        try {
            ProblemEntity problem = problemRepo.findByProblemId(problemId);
            if (problem == null) {
                log.warn("Problem {} not found for notification", problemId);
                return;
            }

            // Get owner's full name
            String ownerFullName = userService.getUserFullName(ownerUserId);
            if (StringUtils.isBlank(ownerFullName)) {
                ownerFullName = ownerUserId;
            }

            String action;
            if (UserContestProblemRole.ROLE_EDITOR.equals(roleId)) {
                action = "edit";
            } else if (UserContestProblemRole.ROLE_VIEWER.equals(roleId)) {
                action = "view";
            } else {
                log.warn("Unknown role for notification: {}", roleId);
                return;
            }

            String content = String.format(
                "%s invited you to %s problem %s",
                ownerFullName,
                action,
                problem.getProblemName());
            String notificationUrl = String.format(
                "/programming-contest/manager-view-problem-detail/%s",
                problemId);
            for (String userId : userIds) {
                notificationsService.create(ownerUserId, userId, content, notificationUrl);
            }

            log.info("Sent permission grant notifications to {} users for problem {}", userIds.size(), problemId);
        } catch (Exception e) {
            log.error("Error sending permission grant notifications for problem {}: {}", problemId, e.getMessage(), e);
        }
    }
}
