package com.hust.baseweb.applications.programmingcontest.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@ToString
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockCode {

    UUID id;

    String code;

    int forStudent;

    int seq;

    String language;
}
