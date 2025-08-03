package com.hust.baseweb.applications.programmingcontest.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttachmentMetadata {

     String id;
     
     String fileName;

     byte[] content;

    public AttachmentMetadata(String id, String fileName) {
        this.id = id;
        this.fileName = fileName;
    }
}
