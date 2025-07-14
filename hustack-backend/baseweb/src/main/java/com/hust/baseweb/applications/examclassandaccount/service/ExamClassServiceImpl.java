package com.hust.baseweb.applications.examclassandaccount.service;

import com.hust.baseweb.applications.examclassandaccount.entity.ExamClass;
import com.hust.baseweb.applications.examclassandaccount.entity.ExamClassUserloginMap;
import com.hust.baseweb.applications.examclassandaccount.model.ModelCreateExamClass;
import com.hust.baseweb.applications.examclassandaccount.model.ModelRepsonseExamClassDetail;
import com.hust.baseweb.applications.examclassandaccount.repo.ExamClassRepo;
import com.hust.baseweb.applications.examclassandaccount.repo.ExamClassUserloginMapRepo;
import com.hust.baseweb.applications.examclassandaccount.utils.ExamClassAuthorizationUtils;
import com.hust.baseweb.dto.response.ApiResponse;
import com.hust.baseweb.dto.response.ErrorCode;
import com.hust.baseweb.dto.response.SuccessCode;
import com.hust.baseweb.entity.UserLogin;
import com.hust.baseweb.repo.UserLoginRepo;
import com.hust.baseweb.service.KeycloakAdminService;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.hust.baseweb.utils.CharGroupPolicy.ONLY_LOWER;
import static com.hust.baseweb.utils.PdfUtils.exportPdf;
import static com.hust.baseweb.utils.RandomGenerator.generateAlphaNumericRandomString;
import static com.hust.baseweb.utils.RandomGenerator.generateRandomStringWithSpecialChars;

