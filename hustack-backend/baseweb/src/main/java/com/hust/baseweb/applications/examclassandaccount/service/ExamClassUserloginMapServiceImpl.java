package com.hust.baseweb.applications.examclassandaccount.service;

import com.hust.baseweb.applications.examclassandaccount.entity.ExamClass;
import com.hust.baseweb.applications.examclassandaccount.entity.ExamClassUserloginMap;
import com.hust.baseweb.applications.examclassandaccount.model.ExamClassAccountDTO;
import com.hust.baseweb.applications.examclassandaccount.repo.ExamClassUserloginMapRepo;
import com.hust.baseweb.entity.UserLogin;
import com.hust.baseweb.repo.UserLoginRepo;
import com.hust.baseweb.service.KeycloakAdminService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.hust.baseweb.utils.CharGroupPolicy.ONLY_LOWER;
import static com.hust.baseweb.utils.RandomGenerator.generateAlphaNumericRandomString;
import static com.hust.baseweb.utils.RandomGenerator.generateRandomStringWithSpecialChars;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamClassUserloginMapServiceImpl implements ExamClassUserloginMapService {

    ExamClassUserloginMapRepo examClassUserloginMapRepo;

    UserLoginRepo userLoginRepo;

    KeycloakAdminService keycloakService;


    @Override
    @Transactional(readOnly = true)
    public List<ExamClassUserloginMap> findByExamClassId(UUID examClassId) {
        return examClassUserloginMapRepo.findByExamClassId(examClassId);

    }

    @Override
    public List<ExamClassUserloginMap> importAccounts(UUID examClassId, List<ExamClassAccountDTO> users) {
        List<ExamClassUserloginMap> examClass = new ArrayList<>();
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
            attributes.put("mail", List.of(dto.getEmail())); // Cannot use 'email' as an attribute name
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
                    log.error("Lỗi tạo user, thử lại lần {}: {}", attempt, e.getMessage());
                }
            }

            ExamClassUserloginMap ecu = new ExamClassUserloginMap();

            ecu.setExamClassId(examClassId);
            ecu.setRealUserLoginId(dto.getEmail());
            ecu.setStudentCode(dto.getStudentCode());
            ecu.setFullname(dto.getFullName());
            ecu.setOrderIndex(i);

            if (attempt != maxRetries) {
                ecu.setRandomUserLoginId(username);
                ecu.setPassword(password);
                ecu.setStatus(ExamClass.STATUS_DISABLE);

                //
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
        examClassUserloginMapRepo.saveAll(examClass);

        return examClass;
    }
}
