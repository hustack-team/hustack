package com.hust.baseweb.applications.examclassandaccount.service;

import com.hust.baseweb.applications.examclassandaccount.entity.ExamClass;
import com.hust.baseweb.applications.examclassandaccount.entity.ExamClassUserloginMap;
import com.hust.baseweb.applications.examclassandaccount.model.ModelCreateExamClass;
import com.hust.baseweb.applications.examclassandaccount.model.ModelRepsonseExamClassDetail;
import com.hust.baseweb.applications.examclassandaccount.repo.ExamClassRepo;
import com.hust.baseweb.applications.examclassandaccount.repo.ExamClassUserloginMapRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.hust.baseweb.utils.PdfUtils.exportPdf;

@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
@Service
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamClassServiceImpl implements ExamClassService {

    ExamClassRepo examClassRepo;

    ExamClassUserloginMapRepo examClassUserloginMapRepo;

    ExamClassUserloginMapService examClassUserloginMapService;

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
    public boolean updateStatusExamClass(UUID examClassId, String status) {
        ExamClass ec = examClassRepo.findById(examClassId).orElse(null);
        if (ec == null) {
            return false;
        }
        if (ec.getStatus().equals(status)) {
            return false;
        }

        ec.setStatus(status);
        List<ExamClassUserloginMap> lst = examClassUserloginMapRepo.findByExamClassId(examClassId);
        for (ExamClassUserloginMap e : lst) {// reverse status
            if (e.getStatus().equals(ExamClassUserloginMap.STATUS_DISABLE)) {
                e.setStatus(ExamClassUserloginMap.STATUS_ACTIVE);
            } else {
                e.setStatus(ExamClassUserloginMap.STATUS_DISABLE);
            }
            e = examClassUserloginMapRepo.save(e);
        }
        return true;
    }

    @Override
    public boolean clearAccountExamClass(UUID examClassId) {
        List<ExamClassUserloginMap> L = examClassUserloginMapRepo.findByExamClassId(examClassId);
        for (ExamClassUserloginMap e : L) {
            examClassUserloginMapRepo.delete(e);
            log.info("clearAccountExamClass, remove " + e.getId() + "," + e.getRealUserLoginId());
        }
        return L != null && L.size() > 0;
    }

    @Override
    public ModelRepsonseExamClassDetail getExamClassDetail(UUID examClassId) {
        ExamClass ec = examClassRepo
            .findById(examClassId)
            .orElseThrow(() -> new EntityNotFoundException("Exam class with ID " + examClassId + " not found"));
        ModelRepsonseExamClassDetail m = new ModelRepsonseExamClassDetail();
        m.setExamClassId(ec.getId());
        m.setName(ec.getName());
        m.setDescription(ec.getDescription());
        m.setExecuteDate(ec.getExecuteDate());
        m.setStatus(ec.getStatus());
        m.setStatusList(ExamClass.getStatusList());
        List<ExamClassUserloginMap> accounts = examClassUserloginMapService.getExamClassUserloginMap(examClassId);
        m.setAccounts(accounts);
        return m;
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
}
