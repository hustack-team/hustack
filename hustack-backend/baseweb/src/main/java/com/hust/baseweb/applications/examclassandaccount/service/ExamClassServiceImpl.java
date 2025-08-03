package com.hust.baseweb.applications.examclassandaccount.service;

import com.hust.baseweb.applications.examclassandaccount.entity.ExamAccount;
import com.hust.baseweb.applications.examclassandaccount.entity.ExamClass;
import com.hust.baseweb.applications.examclassandaccount.model.ExamClassAccountDTO;
import com.hust.baseweb.applications.examclassandaccount.model.ModelCreateExamClass;
import com.hust.baseweb.applications.examclassandaccount.model.ModelRepsonseExamClassDetail;
import com.hust.baseweb.applications.examclassandaccount.repo.ExamAccountRepo;
import com.hust.baseweb.applications.examclassandaccount.repo.ExamClassRepo;
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
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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

    ExamAccountRepo examAccountRepo;

    UserLoginRepo userLoginRepo;

    KeycloakAdminService keycloakService;

    ExamClassAuthorizationUtils examClassAuthorizationUtils;

    @Override
    @Transactional(readOnly = true)
    public List<ExamClass> getAllExamClass() {
        return examClassRepo.findAllByOrderByCreatedStampDesc();
    }

    @Override
    public ExamClass createExamClass(String userId, ModelCreateExamClass dto) {
        ExamClass examClass = new ExamClass();

        examClass.setName(dto.getName());
        examClass.setDescription(dto.getDescription());
        examClass.setExecuteDate(dto.getExecuteDate());
        examClass.setStatus(ExamClass.STATUS_ACTIVE);

        return examClassRepo.save(examClass);
    }

    @Override
    public Object deleteAccounts(String userId, UUID examClassId) {
        examClassAuthorizationUtils.checkUserIsExamClassCreator(userId, examClassId);

        List<ExamAccount> accounts = examAccountRepo.findByExamClassId(examClassId);

        Map<String, Object> response = new HashMap<>();
        int deletedCount = 0;
        List<ExamAccount> disabledAccounts = new ArrayList<>();
        List<ExamAccount> failedAccounts = new ArrayList<>();

        for (ExamAccount account : accounts) {
            String keycloakUsername = account.getRandomUserLoginId();

            if (StringUtils.isBlank(keycloakUsername)) {
                examAccountRepo.delete(account);
                deletedCount++;
                continue;
            }

            try {
//                userLoginRepo.deleteById(keycloakUsername);
//                keycloakService.deleteUserIfExists(keycloakUsername);
//                examAccountRepo.delete(account);
//                deletedCount++;
//            } catch (DataIntegrityViolationException e) {
                keycloakService.updateEnabledUser(keycloakUsername, false);
                keycloakService.logout(keycloakUsername);

                account.setStatus(ExamClass.STATUS_DISABLE);
                examAccountRepo.save(account);

                UserLogin userLogin = userLoginRepo.findById(keycloakUsername).orElse(null);
                if (userLogin != null) {
                    userLogin.setEnabled(false);
                    userLoginRepo.save(userLogin);
                }

                disabledAccounts.add(account);
            } catch (Exception e) {
                log.error(
                    "Error deleting account {} in exam class {}: {}",
                    keycloakUsername, examClassId, e.getMessage());

                failedAccounts.add(account);
            }
        }

        response.put("deleted", deletedCount);
        response.put("disabled", disabledAccounts.size());
        response.put("disabledAccounts", disabledAccounts);
        response.put("failed", failedAccounts.size());
        response.put("failedAccounts", failedAccounts);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<ModelRepsonseExamClassDetail> getExamClassDetail(String userId, UUID examClassId) {
        examClassAuthorizationUtils.checkUserIsExamClassCreator(userId, examClassId);

        ExamClass ec = examClassRepo
            .findById(examClassId)
            .orElseThrow(() -> new EntityNotFoundException("Exam class with ID " + examClassId + " not found"));

        ModelRepsonseExamClassDetail examClassDetail = new ModelRepsonseExamClassDetail();

        examClassDetail.setExamClassId(ec.getId());
        examClassDetail.setName(ec.getName());
        examClassDetail.setDescription(ec.getDescription());
        examClassDetail.setExecuteDate(ec.getExecuteDate());
        examClassDetail.setAccounts(examAccountRepo.findByExamClassId(examClassId));

        return ApiResponse.of(SuccessCode.SUCCESS, examClassDetail);
    }

    /**
     * @param userId      ID of the user requesting the export
     * @param examClassId ID of the exam class to export
     * @return PDF byte array
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportExamClass(String userId, UUID examClassId) {
        examClassAuthorizationUtils.checkUserIsExamClassCreator(userId, examClassId);

        List<ExamAccount> accounts = examAccountRepo.findByExamClassId(examClassId);
        return exportPdf(accounts, "reports/exam-class-account_report.jasper", new HashMap<>());
    }

    @Override
    public Object resetPassword(String userId, UUID examClassId) {
        examClassAuthorizationUtils.checkUserIsExamClassCreator(userId, examClassId);

        List<ExamAccount> accounts = new ArrayList<>();
        List<ExamAccount> all = examAccountRepo.findByExamClassId(examClassId);
        if (!CollectionUtils.isEmpty(all)) {
            accounts = all.stream()
                          .filter(e -> !StringUtils.isBlank(e.getRandomUserLoginId()))
                          .collect(Collectors.toList());
        }

        Map<String, Object> response = new HashMap<>();
        int success = 0, fail = 0;
        if (!CollectionUtils.isEmpty(accounts)) {
            List<ExamAccount> updateSuccessful = new ArrayList<>();
            List<ExamAccount> updateFailed = new ArrayList<>();

            for (ExamAccount account : accounts) {
                try {
                    String newPassword = generateRandomStringWithSpecialChars(12);
                    keycloakService.resetPassword(account.getRandomUserLoginId(), newPassword, true);
                    account.setPassword(newPassword);
                    updateSuccessful.add(account);
                    success++;
                } catch (Exception ex) {
                    updateFailed.add(account);
                    fail++;
                    log.error(
                        "Failed to update status for username={} in examClassId={}. Error: {}",
                        account.getRandomUserLoginId(), examClassId, ex.getMessage());
                }
            }

            examAccountRepo.saveAll(updateSuccessful);
            response.put("success", success);
            response.put("fail", fail);
            response.put("updateFailures", updateFailed);
        }

        return response;
    }

    @Override
    public Object generateAccounts(String userId, UUID examClassId) {
        examClassAuthorizationUtils.checkUserIsExamClassCreator(userId, examClassId);

        List<ExamAccount> accounts = new ArrayList<>();
        List<ExamAccount> all = examAccountRepo.findByExamClassId(examClassId);
        if (!CollectionUtils.isEmpty(all)) {
            accounts = all.stream()
                          .filter(e -> e.getRandomUserLoginId() == null ||
                                       StringUtils.isBlank(e.getRandomUserLoginId()))
                          .collect(Collectors.toList());
        }

        Map<String, Object> response = new HashMap<>();
        int success = 0, fail = 0;
        if (!CollectionUtils.isEmpty(accounts)) {
            List<ExamAccount> updateSuccessful = new ArrayList<>();
            List<ExamAccount> updateFailed = new ArrayList<>();
            List<UserLogin> userLogins = new ArrayList<>();

            for (ExamAccount account : accounts) {
                String username;
                String password;

                UserRepresentation user = new UserRepresentation();

                user.setFirstName(account.getFullname());
                user.setEnabled(false);

                Map<String, List<String>> attributes = new HashMap<>();
                attributes.put("oneTimeAccount", List.of("true"));
                attributes.put(
                    "mail",
                    List.of(account.getRealUserLoginId())); // Cannot use 'email' as an attribute name in Keycloak
                attributes.put("studentCode", List.of(account.getStudentCode()));
                user.setAttributes(attributes);

                int maxRetries = 3;
                int attempt = 0;
                while (attempt < maxRetries) {
                    try {
                        username = "exam-" + generateAlphaNumericRandomString(6, ONLY_LOWER);
                        password = generateRandomStringWithSpecialChars(12);
                        user.setUsername(username);

                        if (keycloakService.createUser(user, password)) {
                            account.setRandomUserLoginId(username);
                            account.setPassword(password);
                            account.setStatus(ExamClass.STATUS_DISABLE);

                            updateSuccessful.add(account);

                            //
                            UserLogin ul = new UserLogin();

                            ul.setUserLoginId(username);
                            ul.setEmail(account.getRealUserLoginId());
                            ul.setEnabled(false);
                            ul.setFirstName(account.getFullname());

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
                    updateFailed.add(account);
                    fail++;
                    log.error(
                        "Failed to update status for username={} in examClassId={}",
                        account.getRandomUserLoginId(), examClassId);
                }
            }

            userLoginRepo.saveAll(userLogins);
            examAccountRepo.saveAll(updateSuccessful);

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
        List<ExamAccount> accounts = examAccountRepo.findByExamClassIdAndStatus(
            examClassId,
            currentStatus);

        Map<String, Object> response = new HashMap<>();
        int success = 0, fail = 0;
        if (!CollectionUtils.isEmpty(accounts)) {
            List<ExamAccount> updateSuccessful = new ArrayList<>();
            List<ExamAccount> updateFailed = new ArrayList<>();
            String newStatus = enabled ? ExamClass.STATUS_ACTIVE : ExamClass.STATUS_DISABLE;

            for (ExamAccount account : accounts) {
                try {
                    if (!StringUtils.isBlank(account.getRandomUserLoginId())) {
                        keycloakService.updateEnabledUser(account.getRandomUserLoginId(), enabled);
                        if (!enabled) { // Log out all user's sessions after disabling the account
                            keycloakService.logout(account.getRandomUserLoginId());
                        }
                        account.setStatus(newStatus);
                        updateSuccessful.add(account);
                        success++;
                    }
                } catch (Exception ex) {
                    updateFailed.add(account);
                    fail++;
                    log.error(
                        "Failed to update status for username={} in examClassId={}, targetEnabled={}. Error: {}",
                        account.getRandomUserLoginId(), examClassId, enabled, ex.getMessage());
                }
            }

            examAccountRepo.saveAll(updateSuccessful);
            response.put("success", success);
            response.put("fail", fail);
            response.put("updateFailures", updateFailed);
        }

        return response;
    }

    @Override
    @Transactional
    public ApiResponse<Void> deleteAccount(String userId, UUID examClassId, UUID accountId) {
        ExamAccount account = examClassAuthorizationUtils.checkUserIsCreatorAndAccountExists(
            userId,
            examClassId,
            accountId);

        String keycloakUsername = account.getRandomUserLoginId();

        if (StringUtils.isBlank(keycloakUsername)) {
            examAccountRepo.delete(account);
            return ApiResponse.of(SuccessCode.ACCOUNT_DELETED);
        }

//        try {
//            userLoginRepo.deleteById(keycloakUsername);
//            keycloakService.deleteUserIfExists(keycloakUsername);
//            examAccountRepo.delete(account);
//
//            return ApiResponse.of(SuccessCode.ACCOUNT_DELETED);
//        } catch (DataIntegrityViolationException e) {
        keycloakService.updateEnabledUser(keycloakUsername, false);
        keycloakService.logout(keycloakUsername);

        account.setStatus(ExamClass.STATUS_DISABLE);
        examAccountRepo.save(account);

        UserLogin userLogin = userLoginRepo.findById(keycloakUsername).orElse(null);
        if (userLogin != null) {
            userLogin.setEnabled(false);
            userLoginRepo.save(userLogin);
        }

        return ApiResponse.of(SuccessCode.ACCOUNT_DISABLED);
//        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> resetPasswordForAccount(String userId, UUID examClassId, UUID accountId) {
        ExamAccount account = examClassAuthorizationUtils.checkUserIsCreatorAndAccountExists(
            userId,
            examClassId,
            accountId);

        if (!StringUtils.isBlank(account.getRandomUserLoginId())) {
            String newPassword = generateRandomStringWithSpecialChars(12);
            keycloakService.resetPassword(account.getRandomUserLoginId(), newPassword, true);

            account.setPassword(newPassword);
            examAccountRepo.save(account);

            return ApiResponse.of(SuccessCode.PASSWORD_RESET_SUCCESS);
        } else {
            return ApiResponse.of(ErrorCode.ACCOUNT_NOT_GENERATED);
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> updateStatusForAccount(String userId, UUID examClassId, UUID accountId, boolean enabled) {
        ExamAccount account = examClassAuthorizationUtils.checkUserIsCreatorAndAccountExists(
            userId,
            examClassId,
            accountId);

        if (!StringUtils.isBlank(account.getRandomUserLoginId())) {
            String newStatus = enabled ? ExamClass.STATUS_ACTIVE : ExamClass.STATUS_DISABLE;
            keycloakService.updateEnabledUser(account.getRandomUserLoginId(), enabled);
            if (!enabled) { // Log out all user's sessions after disabling the account
                keycloakService.logout(account.getRandomUserLoginId());
            }

            account.setStatus(newStatus);
            examAccountRepo.save(account);

            UserLogin userLogin = userLoginRepo.findById(account.getRandomUserLoginId()).orElse(null);
            if (userLogin != null) {
                userLogin.setEnabled(enabled);
                userLoginRepo.save(userLogin);
            }

            return ApiResponse.of(
                SuccessCode.ACCOUNT_STATUS_UPDATED,
                "Account " + (enabled ? "enabled" : "disabled") + " successfully");
        } else {
            return ApiResponse.of(ErrorCode.ACCOUNT_NOT_GENERATED);
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> generateAccountForSingle(String userId, UUID examClassId, UUID accountId) {
        ExamAccount account = examClassAuthorizationUtils.checkUserIsCreatorAndAccountExists(
            userId,
            examClassId,
            accountId);

        if (!StringUtils.isBlank(account.getRandomUserLoginId())) {
            return ApiResponse.of(ErrorCode.ACCOUNT_ALREADY_GENERATED);
        }

        String username;
        String password;

        UserRepresentation user = new UserRepresentation();
        user.setFirstName(account.getFullname());
        user.setEnabled(false);

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("oneTimeAccount", List.of("true"));
        attributes.put("mail", List.of(account.getRealUserLoginId()));
        attributes.put("studentCode", List.of(account.getStudentCode()));
        user.setAttributes(attributes);

        int maxRetries = 3;
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                username = "exam-" + generateAlphaNumericRandomString(6, ONLY_LOWER);
                password = generateRandomStringWithSpecialChars(12);
                user.setUsername(username);

                if (keycloakService.createUser(user, password)) {
                    account.setRandomUserLoginId(username);
                    account.setPassword(password);
                    account.setStatus(ExamClass.STATUS_DISABLE);

                    // Create UserLogin record
                    UserLogin userLogin = new UserLogin();
                    userLogin.setUserLoginId(username);
                    userLogin.setEmail(account.getRealUserLoginId());
                    userLogin.setEnabled(false);
                    userLogin.setFirstName(account.getFullname());

                    userLoginRepo.save(userLogin);
                    examAccountRepo.save(account);

                    return ApiResponse.of(SuccessCode.ACCOUNT_GENERATED_SUCCESS);
                } else {
                    attempt++;
                }
            } catch (Exception ex) {
                attempt++;
                log.error("Error creating user, retry attempt {}: {}", attempt, ex.getMessage());
            }
        }

        return ApiResponse.of(ErrorCode.ACCOUNT_GENERATION_FAILED);
    }

    @Override
    public List<ExamAccount> importAccountsFromExcel(
        UUID examClassId,
        MultipartFile file
    ) throws IOException {
        List<ExamClassAccountDTO> users = new ArrayList<>();

        try (InputStream is = file.getInputStream()) {
            XSSFWorkbook wb = new XSSFWorkbook(is);
            XSSFSheet sheet = wb.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            for (int i = 1; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                int columns = row.getLastCellNum();

                String email = null;
                String studentCode = null;
                DataFormatter formatter = new DataFormatter();

                if (columns > 0) {
                    Cell c = row.getCell(0);
                    if (c != null) {
                        email = formatter.formatCellValue(c).trim();
                    }
                }
                if (columns > 1) {
                    Cell c = row.getCell(1);
                    if (c != null) {
                        if (c.getCellType() == CellType.NUMERIC) {
                            studentCode = String.valueOf((long) c.getNumericCellValue());  // no decimal part
                        } else {
                            studentCode = formatter.formatCellValue(c).trim();
                        }
                    }
                }

                if (StringUtils.isAllBlank(email, studentCode)) {
                    continue;
                }

                String fullName = null;
                if (columns > 2) {
                    Cell c = row.getCell(2);
                    if (c != null) {
                        fullName = formatter.formatCellValue(c).trim();
                    }
                }

                ExamClassAccountDTO u = new ExamClassAccountDTO(email, studentCode, fullName);
                users.add(u);
            }
        }

        return importAccounts(examClassId, users);
    }

    private List<ExamAccount> importAccounts(UUID examClassId, List<ExamClassAccountDTO> users) {
        List<ExamAccount> examClass = new ArrayList<>();
        List<UserLogin> userLogins = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            ExamClassAccountDTO dto = users.get(i);
            String username = null;
            String password = null;

            UserRepresentation user = new UserRepresentation();

            user.setFirstName(dto.getFullName());
            user.setEnabled(false);

            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("oneTimeAccount", List.of("true"));
            attributes.put("mail", List.of(dto.getEmail())); // Cannot use 'email' as an attribute name in Keycloak
            attributes.put("studentCode", List.of(dto.getStudentCode()));
            user.setAttributes(attributes);

            int maxRetries = 3;
            int attempt = 0;
            while (attempt < maxRetries) {
                try {
                    username = "exam-" + generateAlphaNumericRandomString(6, ONLY_LOWER);
                    password = generateRandomStringWithSpecialChars(12);
                    user.setUsername(username);

                    if (keycloakService.createUser(user, password)) {
                        break;
                    } else {
                        attempt++;
                    }
                } catch (Exception e) {
                    attempt++;
                    log.error("Error creating user, retry attempt {}: {}", attempt, e.getMessage());
                }
            }

            ExamAccount ecu = new ExamAccount();

            ecu.setExamClassId(examClassId);
            ecu.setRealUserLoginId(dto.getEmail());
            ecu.setStudentCode(dto.getStudentCode());
            ecu.setFullname(dto.getFullName());
            ecu.setOrderIndex(i);

            if (attempt != maxRetries) {
                ecu.setRandomUserLoginId(username);
                ecu.setPassword(password);
                ecu.setStatus(ExamClass.STATUS_DISABLE);

                // Create UserLogin record
                UserLogin ul = new UserLogin();

                ul.setUserLoginId(username);
                ul.setEmail(dto.getEmail());
                ul.setEnabled(false);
                ul.setFirstName(dto.getFullName());

                userLogins.add(ul);
            }

            examClass.add(ecu);
        }

        userLoginRepo.saveAll(userLogins);
        examAccountRepo.saveAll(examClass);

        return examClass;
    }
}
