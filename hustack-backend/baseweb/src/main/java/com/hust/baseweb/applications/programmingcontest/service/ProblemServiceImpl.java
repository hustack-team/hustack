package com.hust.baseweb.applications.programmingcontest.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hust.baseweb.applications.contentmanager.model.ContentModel;
import com.hust.baseweb.applications.contentmanager.repo.MongoContentService;
import com.hust.baseweb.applications.education.classmanagement.utils.ZipOutputStreamUtils;
import com.hust.baseweb.applications.notifications.service.NotificationsService;
import com.hust.baseweb.applications.programmingcontest.constants.Constants;
import com.hust.baseweb.applications.programmingcontest.entity.*;
import com.hust.baseweb.applications.programmingcontest.exception.MiniLeetCodeException;
import com.hust.baseweb.applications.programmingcontest.model.*;
import com.hust.baseweb.applications.programmingcontest.model.externalapi.SubmissionModelResponse;
import com.hust.baseweb.applications.programmingcontest.repo.*;
import com.hust.baseweb.applications.programmingcontest.utils.ContestProblemPermissionUtil;
import com.hust.baseweb.model.ProblemFilter;
import com.hust.baseweb.model.ProblemProjection;
import com.hust.baseweb.model.dto.ProblemDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    ProblemBlockRepo problemBlockRepo;

    ContestProblemPermissionUtil contestProblemPermissionUtil;

    @Override
    @Transactional
    public ProblemEntity createProblem(
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

        if (Integer.valueOf(1).equals(dto.getCategoryId()) && !CollectionUtils.isEmpty(dto.getBlockCodes())) {
            createProblemBlocks(problemId, dto.getBlockCodes());
        }

        List<UserContestProblemRole> roles = new ArrayList<>();
        List<String> users = new ArrayList<>();
        users.add(createdBy);
        if (!"admin".equals(createdBy)) {
            users.add("admin");
        }

        List<String> roleIds = new ArrayList<>(List.of(
            UserContestProblemRole.ROLE_OWNER,
            UserContestProblemRole.ROLE_EDITOR,
            UserContestProblemRole.ROLE_VIEWER
        ));
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

        notificationsService.create(
            createdBy,
            "admin",
            createdBy + " has created a contest problem ID " + problem.getProblemId(),
            "/programming-contest/manager-view-problem-detail/" + problem.getProblemId());

        return problem;
    }

    @Transactional
    @Override
    public ProblemEntity editProblem(
        String problemId,
        String userId,
        ModelUpdateContestProblem dto,
        MultipartFile[] files
    ) {
        ProblemEntity problem = problemRepo.findById(problemId)
                                           .orElseThrow(() -> new EntityNotFoundException("Problem not found"));

        List<UserContestProblemRole> roles = userContestProblemRoleRepo.findAllByProblemIdAndUserId(problemId, userId);
        boolean hasPermission = false;
        boolean isAuthor = userId.equals(problem.getCreatedBy());

        if (isAuthor) {
            boolean hasOwnerRole = roles.stream()
                                        .anyMatch(role -> UserContestProblemRole.ROLE_OWNER.equals(role.getRoleId()));

            if (!hasOwnerRole) {
                log.warn("Author {} does not have ROLE_OWNER for problem {}", userId, problemId);
            }

            hasPermission = hasOwnerRole;
        } else {
            for (UserContestProblemRole role : roles) {
                if (UserContestProblemRole.ROLE_EDITOR.equals(role.getRoleId())) {
                    hasPermission = true;
                    break;
                }
            }
        }

        if (!hasPermission) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No permission");
        }

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
        String[] fileId = dto.getFileId();
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
        problem.setLevelId(dto.getLevelId());
        problem.setSolution(dto.getSolution());
