package com.hust.baseweb.applications.exam.controller;

import com.hust.baseweb.applications.exam.service.MongoFileService;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.core.io.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/service")
@RequiredArgsConstructor
public class MongoFileController {

    private final MongoFileService mongoFileService;

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
