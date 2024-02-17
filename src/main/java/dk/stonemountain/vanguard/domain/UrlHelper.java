package dk.stonemountain.vanguard.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.stonemountain.vanguard.domain.RouteManager.Service;
import dk.stonemountain.vanguard.domain.RouteManager.ServiceEndpoint;
import dk.stonemountain.vanguard.util.ApplicationException;
import jakarta.ws.rs.core.UriInfo;

public class UrlHelper {
    private static final Logger log = LoggerFactory.getLogger(UrlHelper.class);

    public URI constructURL(Service service, ServiceEndpoint endpoint, UriInfo uriInfo) {
        var protocol = endpoint.isSsl() ? "https" : "http";
        var path = uriInfo.getPath().replace(" ", "%20");
        var parameters = uriInfo.getQueryParameters().keySet().stream()
            .flatMap(k -> uriInfo.getQueryParameters().get(k).stream().map(v -> new Pair(k,v)))
            .map(p -> p.key() + "=" + URLEncoder.encode(p.value(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));

        // requestUri is the complete url. New to find a way to construct the uri and remember the query parameters
        var url = protocol + "://" + service.host() + ":" + service.port() + path + (parameters.isBlank() ? "" : "?" + parameters);
        try {
            var newUri = new URI(url);
            return newUri;
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
