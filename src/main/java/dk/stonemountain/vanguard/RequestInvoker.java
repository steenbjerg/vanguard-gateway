package dk.stonemountain.vanguard;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.stonemountain.vanguard.domain.RouteManager.Method;
import dk.stonemountain.vanguard.domain.RouteManager.ServiceEndpoint;
import dk.stonemountain.vanguard.domain.UrlHelper;
import dk.stonemountain.vanguard.util.ApplicationException;
import dk.stonemountain.vanguard.util.Log;
import dk.stonemountain.vanguard.util.Log.LogType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RequestInvoker {
    private static final Logger log = LoggerFactory.getLogger(RequestInvoker.class);

    private static final TrustManager TRUSTING_MANAGER = new X509ExtendedTrustManager() {
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1, Socket arg2) throws CertificateException {
        }

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1, Socket arg2) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1, SSLEngine arg2) throws CertificateException {
        }
    };

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
            return invoke(endpoint.method(), url, endpoint.connectionTimeout(), request);

        } catch (Exception e) { // NOSONAR
            throw new RequestException(url, headers, e);
        }
    }

    private HttpResponse<InputStream> invoke(Method method, URI url, Duration readTimeout, HttpRequest request) throws InterruptedException, IOException, NoSuchAlgorithmException, KeyManagementException {
        HttpResponse<InputStream> response = null;
        var startTime = LocalDateTime.now();
        try {
            requestLog.info("Invoking request at {}. Url: {}. Method: {}, Headers: {}", startTime, url, method, request.headers().map());                
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{TRUSTING_MANAGER}, new SecureRandom());
            response = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(readTimeout)
                .sslContext(sslContext)
                .build()
                .send(request, BodyHandlers.ofInputStream());
        } catch (InterruptedException | IOException e) { // NOSONAR
            var endTime = LocalDateTime.now();
            requestLog.info("Invoked request completed at {}. Duration: {}. Failure: {} - {}", endTime, Duration.between(endTime, startTime), e.getClass().getName(), e.getMessage());
            throw e;
        } catch (Exception e) { // NOSONAR
            var endTime = LocalDateTime.now();
            requestLog.info("Invoked request completed at {}. Duration: {}. Failure: {} - {}", endTime, Duration.between(endTime, startTime), e.getClass().getName(), e.getMessage());
            throw e;
        } finally {
            var endTime = LocalDateTime.now();
            if (response != null) {
                requestLog.info("Invoked request completed at {}. Duration: {}. Status: {}, Headers: {}", endTime, Duration.between(endTime, startTime), response.statusCode(), response.headers().map());
            }
        }
        return response;
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
