package com.hust.baseweb.applications.examclassandaccount.utils;

import com.hust.baseweb.applications.examclassandaccount.entity.ExamAccount;
import com.hust.baseweb.applications.examclassandaccount.entity.ExamClass;
import com.hust.baseweb.applications.examclassandaccount.repo.ExamAccountRepo;
import com.hust.baseweb.applications.examclassandaccount.repo.ExamClassRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Utility class for handling common authorization and existence checking patterns
 * related to exam classes and accounts.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamClassAuthorizationUtils {

    ExamClassRepo examClassRepo;

    ExamAccountRepo examAccountRepo;

    /**
     * Checks if an exam class exists and returns it.
     *
     * @param examClassId ID of the exam class to check
     * @return The exam class if it exists
     * @throws EntityNotFoundException if the exam class does not exist
     */
    public ExamClass checkExamClassExists(UUID examClassId) {
        return examClassRepo.findById(examClassId)
                            .orElseThrow(() -> new EntityNotFoundException("Exam class with id " +
                                                                           examClassId +
                                                                           " not found"));
    }

    /**
     * Checks if a user is the creator of an exam class.
     *
     * @param userId      ID of the user to check
     * @param examClassId ID of the exam class to check
     * @return The exam class if the user is the creator
     * @throws EntityNotFoundException if the exam class does not exist
     * @throws AccessDeniedException   if the user is not the creator
     */
    public ExamClass checkUserIsExamClassCreator(String userId, UUID examClassId) {
        ExamClass examClass = checkExamClassExists(examClassId);

        if (!examClass.getCreatedBy().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to perform this action on this exam class");
        }

        return examClass;
    }

    /**
     * Checks if an account exists in an exam class and returns it.
     *
     * @param examClassId ID of the exam class
     * @param accountId   ID of the account to check
     * @return The account map if it exists
     * @throws EntityNotFoundException if the account does not exist in the exam class
     */
    public ExamAccount checkAccountExistsInExamClass(UUID examClassId, UUID accountId) {
        ExamAccount account = examAccountRepo
            .findByExamClassIdAndId(examClassId, accountId);

        if (account == null) {
            throw new EntityNotFoundException("Account with id " +
                                              accountId +
                                              " not found in exam class " +
                                              examClassId);
        }

        return account;
    }

    /**
     * Performs both checks: if a user is the creator of an exam class and if an account exists in that class.
     *
     * @param userId      ID of the user to check
     * @param examClassId ID of the exam class to check
     * @param accountId   ID of the account to check
     * @return The account map if all checks pass
     * @throws EntityNotFoundException if the exam class or account does not exist
     * @throws AccessDeniedException   if the user is not the creator
     */
    public ExamAccount checkUserIsCreatorAndAccountExists(
        String userId, UUID examClassId, UUID accountId) {
        checkUserIsExamClassCreator(userId, examClassId);
        return checkAccountExistsInExamClass(examClassId, accountId);
    }
}
