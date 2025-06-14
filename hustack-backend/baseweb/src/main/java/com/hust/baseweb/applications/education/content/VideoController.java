//package com.hust.baseweb.applications.education.content;
//
//import internal.org.springframework.content.rest.annotations.ContentRestController;
//import internal.org.springframework.content.rest.contentservice.ContentService;
//import internal.org.springframework.content.rest.contentservice.ContentServiceFactory;
//import internal.org.springframework.content.rest.controllers.MethodNotAllowedException;
//import internal.org.springframework.content.rest.controllers.ResourceNotFoundException;
//import internal.org.springframework.content.rest.io.StoreResource;
//import internal.org.springframework.content.rest.mappingcontext.ContentPropertyToExportedContext;
//import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;
//import jakarta.activation.MimetypesFileTypeMap;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.beans.BeansException;
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.content.commons.mappingcontext.MappingContext;
//import org.springframework.content.commons.storeservice.Stores;
//import org.springframework.content.rest.config.RestConfiguration;
//import org.springframework.context.ApplicationContext;
//import org.springframework.data.repository.support.DefaultRepositoryInvokerFactory;
//import org.springframework.data.repository.support.Repositories;
//import org.springframework.data.repository.support.RepositoryInvokerFactory;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.RequestHeader;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.context.request.ServletWebRequest;
//
//import java.io.IOException;
//import java.nio.file.FileSystemNotFoundException;
//@ContentRestController
//public class VideoController implements InitializingBean {
//
//    @Autowired
//    ApplicationContext context;
//
//    @Autowired(required = false)
//    private Repositories repositories;
//
//    @Autowired
//    private Stores stores;
//
//    @Autowired(required = false)
//    private RepositoryInvokerFactory repoInvokerFactory;
//    @Autowired
//    private RestConfiguration config;
//
//    @Autowired
//    private StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler;
//
//    @Autowired
//    private MappingContext mappingContext;
//
//    @Autowired
//    private ContentPropertyToExportedContext exportedMappingContext;
//
//    private ContentServiceFactory contentServiceFactory;
//
//    public VideoController() {
//    }
//
//    @RequestMapping(value = {"/videos/**"}, method = {RequestMethod.GET})
//    public void getContent(
//        HttpServletRequest request,
//        HttpServletResponse response,
//        @RequestHeader HttpHeaders headers,
//        StoreResource storeResource
//    ) throws MethodNotAllowedException {
//        if (storeResource != null && storeResource.exists()) {
//            if (!(new ServletWebRequest(request, response)).checkNotModified(
//                storeResource.getETag() != null
//                    ? storeResource.getETag().toString()
//                    : null,
//                resolveLastModified(storeResource))) {
//                ContentService contentService = this.contentServiceFactory.getContentService(storeResource);
//                contentService.getContent(request, response, headers, storeResource, getMimeType(storeResource));
//            }
//        } else {
//            throw new ResourceNotFoundException();
//        }
//    }
//
//    private MediaType getMimeType(StoreResource storeResource) {
//        String mimeType = (new MimetypesFileTypeMap()).getContentType(storeResource.getFilename());
//        return MediaType.valueOf(mimeType != null ? mimeType : "");
//    }
//
//    protected static long resolveLastModified(StoreResource storeResource) {
//        long lastModified = -1L;
//
//        try {
//            lastModified = storeResource.lastModified();
//        } catch (FileSystemNotFoundException var4) {
//        } catch (IOException var5) {
//        }
//
//        return lastModified;
//    }
//
//    public void afterPropertiesSet() throws Exception {
//        try {
//            this.repositories = (Repositories) this.context.getBean(Repositories.class);
//        } catch (BeansException var2) {
//            this.repositories = new Repositories(this.context);
//        }
//
//        if (this.repoInvokerFactory == null) {
//            this.repoInvokerFactory = new DefaultRepositoryInvokerFactory(this.repositories);
//        }
//
//        this.contentServiceFactory = new ContentServiceFactory(
//            this.config,
//            this.repositories,
//            this.repoInvokerFactory,
//            this.stores,
//            this.mappingContext,
//            this.exportedMappingContext,
//            this.byteRangeRestRequestHandler);
//    }
//}
//
