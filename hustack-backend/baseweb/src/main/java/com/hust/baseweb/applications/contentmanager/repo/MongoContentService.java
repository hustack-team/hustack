package com.hust.baseweb.applications.contentmanager.repo;

import com.hust.baseweb.applications.contentmanager.model.ContentModel;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface MongoContentService {

    ObjectId storeFileToGridFs(ContentModel contentModel) throws IOException;

    GridFsResource getById(String id);

    void deleteFilesById(String id);

    List<String> storeFiles(MultipartFile[] files);

    String getOriginalFileName(GridFsResource content);
}
