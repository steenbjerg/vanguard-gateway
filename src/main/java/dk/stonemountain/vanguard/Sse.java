package dk.stonemountain.vanguard;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public class Sse {
    public HttpResponse<InputStream> invoke(URI url) {
        try {
            // Construct request
            var reqBuilder = HttpRequest.newBuilder()
                .uri(url)
                .version(Version.HTTP_2)
                .timeout(Duration.ofSeconds(5))
                .GET();

            var request = reqBuilder.build();
            
            // Make invocation
            return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(15))
                .build()
                .send(request, BodyHandlers.ofInputStream());
        } catch (Exception e) { // NOSONAR
            throw new RuntimeException("failed", e);
        }
    }

    private void test() throws URISyntaxException, IOException {
        HttpResponse<InputStream> response = invoke(new URI("http://localhost:8280/sse-test"));
        try (var is = response.body()) {
            is.transferTo(System.out);
        }
    }

    public static void main(String[] args) {
        try {
            new Sse().test();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