//        problem.setTimeLimit(dto.getTimeLimit());
        problem.setIsPreloadCode(dto.getIsPreloadCode());
        problem.setPreloadCode(dto.getPreloadCode());
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
        problem.setCategoryId(newCategoryId);

        if (isAuthor) {
            problem.setStatusId(dto.getStatus().toString());
        }

        return problemCacheService.saveProblemWithCache(problem);
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
        response.setSolution(problem.getSolution());
        response.setIsPreloadCode(problem.getIsPreloadCode());
        response.setPreloadCode(problem.getPreloadCode());
        response.setLevelOrder(problem.getLevelOrder());
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
    public void exportProblem(String problemId, OutputStream outputStream) {
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
        ProblemEntity problem = problemRepo.findByProblemId(problemId);
        if (problem == null) {
            throw new EntityNotFoundException("Problem not found");
        }

        List<String> allowedRoles = new ArrayList<>(List.of(
            UserContestProblemRole.ROLE_OWNER,
            UserContestProblemRole.ROLE_EDITOR
        ));
        if (ProblemEntity.PROBLEM_STATUS_HIDDEN.equals(problem.getStatusId())) {
        } else if (ProblemEntity.PROBLEM_STATUS_OPEN.equals(problem.getStatusId())) {
            allowedRoles.add(UserContestProblemRole.ROLE_VIEWER);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to view this problem");
        }

        List<String> roles = userContestProblemRoleRepo.getRolesByProblemIdAndUserId(problemId, userId);
        boolean hasPermission = !CollectionUtils.isEmpty(roles) && roles.stream().anyMatch(allowedRoles::contains);
        if (!hasPermission) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to view this problem");
        }

        ModelCreateContestProblemResponse problemDetail = getProblemDetail(problemId);
        problemDetail.setRoles(roles);
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
    public ProblemEntity cloneProblem(String userId, CloneProblemDTO cloneRequest) throws MiniLeetCodeException {
        ProblemEntity originalProblem = problemRepo.findById(cloneRequest.getOldProblemId())
                                                   .orElseThrow(() -> new EntityNotFoundException("Problem not found"));

        List<String> allowedCloneRoles = new ArrayList<>(List.of(
            UserContestProblemRole.ROLE_OWNER,
            UserContestProblemRole.ROLE_EDITOR
        ));
        if (ProblemEntity.PROBLEM_STATUS_HIDDEN.equals(originalProblem.getStatusId())) {
        } else if (ProblemEntity.PROBLEM_STATUS_OPEN.equals(originalProblem.getStatusId())) {
            allowedCloneRoles.add(UserContestProblemRole.ROLE_VIEWER);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to clone this problem");
        }

        boolean hasClonePermission = userContestProblemRoleRepo.existsByProblemIdAndUserIdAndRoleIdIn(
            cloneRequest.getOldProblemId(),
            userId,
            allowedCloneRoles);
        if (!hasClonePermission) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to clone this problem");
        }

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

        grantRoles(newProblem.getProblemId(), userId);
        grantRoles(newProblem.getProblemId(), "admin");

        notificationsService.create(
            userId, "admin",
            userId + " has cloned a contest problem ID " + newProblem.getProblemId(),
            "/programming-contest/manager-view-problem-detail/" + newProblem.getProblemId()
        );

        return newProblem;
    }

    private void grantRoles(String problemId, String userId) {
        String[] roles = {
            UserContestProblemRole.ROLE_OWNER,
            UserContestProblemRole.ROLE_EDITOR,
            UserContestProblemRole.ROLE_VIEWER};

        List<UserContestProblemRole> toSave = new ArrayList<>();
        for (String roleId : roles) {
            UserContestProblemRole upr = new UserContestProblemRole();

            upr.setProblemId(problemId);
            upr.setUserId(userId);
            upr.setRoleId(roleId);

            toSave.add(upr);
        }
        userContestProblemRoleRepo.saveAll(toSave);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ModelResponseUserProblemRole> getUserProblemRoles(String problemId, String userId) {
        checkProblemOwnerPermission(problemId, userId);
        return userContestProblemRoleRepo.findAllByProblemIdWithFullName(problemId);
    }

    @Transactional
    @Override
    public Map<String, Object> addUserProblemRole(String userName, ModelUserProblemRoleInput input) throws Exception {
        checkProblemOwnerPermission(input.getProblemId(), userName);

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
            if (L != null && !L.isEmpty()) {
                success = false;
                skippedUsers.add(userId);
                continue;
            }
            UserContestProblemRole e = new UserContestProblemRole();

            e.setUserId(userId);
            e.setProblemId(input.getProblemId());
            e.setRoleId(input.getRoleId());

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
    public boolean removeAUserProblemRole(String userName, ModelUserProblemRole input) {
        checkProblemOwnerPermission(input.getProblemId(), userName);
        List<UserContestProblemRole> record = userContestProblemRoleRepo.findAllByProblemIdAndUserIdAndRoleId(
            input.getProblemId(),
            input.getUserId(),
            input.getRoleId());

        if (CollectionUtils.isEmpty(record)) {
            return false;
        }

        userContestProblemRoleRepo.deleteAll(record);
        return true;
    }

    private void checkProblemOwnerPermission(String problemId, String userId) {
        if (!problemRepo.existsByProblemId(problemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
        }

        boolean isOwner = userContestProblemRoleRepo.existsByProblemIdAndUserIdAndRoleId(
            problemId,
            userId,
            UserContestProblemRole.ROLE_OWNER);
        if (!isOwner) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You don't have permission to perform this operation");
        }
    }

    private List<ProblemBlock> createProblemBlocks(String problemId, List<BlockCode> blockCodes) {
        if (CollectionUtils.isEmpty(blockCodes)) {
            return new ArrayList<>();
        }

        List<ProblemBlock> problemBlocks = new ArrayList<>();
        Map<String, List<BlockCode>> blocksByLanguage = blockCodes.stream()
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
        response.setBlockCodes(problemDetail.getBlockCodes());
        response.setListLanguagesAllowed(contest.getListLanguagesAllowedInContest());

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
        // Check if teacher has minimum VIEW permission to access this problem
        ProblemEntity problem = problemRepo.findByProblemId(problemId);
        if (problem == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
        }

        List<String> allowedRoles = new ArrayList<>(List.of(
            UserContestProblemRole.ROLE_OWNER,
            UserContestProblemRole.ROLE_EDITOR
        ));
        if (ProblemEntity.PROBLEM_STATUS_HIDDEN.equals(problem.getStatusId())) {
            // For hidden problems, only OWNER and EDITOR can access
        } else if (ProblemEntity.PROBLEM_STATUS_OPEN.equals(problem.getStatusId())) {
            // For open problems, VIEWER can also access
            allowedRoles.add(UserContestProblemRole.ROLE_VIEWER);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to access this problem");
        }

        List<String> roles = userContestProblemRoleRepo.getRolesByProblemIdAndUserId(problemId, userId);
        boolean hasPermission = !CollectionUtils.isEmpty(roles) && roles.stream().anyMatch(allowedRoles::contains);
        if (!hasPermission) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to access this problem");
        }

        // Check if file exists in problem attachments
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

}
