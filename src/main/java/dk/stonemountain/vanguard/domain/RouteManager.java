package dk.stonemountain.vanguard.domain;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import dk.stonemountain.vanguard.util.ApplicationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
public class RouteManager {
    public enum FILTER_HANDLER {
        INTRUSION_DETECTION, DDOS_DETECTION, SQL_INJECTION_DETECTION, LOCAL_NETWORK_ONLY;
    }

    public enum Method {
        GET, PUT, POST, DELETE;
    }
    
    public static record ServiceEndpoint(String uri, Method method, boolean isSsl, boolean isSse, Duration connectionTimeout, Duration readTimeout) {}

    public static record Service(String host, int port, List<ServiceEndpoint> endpoints) {}

    public static record ServiceRoute(String domain, Service service) {
        public ServiceEndpoint matchEndpoint(Method method, UriInfo info) {
            return service.endpoints().stream()
                .sorted((e1, e2) -> e1.uri.length() < e2.uri.length() ? 1 : 0)
                .filter(e -> e.method() == method && (info.getPath().equals(e.uri()) || info.getPath().startsWith(e.uri() + "/")))
                .findFirst()
                .orElseThrow(() -> new NoMatchingEndpoint(info.getPath(), this));
        }
    }

    private final List<ServiceRoute> endpoints = new ArrayList<>();
    
    public ServiceRoute find(UriInfo uriInfo) {
        var host = uriInfo.getBaseUri().getHost();
        return getServiceRoutes()
            .filter(r -> r.domain().equalsIgnoreCase(host.trim()))
            .findFirst()
            .orElseThrow(() -> new NoMatchingRoute(uriInfo.getAbsolutePath()));
    }

    public Stream<ServiceRoute> getServiceRoutes() {
        return endpoints.stream();
    }

    public void addEndpoint(ServiceRoute endpoint) {
        endpoints.add(endpoint);
    }

    public static class NoMatchingEndpoint extends ApplicationException {
        private final String path;
        private final ServiceRoute route;

        public NoMatchingEndpoint(String path, ServiceRoute route) {
            super("No endpoint matches " + path + " in route " + route);
            this.path = path;
            this.route = route;
        }

        public String getPath() {
            return path;
        }

        public ServiceRoute getRoute() {
            return route;
        }
    }

    public static class NoMatchingRoute extends ApplicationException {
        private final URI uri;

        public NoMatchingRoute(URI uri) {
            super("No Route matching url "+ uri);
            this.uri = uri;
        }

        public URI getUri() {
            return uri;
        }
    }
}
