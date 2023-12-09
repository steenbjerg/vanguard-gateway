package dk.stonemountain.vanguard.domain.filters;

import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
public interface PreRequestFilter {
    void preRequestFilter(HttpServerRequest request, UriInfo info);
}
