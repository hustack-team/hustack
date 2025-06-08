package com.hust.baseweb.applications.examclassandaccount.service;

import com.hust.baseweb.applications.examclassandaccount.entity.ExamClass;
import com.hust.baseweb.applications.examclassandaccount.entity.ExamClassUserloginMap;
import com.hust.baseweb.applications.examclassandaccount.model.ModelCreateExamClass;
import com.hust.baseweb.applications.examclassandaccount.model.ModelRepsonseExamClassDetail;
import com.hust.baseweb.applications.examclassandaccount.repo.ExamClassRepo;
import com.hust.baseweb.applications.examclassandaccount.repo.ExamClassUserloginMapRepo;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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

    @Override
    public List<ExamClass> getAllExamClass() {
        List<ExamClass> res = examClassRepo.findAll();
        return res;
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
    public Object deleteAccounts(UUID examClassId) {
        Map<String, Object> response = (Map<String, Object>) updateStatus(examClassId, false);
        int count = examClassUserloginMapRepo.deleteByExamClassIdAndStatusIsNot(examClassId, ExamClass.STATUS_ACTIVE);
        response.put("success", count);
        return response;
    }

    @Override
    public ModelRepsonseExamClassDetail getExamClassDetail(UUID examClassId) {
        ExamClass ec = examClassRepo
            .findById(examClassId)
            .orElseThrow(() -> new EntityNotFoundException("Exam class with ID " + examClassId + " not found"));
        ModelRepsonseExamClassDetail examClassDetail = new ModelRepsonseExamClassDetail();

        examClassDetail.setExamClassId(ec.getId());
        examClassDetail.setName(ec.getName());
        examClassDetail.setDescription(ec.getDescription());
        examClassDetail.setExecuteDate(ec.getExecuteDate());
        examClassDetail.setAccounts(examClassUserloginMapService.getExamClassUserloginMap(examClassId));

        return examClassDetail;
    }

    /**
     * @param examClassId
     * @return
     */
    @Override
    public byte[] exportExamClass(UUID examClassId) {
        List<ExamClassUserloginMap> accounts = getExamClassDetail(examClassId).getAccounts();
        return exportPdf(accounts, "reports/exam-class-account_report.jasper", new HashMap<>());
    }

    @Override
    public Object resetPassword(UUID examClassId) {
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
                    keycloakService.resetPassword(e.getRandomUserLoginId(), newPassword);
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
    public Object generateAccounts(UUID examClassId) {
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
                user.setEnabled(true);

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
                        username = "exam-" + generateAlphaNumericRandomString(6);
                        password = generateRandomStringWithSpecialChars(12);
                        user.setUsername(username);

                        if (keycloakService.createUser(user, password)) {
                            ecu.setRandomUserLoginId(username);
                            ecu.setPassword(password);
                            ecu.setStatus(ExamClassUserloginMap.STATUS_ACTIVE);

                            updateSuccessful.add(ecu);

                            //
                            UserLogin ul = new UserLogin();

                            ul.setUserLoginId(username);
                            ul.setEmail(ecu.getRealUserLoginId());
                            ul.setEnabled(true);
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
    public Object updateStatus(UUID examClassId, boolean enabled) {
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
                    keycloakService.updateEnabledUser(e.getRandomUserLoginId(), enabled);
                    e.setStatus(newStatus);
                    updateSuccessful.add(e);
                    success++;
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
}
