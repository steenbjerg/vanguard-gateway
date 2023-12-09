package dk.stonemountain.vanguard.domain;

import java.net.URI;
import java.text.MessageFormat;

import dk.stonemountain.vanguard.domain.RouteManager.ServiceEndpoint;
import dk.stonemountain.vanguard.domain.RouteManager.ServiceRoute;
import dk.stonemountain.vanguard.domain.filters.HostFilter;
import dk.stonemountain.vanguard.domain.filters.IntruderFilter;
import dk.stonemountain.vanguard.util.ApplicationException;
import io.vertx.core.http.HttpServerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
public class FilterManager {
    @Inject 
    HostFilter hostFilter;

    @Inject 
    IntruderFilter intruderFilter;

    public void preRequestFilter(HttpServerRequest request, UriInfo info) {
        hostFilter.preRequestFilter(request, info);
        intruderFilter.preRequestFilter(request, info);
    }

    public void preInvokeBackendFilter(URI url, ServiceRoute route, ServiceEndpoint endpoint, HttpServerRequest request) { // NOSONAR
    }

    public static class SecurityCheckFailed extends ApplicationException {
        private final String absoluteIncomingUrl;
        private final String method;
        private final String failureDescription;

        public SecurityCheckFailed(String failureDescription, String method, String absoluteIncomingUrl) {
            super(MessageFormat.format("Failed security check ({0}) for url {1} ({2} method)", failureDescription, absoluteIncomingUrl, method));
            this.failureDescription = failureDescription;
            this.method = method;
            this.absoluteIncomingUrl = absoluteIncomingUrl;
        }

        public String getAbsoluteIncomingUrl() {
            return absoluteIncomingUrl;
        }

        public String getMethod() {
            return method;
        }

        public String getFailureDescription() {
            return failureDescription;
        }
    }

}
