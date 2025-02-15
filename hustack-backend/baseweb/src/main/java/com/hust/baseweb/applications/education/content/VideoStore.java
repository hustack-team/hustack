package com.hust.baseweb.applications.education.content;

import org.springframework.content.commons.repository.Store;
import org.springframework.content.rest.StoreRestResource;

import java.util.UUID;

@StoreRestResource
public interface VideoStore extends Store<UUID> {

}
