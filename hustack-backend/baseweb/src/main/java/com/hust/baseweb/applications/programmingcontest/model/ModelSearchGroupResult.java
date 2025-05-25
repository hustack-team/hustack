package com.hust.baseweb.applications.programmingcontest.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ModelSearchGroupResult {
    private String id;
    private String name;
    private List<String> users;

}
