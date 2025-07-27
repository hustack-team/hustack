package com.hust.baseweb.applications.contentmanager.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContentModel {

    // TODO: consider removing
    String id;

    MultipartFile file;

}
