package com.hust.baseweb.applications.contentmanager.repo;

import com.hust.baseweb.applications.contentmanager.model.ContentModel;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MongoContentServiceImpl implements MongoContentService {

    GridFsOperations operations;

    public ObjectId storeFileToGridFs(ContentModel contentModel) throws IOException {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("upload_file_name", contentModel.getFile().getOriginalFilename());
        String uniqueFileName = UUID.randomUUID() + "_" + contentModel.getFile().getOriginalFilename();

        return operations.store(
            contentModel.getFile().getInputStream(),
            uniqueFileName,
            contentModel.getFile().getContentType(),
            metadata);
    }

    public GridFsResource getById(String id) {
        GridFSFile fID = operations.findOne(Query.query(Criteria.where("_id").is(id)));
        if (fID == null) {
            return null;
        }
        return operations.getResource(fID);
    }

    public void deleteFilesById(String id) {
        operations.delete(Query.query(Criteria.where("_id").is(id)));
    }

    @Override
    public List<String> storeFiles(MultipartFile[] files) {
        List<String> storageIds = new ArrayList<>(files.length);

        for (MultipartFile file : files) {
            ContentModel contentModel = new ContentModel(file.getOriginalFilename(), file);
            try {
                ObjectId id = storeFileToGridFs(contentModel);
                storageIds.add(id.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return storageIds;
    }

    @Override
    public String getOriginalFileName(GridFsResource content) {
        if (content == null) {
            return null;
        }

        try {
            // Try to get from metadata first
            String fileName = content.getGridFSFile().getMetadata().get("upload_file_name", String.class);
            if (!StringUtils.isBlank(fileName)) {
                return fileName;
            }
        } catch (Exception e) {
            log.warn("Cannot get upload_file_name from metadata: {}", e.getMessage());
        }

        // Fallback to filename from GridFS
        try {
            return content.getFilename();
        } catch (Exception e) {
            log.warn("Cannot get filename from GridFS: {}", e.getMessage());
            return null;
        }
    }
}
