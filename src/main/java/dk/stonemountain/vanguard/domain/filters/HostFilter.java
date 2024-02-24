package dk.stonemountain.vanguard.domain.filters;

import dk.stonemountain.vanguard.domain.FilterManager.SecurityCheckFailed;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
public class HostFilter implements PreRequestFilter {
    @Override
    public void preRequestFilter(HttpServerRequest request, UriInfo info) {
        var hostAndPort = request.authority();

    	if (hostAndPort.host() == null || hostAndPort.host().length() == 0) {
            throw new SecurityCheckFailed("No host specified", request.method().name(), request.absoluteURI());
    	}
    	
    	if (!(hostAndPort.host().endsWith("admin.my-keycloak.org") || hostAndPort.host().endsWith("my-keycloak.org") || hostAndPort.host().endsWith("stonemountain.dk") || (hostAndPort.host().equals("localhost") && hostAndPort.port() == 9443) || (hostAndPort.host().equals("mars") && hostAndPort.port() == 9443))) {
            throw new SecurityCheckFailed("Not expected host name", request.method().name(), request.absoluteURI());
    	}

        if (hostAndPort.host().equals("auth.stonemountain.dk")) {
            var remote = request.remoteAddress();
    		if (!"192.168.86.1".equals(remote.hostAddress()) && !"188.114.172.95".equals(remote.hostAddress()) && !"testwifi.here".equals(remote.hostAddress())) {
    			if (!(info.getPath().startsWith("/realms/house") || info.getPath().startsWith("/realms/demo") || info.getPath().startsWith("/resources/"))) {
                    throw new SecurityCheckFailed("Not expected call to authentication server", request.method().name(), request.absoluteURI());
    			}
    		}
    	}
    }
}
