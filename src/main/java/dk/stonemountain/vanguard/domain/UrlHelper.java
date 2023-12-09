package dk.stonemountain.vanguard.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import dk.stonemountain.vanguard.domain.RouteManager.Service;
import dk.stonemountain.vanguard.domain.RouteManager.ServiceEndpoint;
import dk.stonemountain.vanguard.util.ApplicationException;
import jakarta.ws.rs.core.UriInfo;

public class UrlHelper {
    public URI constructURL(Service service, ServiceEndpoint endpoint, UriInfo uriInfo) {
        var protocol = endpoint.isSsl() ? "https" : "http";
        var uri = uriInfo.getPath();

        var parameters = uriInfo.getQueryParameters().keySet().stream()
            .flatMap(k -> uriInfo.getQueryParameters().get(k).stream().map(v -> new Pair(k,v)))
            .map(p -> p.key() + "=" + URLEncoder.encode(p.value(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));

        // requestUri is the complete url. New to find a way to construct the uri and remember the query parameters
        var url = protocol + "://" + service.host() + ":" + service.port() + uri + (parameters.isBlank() ? "" : "?" + parameters);
        
        try {
            return new URI(url);
        } catch(URISyntaxException e) {
            throw new URLConstructionFailure(url, e);
        }
    }

    public static class URLConstructionFailure extends ApplicationException {
        private final String url;

        public URLConstructionFailure(String url, URISyntaxException e) {
            super("Failed to construct url " + url, e);
            this.url = url;
        }
    
        public String getUrl() {
            return url;
        }
    }

    public static record Pair(String key, String value) {
    }
}
