package com.hust.baseweb.applications.education.thesisdefensejury.controller;


import com.hust.baseweb.applications.education.thesisdefensejury.entity.TraningProgram;
import com.hust.baseweb.applications.education.thesisdefensejury.models.Response;
import com.hust.baseweb.applications.education.thesisdefensejury.models.TranningProgramIM;
import com.hust.baseweb.applications.education.thesisdefensejury.service.TranningProgramService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ConditionalOnProperty(
    prefix = "feature",
    name = "enable-non-programming-contest-modules",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
@RestController
@Validated
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TraningProgramController {

    TranningProgramService tranningProgramService;

    @GetMapping("/program_tranings")
    public ResponseEntity<?> getAllTranningProgram(Pageable pageable) {
        try {
            List<TraningProgram> tp;
            tp = tranningProgramService.getAllTranningProgram();

            return new ResponseEntity<>(tp, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/tranning_program")
    public ResponseEntity<?> createTranningProgram(
        @RequestBody TranningProgramIM request
    ) {
        Response res = new Response();
        // TODO: check valid request
        if (request == null || request.getName() == "") {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Invalid body request or invalid tranning program name");
        }

        res = tranningProgramService.createTranningProgram(request);

        return ResponseEntity.status(HttpStatus.OK).body(res);
    }
}
