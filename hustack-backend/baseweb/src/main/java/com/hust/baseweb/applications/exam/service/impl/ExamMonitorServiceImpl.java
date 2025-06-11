package com.hust.baseweb.applications.exam.service.impl;

import com.hust.baseweb.applications.exam.entity.ExamMonitorEntity;
import com.hust.baseweb.applications.exam.model.ResponseData;
import com.hust.baseweb.applications.exam.model.request.ExamMonitorSaveReq;
import com.hust.baseweb.applications.exam.repository.ExamMonitorRepository;
import com.hust.baseweb.applications.exam.service.ExamMonitorService;
import com.hust.baseweb.applications.exam.utils.DataUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExamMonitorServiceImpl implements ExamMonitorService {

    ExamMonitorRepository examMonitorRepository;
    ModelMapper modelMapper;

    @Override
    public List<ExamMonitorEntity> findAllByExamResultId(String examResultId) {
        return examMonitorRepository.findAllByExamResultId(examResultId);
    }

    @Override
    public ResponseData<ExamMonitorEntity> create(List<ExamMonitorSaveReq> list) {
        ResponseData<ExamMonitorEntity> responseData = new ResponseData<>();

        List<ExamMonitorEntity> listSave = list.stream()
                                               .map(req -> {
                                                   ExamMonitorEntity entity = modelMapper.map(req, ExamMonitorEntity.class);
                                                   entity.setStartTime(DataUtils.formatStringToLocalDateTime(req.getStartTime()));
                                                   entity.setToTime(DataUtils.formatStringToLocalDateTime(req.getToTime()));
                                                   return entity;
                                               })
                                               .collect(Collectors.toList());
        examMonitorRepository.saveAll(listSave);

        responseData.setHttpStatus(HttpStatus.OK);
        responseData.setResultCode(HttpStatus.OK.value());
        responseData.setResultMsg("Success");
        return responseData;
    }
}
