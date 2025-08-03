package com.hust.baseweb.applications.education.recourselink.controller;

import com.hust.baseweb.applications.education.recourselink.entity.EducationResource;
import com.hust.baseweb.applications.education.recourselink.entity.EducationResourceDomain;
import com.hust.baseweb.applications.education.recourselink.service.EducationResourceDomainService;
import com.hust.baseweb.applications.education.recourselink.service.EducationResourceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


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
public class ResourceController {

    EducationResourceService educationResourceService;

    EducationResourceDomainService educationResourceDomainService;

    // Xem phan 9 trong README, co yeu cau ve dinh danh tai nguyen URI/URL
    //o day dung phuong thuc HTTP la GET nen URI chi can /resource la du de hieu
    // API nay de lay tat ca resource ve
    @GetMapping("/domains/{domainId}/resources")
    public ResponseEntity<?> getAllResources(
        Pageable pageable,
        @PathVariable("domainId") EducationResourceDomain domainId
    ) {
        return ResponseEntity.ok().body(educationResourceService.findByDomainId(domainId.getId(), pageable));
    }

    @GetMapping("/domains/{domainId}")
    public ResponseEntity<?> getDomain(@PathVariable("domainId") EducationResourceDomain domainId) {
        return ResponseEntity.ok().body(educationResourceDomainService.findById(domainId.getId()));
    }

    @GetMapping("/domains/{domainId}/resources/{resourceId}")
    public ResponseEntity<?> getResource(
        @PathVariable("domainId") EducationResourceDomain domainId,
        @PathVariable("resourceId") UUID resourceId
    ) {
        return ResponseEntity.ok().body(educationResourceService.findByIdAndDomainId(resourceId, domainId.getId()));
    }

    @GetMapping("/domains")
    public ResponseEntity<?> getAllDomains(Pageable pageable) {
        try {
            Map<String, Object> response = new HashMap<>();
            Page<EducationResourceDomain> pageDomain;
            pageDomain = educationResourceDomainService.findAll(pageable);
            response.put("Domains", pageDomain.getContent());
            response.put("currentPagge", pageDomain.getNumber());
            response.put("totalItems", pageDomain.getTotalElements());
            response.put("totalPages", pageDomain.getTotalPages());

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/domain")
    public ResponseEntity<?> createDomain(
        @RequestBody EducationResourceDomain request
    ) {
        EducationResourceDomain domain = new EducationResourceDomain(
            request.getName()
        );
        Boolean status = educationResourceDomainService.createDomain(domain);
        if (!status) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error Message");
        }
        return ResponseEntity.status(HttpStatus.OK).body(status);
    }

    @PostMapping("/domains/{domainId}/resource")
    public ResponseEntity<?> createResource(
        @RequestBody EducationResource request,
        @PathVariable("domainId") EducationResourceDomain domainId
    ) {

        Boolean status = educationResourceService.createResource(domainId.getId(), request);
        if (!status) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error Message");
        }
        return ResponseEntity.status(HttpStatus.OK).body(status);
    }

    @PostMapping("/domains/{domainId}/resources")
    public ResponseEntity<?> getResourceWithFilter(
        @RequestBody EducationResource request,
        @PathVariable("domainId") EducationResourceDomain domainId
    ) {


        return ResponseEntity
            .status(HttpStatus.OK)
            .body(educationResourceService.findResourceWithFilter(request, domainId.getId()));
    }


    @PatchMapping("/domain/{id}")
    public ResponseEntity<?> updateDomain(
        @RequestBody EducationResourceDomain request,
        @PathVariable("id") EducationResourceDomain domainId
    ) {
        EducationResourceDomain domain = new EducationResourceDomain(
            request.getName()
        );
        EducationResourceDomain res = educationResourceDomainService.updateDomain(domainId.getId(), domain);
        return ResponseEntity.ok().body(res);
    }

}
