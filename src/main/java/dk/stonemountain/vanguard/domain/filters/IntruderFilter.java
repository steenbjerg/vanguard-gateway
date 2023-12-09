package dk.stonemountain.vanguard.domain.filters;

import dk.stonemountain.vanguard.domain.FilterManager.SecurityCheckFailed;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
public class IntruderFilter implements PreRequestFilter {

    @Override
    public void preRequestFilter(HttpServerRequest request, UriInfo info) {
        String url = request.absoluteURI();
       	if (url.contains(".php")) {
            throw new SecurityCheckFailed("None expected request", request.method().name(), request.absoluteURI());
    	}
    }
    
}
