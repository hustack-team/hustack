package com.hust.baseweb.applications.programmingcontest.callexternalapi.service;

import com.hust.baseweb.applications.programmingcontest.callexternalapi.config.ClientCredential;
import com.hust.baseweb.applications.programmingcontest.callexternalapi.model.LmsLogModelCreate;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
//import openerp.openerpresourceserver.config.ClientCredential;

@Service
@Log4j2
public class ApiService {
  private final WebClient webClient;
  private final KeycloakService keycloakService;
  private ClientCredential clientCredential;

  public ApiService(WebClient.Builder webClientBuilder, KeycloakService keycloakService,
      ClientCredential clientCredential) {
    // TODO: remove hard-coded URL
    //this.webClient = webClientBuilder.baseUrl("http://localhost:8081/api").build();
      //this.webClient = webClientBuilder.baseUrl("http://localhost:9090/api").build();
      this.webClient = webClientBuilder.baseUrl("https://analytics.soict.ai").build();

      this.keycloakService = keycloakService;
    this.clientCredential = clientCredential;
  }

  public <T> ResponseEntity<T> callApi(String endpoint, Class<T> responseType) {
    if (clientCredential == null) {
      throw new RuntimeException("Client credential is unset");
    }

    log.debug("Calling API with credential: {}, endpoint: {}", clientCredential, endpoint);
    String accessToken = keycloakService.getAccessToken(clientCredential.getClientId(),
        clientCredential.getClientSecret());
    log.debug("Get access token: " + accessToken);

    try {
        return this.webClient.get()
                             .uri(endpoint)
                             .header("Authorization", "Bearer " + accessToken)
                             .retrieve()
                             .toEntity(responseType)
                             // .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                             // .filter(RuntimeException.class::isInstance))
                             .block();
    }catch (Exception e){
        e.printStackTrace();
    }
    return null;
  }
    public <T, B> ResponseEntity<T> callPostApi(String endpoint, Class<T> responseType, B body) {
        if (clientCredential == null) {
            throw new RuntimeException("Client credential is unset");
        }
        log.debug("Calling API with credential: {}, endpoint: {}", clientCredential, endpoint);
        String accessToken = keycloakService.getAccessToken(clientCredential.getClientId(),
                                                            clientCredential.getClientSecret());
        log.debug("Get access token: " + accessToken);

        try {
            return this.webClient.post()
                                 .uri(endpoint)
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .header("Authorization", "Bearer " + accessToken)
                                 .body(BodyInserters.fromValue(body))
                                 .retrieve()
                                 .toEntity(responseType)
                                 // .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                                 // .filter(RuntimeException.class::isInstance))
                                 .block();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

  public ResponseEntity<?> callLogAPI(String url, LmsLogModelCreate model){
      if (clientCredential == null) {
          throw new RuntimeException("Client credential is unset");
      }

      log.debug("Calling API with credential: {}, endpoint: {}", clientCredential.getClientId() + "," + clientCredential.getClientSecret(), url);
      String accessToken = keycloakService.getAccessToken(clientCredential.getClientId(),
                                                          clientCredential.getClientSecret());
      log.debug("Get access token: " + accessToken);

      try {
          return this.webClient.post()
                               .uri(url)
                               .contentType(MediaType.APPLICATION_JSON)
                               .header("Authorization", "Bearer " + accessToken)
                               .body(BodyInserters.fromValue(model))
                               .retrieve()

                               // Xử lý lỗi 401
                               .onStatus(
                                   status -> status.value() == 401, response ->
                                       response.bodyToMono(String.class).flatMap(body -> {
                                           log.error("Unauthorized (401): {}", body);
                                           return Mono.error(new RuntimeException("Unauthorized: " + body));
                                       })
                               )

                               // Xử lý lỗi 403
                               .onStatus(
                                   status -> status.value() == 403, response ->
                                       response.bodyToMono(String.class).flatMap(body -> {
                                           log.error("Forbidden (403): {}", body);
                                           return Mono.error(new RuntimeException("Forbidden: " + body));
                                       })
                               )

                               // Xử lý lỗi 404
                               .onStatus(
                                   status -> status.value() == 404, response ->
                                       response.bodyToMono(String.class).flatMap(body -> {
                                           log.error("Not Found (404): {}", body);
                                           return Mono.error(new RuntimeException("Not Found: " + body));
                                       })
                               )

                               // Xử lý lỗi 422
                               .onStatus(
                                   status -> status.value() == 422, response ->
                                       response.bodyToMono(String.class).flatMap(body -> {
                                           log.error("Unprocessable Entity (422): {}", body);
                                           return Mono.error(new RuntimeException("Unprocessable Entity: " + body));
                                       })
                               )

                               // Xử lý lỗi 5xx khác
                               .onStatus(
                                   HttpStatusCode::is5xxServerError, response ->
                                       response.bodyToMono(String.class).flatMap(body -> {
                                           log.error("Server error (5xx): {}", body);
                                           return Mono.error(new RuntimeException("Server error: " + body));
                                       })
                               )

                               .toEntity(Void.class)

                               // Timeout 2 giây
                               .timeout(Duration.ofSeconds(2))

                               // Retry khi bị timeout hoặc lỗi 5xx
                               .retryWhen(
                                   Retry.fixedDelay(3, Duration.ofSeconds(1))
                                        .filter(throwable ->
                                                    throwable instanceof TimeoutException ||
                                                    throwable instanceof ConnectException ||
                                                    throwable instanceof SocketTimeoutException ||
                                                    (throwable instanceof WebClientResponseException wcre &&
                                                     wcre.getStatusCode().is5xxServerError())
                                        )
                               )
                               .block();
      } catch (Exception e) {
          // Xử lý lỗi chung sau khi hết retry hoặc lỗi client (4xx)
          log.error("Failed to send event: {} - {}", e.getClass().getSimpleName(), e.getMessage());
      }
       return null;

      /*
      this.webClient.post()
                           //.uri(url + "/log/create-log")
                    //.uri("/log/create-log")
                    .uri(url)
          .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(BodyInserters.fromValue(model))
                           .header("Authorization", "Bearer " + accessToken)
                           .retrieve()
          .toEntity(LmsLogModelCreate.class)
          .subscribe(
            responseEntity ->{
                HttpStatus status = responseEntity.getStatusCode();
                // Handle success response here
                //HttpStatusCode status = responseEntity.getStatusCode();
                //URI location = responseEntity.getHeaders().getLocation();
                //Employee createdEmployee = responseEntity.getBody();    // Response body
                // handle response as necessary
                log.info("callLogAPI -> OK!!!");
            },
            error -> {
                //HttpStatus status = responseEntity.getStatusCode();
                log.info("callLogAPI -> ERROR ??? status = " + error.getMessage());
            }
          );


      return ResponseEntity.ok().body("OK");
      */
  }
  public <T> ResponseEntity<T> callApi(String endpoint, ParameterizedTypeReference<T> responseType) {
    if (clientCredential == null) {
      throw new RuntimeException("Client credential is unset");
    }

    log.debug("Calling API with credential: {}, endpoint: {}", clientCredential, endpoint);
    String accessToken = keycloakService.getAccessToken(clientCredential.getClientId(),
        clientCredential.getClientSecret());
    log.debug("Get access token: " + accessToken);

    try {
        return this.webClient.get()
                             .uri(endpoint)
                             .header("Authorization", "Bearer " + accessToken)
                             .retrieve()
                             .toEntity(responseType)
                             // .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                             // .filter(RuntimeException.class::isInstance))
                             .block();
    }catch (Exception e){
        e.printStackTrace();
    }
    return null;
  }

  public void setCredential(ClientCredential clientCredential) {
    this.clientCredential = clientCredential;
  }

  public void setCredential(String clientId, String clientSecret) {
    this.clientCredential = new ClientCredential(clientId, clientSecret);
  }
}
