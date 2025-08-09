package com.hust.baseweb.applications.exam.controller;

import com.hust.baseweb.applications.exam.service.MongoFileService;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(
    prefix = "feature",
    name = "enable-non-programming-contest-modules",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
@RestController
@RequestMapping("/service")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MongoFileController {

    MongoFileService mongoFileService;

    @GetMapping("/files/{fileId}/{fileName}")
    public ResponseEntity<byte[]> filter(@PathVariable String fileId, @PathVariable String fileName) {
        GridFSFile file = mongoFileService.getFileMetadata(fileId);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] fileData = mongoFileService.getFileData(file);
        return ResponseEntity.ok()
                             .contentType(MediaType.parseMediaType(file.getMetadata().getString("_contentType")))
                             .body(fileData);
    }
}