@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@Service
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamClassServiceImpl implements ExamClassService {

    ExamClassRepo examClassRepo;

    ExamClassUserloginMapRepo examClassUserloginMapRepo;

    ExamClassUserloginMapService examClassUserloginMapService;

    UserLoginRepo userLoginRepo;

    KeycloakAdminService keycloakService;

    ExamClassAuthorizationUtils examClassAuthorizationUtils;

    @Override
    public List<ExamClass> getAllExamClass() {
        return examClassRepo.findAll();
    }

    @Override
    public ExamClass createExamClass(String userLoginId, ModelCreateExamClass m) {
        ExamClass ec = new ExamClass();
        ec.setName(m.getName());
        ec.setDescription(m.getDescription());
        ec.setExecuteDate(m.getExecuteDate());
        ec.setCreatedByUserId(userLoginId);
        ec.setStatus(ExamClass.STATUS_ACTIVE);
        ec = examClassRepo.save(ec);
        return ec;
    }

    @Override
    public Object deleteAccounts(String userId, UUID examClassId) {
        examClassAuthorizationUtils.checkUserIsExamClassCreator(userId, examClassId);

        Map<String, Object> response = (Map<String, Object>) updateStatus(userId, examClassId, false);
        int count = examClassUserloginMapRepo.deleteByExamClassIdAndStatusIsNot(examClassId, ExamClass.STATUS_ACTIVE);
        response.put("success", count);
        return response;
    }

    @Override
    public ModelRepsonseExamClassDetail getExamClassDetail(String userId, UUID examClassId) {
        examClassAuthorizationUtils.checkUserIsExamClassCreator(userId, examClassId);

        ExamClass ec = examClassRepo
            .findById(examClassId)
            .orElseThrow(() -> new EntityNotFoundException("Exam class with ID " + examClassId + " not found"));
        ModelRepsonseExamClassDetail examClassDetail = new ModelRepsonseExamClassDetail();

        examClassDetail.setExamClassId(ec.getId());
        examClassDetail.setName(ec.getName());
        examClassDetail.setDescription(ec.getDescription());
        examClassDetail.setExecuteDate(ec.getExecuteDate());
        examClassDetail.setAccounts(examClassUserloginMapService.findByExamClassId(examClassId));

        return examClassDetail;
    }

    /**
     * @param userId      ID of the user requesting the export
     * @param examClassId ID of the exam class to export
     * @return PDF byte array
     */
    @Override
    public byte[] exportExamClass(String userId, UUID examClassId) {
        examClassAuthorizationUtils.checkUserIsExamClassCreator(userId, examClassId);

        List<ExamClassUserloginMap> accounts = getExamClassDetail(userId, examClassId).getAccounts();
        return exportPdf(accounts, "reports/exam-class-account_report.jasper", new HashMap<>());
    }

    @Override
    public Object resetPassword(String userId, UUID examClassId) {
        examClassAuthorizationUtils.checkUserIsExamClassCreator(userId, examClassId);

        List<ExamClassUserloginMap> entities = new ArrayList<>();
        List<ExamClassUserloginMap> all = examClassUserloginMapRepo.findByExamClassId(examClassId);
        if (!CollectionUtils.isEmpty(all)) {
            entities = all.stream()
                          .filter(e -> !StringUtils.isBlank(e.getRandomUserLoginId()))
                          .collect(Collectors.toList());
        }

        Map<String, Object> response = new HashMap<>();
        int success = 0, fail = 0;
        if (!CollectionUtils.isEmpty(entities)) {
            List<ExamClassUserloginMap> updateSuccessful = new ArrayList<>();
            List<ExamClassUserloginMap> updateFailed = new ArrayList<>();

            for (ExamClassUserloginMap e : entities) {
                try {
                    String newPassword = generateRandomStringWithSpecialChars(12);
                    keycloakService.resetPassword(e.getRandomUserLoginId(), newPassword, true);
                    e.setPassword(newPassword);
                    updateSuccessful.add(e);
                    success++;
                } catch (Exception ex) {
                    updateFailed.add(e);
                    fail++;
                    log.error(
                        "Failed to update status for username={} in examClassId={}. Error: {}",
                        e.getRandomUserLoginId(), examClassId, ex.getMessage());
                }
            }

            examClassUserloginMapRepo.saveAll(updateSuccessful);
            response.put("success", success);
            response.put("fail", fail);
            response.put("updateFailures", updateFailed);
        }

        return response;
    }

    @Override
    public Object generateAccounts(String userId, UUID examClassId) {
        examClassAuthorizationUtils.checkUserIsExamClassCreator(userId, examClassId);

        List<ExamClassUserloginMap> entities = new ArrayList<>();
        List<ExamClassUserloginMap> all = examClassUserloginMapRepo.findByExamClassId(examClassId);
        if (!CollectionUtils.isEmpty(all)) {
            entities = all.stream()
                          .filter(e -> e.getRandomUserLoginId() == null ||
                                       StringUtils.isBlank(e.getRandomUserLoginId()))
                          .collect(Collectors.toList());
        }

        Map<String, Object> response = new HashMap<>();
        int success = 0, fail = 0;
        if (!CollectionUtils.isEmpty(entities)) {
            List<ExamClassUserloginMap> updateSuccessful = new ArrayList<>();
            List<ExamClassUserloginMap> updateFailed = new ArrayList<>();
            List<UserLogin> userLogins = new ArrayList<>();

            for (ExamClassUserloginMap ecu : entities) {
                String username;
                String password;

                UserRepresentation user = new UserRepresentation();

                user.setFirstName(ecu.getFullname());
                user.setEnabled(false);

                Map<String, List<String>> attributes = new HashMap<>();
                attributes.put("oneTimeAccount", List.of("true"));
                attributes.put(
                    "mail",
                    List.of(ecu.getRealUserLoginId())); // Cannot use 'email' as an attribute name
                attributes.put("studentCode", List.of(ecu.getStudentCode()));
                user.setAttributes(attributes);

                int maxRetries = 3;
                int attempt = 0;
                while (attempt < maxRetries) {
                    try {
                        username = "exam-" + generateAlphaNumericRandomString(6, ONLY_LOWER);
                        password = generateRandomStringWithSpecialChars(12);
                        user.setUsername(username);

                        if (keycloakService.createUser(user, password)) {
                            ecu.setRandomUserLoginId(username);
                            ecu.setPassword(password);
                            ecu.setStatus(ExamClass.STATUS_DISABLE);

                            updateSuccessful.add(ecu);

                            //
                            UserLogin ul = new UserLogin();

                            ul.setUserLoginId(username);
                            ul.setEmail(ecu.getRealUserLoginId());
                            ul.setEnabled(false);
                            ul.setFirstName(ecu.getFullname());

                            userLogins.add(ul);
                            success++;
                            break;
                        } else {
                            attempt++;
                        }
                    } catch (Exception ex) {
                        attempt++;
                        log.error("Lỗi tạo user, thử lại lần {}: {}", attempt, ex.getMessage());
                    }
                }

                if (attempt == maxRetries) {
                    updateFailed.add(ecu);
                    fail++;
                    log.error(
                        "Failed to update status for username={} in examClassId={}",
                        ecu.getRandomUserLoginId(), examClassId);
                }
            }

            userLoginRepo.saveAll(userLogins);
            examClassUserloginMapRepo.saveAll(updateSuccessful);

            response.put("success", success);
            response.put("fail", fail);
            response.put("updateFailures", updateFailed);
        }

        return response;
    }

    @Override
    public Object updateStatus(String userId, UUID examClassId, boolean enabled) {
        examClassAuthorizationUtils.checkUserIsExamClassCreator(userId, examClassId);

        String currentStatus = enabled ? ExamClass.STATUS_DISABLE : ExamClass.STATUS_ACTIVE;
        List<ExamClassUserloginMap> data = examClassUserloginMapRepo.findByExamClassIdAndStatus(
            examClassId,
            currentStatus);

        Map<String, Object> response = new HashMap<>();
        int success = 0, fail = 0;
        if (!CollectionUtils.isEmpty(data)) {
            List<ExamClassUserloginMap> updateSuccessful = new ArrayList<>();
            List<ExamClassUserloginMap> updateFailed = new ArrayList<>();
            String newStatus = enabled ? ExamClass.STATUS_ACTIVE : ExamClass.STATUS_DISABLE;

            for (ExamClassUserloginMap e : data) {
                try {
                    if (!StringUtils.isBlank(e.getRandomUserLoginId())) {
                        keycloakService.updateEnabledUser(e.getRandomUserLoginId(), enabled);
                        if (!enabled) { // log out all user's sessions after disabling the account
                            keycloakService.logout(e.getRandomUserLoginId());
                        }
                        e.setStatus(newStatus);
                        updateSuccessful.add(e);
                        success++;
                    }
                } catch (Exception ex) {
                    updateFailed.add(e);
                    fail++;
                    log.error(
                        "Failed to update status for username={} in examClassId={}, targetEnabled={}. Error: {}",
                        e.getRandomUserLoginId(), examClassId, enabled, ex.getMessage());
                }
            }

            examClassUserloginMapRepo.saveAll(updateSuccessful);
            response.put("success", success);
            response.put("fail", fail);
            response.put("updateFailures", updateFailed);
        }

        return response;
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteAccount(String userId, UUID examClassId, UUID accountId) {
        ExamClassUserloginMap account = examClassAuthorizationUtils.checkUserIsCreatorAndAccountExists(
            userId,
            examClassId,
            accountId);

        String keycloakUsername = account.getRandomUserLoginId();
        try {
            userLoginRepo.deleteById(keycloakUsername);
            keycloakService.deleteUserIfExists(keycloakUsername);
            examClassUserloginMapRepo.delete(account);

            return ApiResponse.of(SuccessCode.ACCOUNT_DELETED);
        } catch (DataIntegrityViolationException e) {
            keycloakService.updateEnabledUser(keycloakUsername, false);
            keycloakService.logout(keycloakUsername);

            account.setStatus(ExamClass.STATUS_DISABLE);
            examClassUserloginMapRepo.save(account);

            UserLogin userLogin = userLoginRepo.findById(keycloakUsername).orElse(null);
            if (userLogin != null) {
                userLogin.setEnabled(false);
                userLoginRepo.save(userLogin);
            }

            return ApiResponse.of(SuccessCode.ACCOUNT_DISABLED);
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> resetPasswordForAccount(String userId, UUID examClassId, UUID accountId) {
        ExamClassUserloginMap account = examClassAuthorizationUtils.checkUserIsCreatorAndAccountExists(
            userId,
            examClassId,
            accountId);

        if (!StringUtils.isBlank(account.getRandomUserLoginId())) {
            String newPassword = generateRandomStringWithSpecialChars(12);
            keycloakService.resetPassword(account.getRandomUserLoginId(), newPassword, true);

            account.setPassword(newPassword);
            examClassUserloginMapRepo.save(account);

            return ApiResponse.of(SuccessCode.PASSWORD_RESET_SUCCESS);
        } else {
            return ApiResponse.of(ErrorCode.ACCOUNT_NOT_GENERATED);
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> updateStatusForAccount(String userId, UUID examClassId, UUID accountId, boolean enabled) {
        ExamClassUserloginMap account = examClassAuthorizationUtils.checkUserIsCreatorAndAccountExists(
            userId,
            examClassId,
            accountId);

        if (!StringUtils.isBlank(account.getRandomUserLoginId())) {
            String newStatus = enabled ? ExamClass.STATUS_ACTIVE : ExamClass.STATUS_DISABLE;
            keycloakService.updateEnabledUser(account.getRandomUserLoginId(), enabled);
            if (!enabled) { // log out all user's sessions after disabling the account
                keycloakService.logout(account.getRandomUserLoginId());
            }

            account.setStatus(newStatus);
            examClassUserloginMapRepo.save(account);

            UserLogin userLogin = userLoginRepo.findById(account.getRandomUserLoginId()).orElse(null);
            if (userLogin != null) {
                userLogin.setEnabled(enabled);
                userLoginRepo.save(userLogin);
            }

            return ApiResponse.of(
                SuccessCode.ACCOUNT_STATUS_UPDATED,
                null,
                "Account " + (enabled ? "enabled" : "disabled") + " successfully");
        } else {
            return ApiResponse.of(ErrorCode.ACCOUNT_NOT_GENERATED);
        }
    }
}
