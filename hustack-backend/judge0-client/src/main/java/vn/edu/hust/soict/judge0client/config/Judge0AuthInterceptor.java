package vn.edu.hust.soict.judge0client.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@AllArgsConstructor(onConstructor_ = {@Autowired})
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EnableConfigurationProperties(Judge0Config.class)
public class Judge0AuthInterceptor implements ClientHttpRequestInterceptor {

    Judge0Config judge0Config;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        Judge0Config.ServerConfig serverConfig = getServerConfig(request.getURI().toString());

        headers.add(serverConfig.getAuthn().getHeader(), serverConfig.getAuthn().getToken());
        headers.add(serverConfig.getAuthz().getHeader(), serverConfig.getAuthz().getToken());

        return execution.execute(request, body);
    }

    private Judge0Config.ServerConfig getServerConfig(String requestUri) {
        if (requestUri.startsWith(judge0Config.getSingleThreaded().getUri())) {
            return judge0Config.getSingleThreaded();
        } else if (requestUri.startsWith(judge0Config.getMultiThreaded().getUri())) {
            return judge0Config.getMultiThreaded();
        } else {
            throw new IllegalArgumentException("Unknown Judge0 server for request URI: " + requestUri);
        }
    }

}
