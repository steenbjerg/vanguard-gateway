package dk.stonemountain.vanguard;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import dk.stonemountain.vanguard.domain.RouteManager.ServiceEndpoint;
import dk.stonemountain.vanguard.domain.UrlHelper;
import dk.stonemountain.vanguard.util.ApplicationException;
import dk.stonemountain.vanguard.util.Log;
import dk.stonemountain.vanguard.util.Log.LogType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RequestInvoker {
    @Inject
    @Log(LogType.REQUEST_LOG)
    Logger requestLog;

    public HttpResponse<InputStream> invokeAndReturn(ServiceEndpoint endpoint, URI url, Map<String, List<String>> headers, BodyPublisher bodyPublisher) {
        try {
            // Construct request
            var tmpBuilder = HttpRequest.newBuilder()
                .uri(url)
                .version(Version.HTTP_2)
                .timeout(endpoint.readTimeout());
            var reqBuilder = switch(endpoint.method()) {
                case GET -> tmpBuilder.GET();
                case POST -> tmpBuilder.POST(bodyPublisher);
                case PUT -> tmpBuilder.PUT(bodyPublisher);
                case DELETE -> tmpBuilder.DELETE();
            };

            headers.entrySet()
                .stream()
                .flatMap(e -> e.getValue().stream().map(v -> new UrlHelper.Pair(e.getKey(), v)))
                .forEach(p -> reqBuilder.setHeader(p.key(), p.value()));
            var request = reqBuilder.build();
            
            // Make invocation
            HttpResponse<InputStream> response = null;
            var startTime = LocalDateTime.now();
            try {
                requestLog.info("Invoking request at {}. Url: {}. Method: {}, Headers: {}", startTime, url, endpoint.method(), request.headers().map());                
                response = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .connectTimeout(endpoint.connectionTimeout())
                    .build()
                    .send(request, BodyHandlers.ofInputStream());
            } catch (Exception e) { // NOSONAR
                var endTime = LocalDateTime.now();
                requestLog.info("Invoked request completed at {}. Duration: {}. Failure: {}", endTime, Duration.between(endTime, startTime), e.getMessage());
                throw e;
            } finally {
                var endTime = LocalDateTime.now();
                if (response != null) {
                    requestLog.info("Invoked request completed at {}. Duration: {}. Headers: {}", endTime, Duration.between(endTime, startTime), response.headers().map());
                }
            }
            return response;

        } catch (Exception e) { // NOSONAR
            throw new RequestException(url, headers, e);
        }
    }

    public static class RequestException extends ApplicationException {
        private final URI url; 
        private final transient Map<String, List<String>> headers;

        public RequestException(URI url, Map<String, List<String>> headers, Exception e) {
            super(MessageFormat.format("request to url {0} failed: {1}", url, e.getMessage()), e);
            this.url = url;
            this.headers = headers;
        }

        public URI getUrl() {
            return url;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }
    }
}
