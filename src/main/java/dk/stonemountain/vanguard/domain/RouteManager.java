package dk.stonemountain.vanguard.domain;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.stonemountain.vanguard.util.ApplicationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.ws.rs.core.UriInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ApplicationScoped
public class RouteManager {
    @Inject
    Logger log;
    
    public enum FILTER_HANDLER {
        INTRUSION_DETECTION, DDOS_DETECTION, SQL_INJECTION_DETECTION, LOCAL_NETWORK_ONLY;
    }

    public enum Method {
        GET, PUT, POST, DELETE;
    }
    
    public static record ServiceEndpoint(@NotNull @NotBlank String uri, @NotNull Method method, @NotNull @JsonbProperty("ssl") boolean isSsl, @NotNull @JsonbProperty("sse") boolean isSse, @NotNull @NotNull Duration connectionTimeout, @NotNull Duration readTimeout) {}
    public static record Service(@NotNull @NotBlank String host, @NotNull @NotBlank int port, @NotNull List<ServiceEndpoint> endpoints) {}

    public static record ServiceRoute(@NotNull @NotBlank String domain, @NotNull List<Service> services) {
        public Match matchEndpoint(Method method, UriInfo info) {
            final var endpoint = services
                .stream()
                .flatMap(s -> s.endpoints().stream())
                .sorted((e1, e2) -> e1.uri.length() < e2.uri.length() ? 1 : 0)
                .peek(e -> LoggerFactory.getLogger(ServiceRoute.class).info("Endpoint: {}, uri path {}", e, info.getPath()))
                .filter(e -> e.method() == method && (info.getPath().equals(e.uri()) || info.getPath().startsWith(e.uri() + (e.uri().endsWith("/") ? "" : "/"))))
                .findFirst()
                .orElseThrow(() -> new NoMatchingEndpoint(info.getPath(), this));

            final var service = services
                .stream()
                .filter(s -> s.endpoints().contains(endpoint))
                .findFirst()
                .orElseThrow(() -> new NoMatchingEndpoint(info.getPath(), this));

            return new Match(this, service, endpoint);

        }
    }

    public static record Match(ServiceRoute route, Service service, ServiceEndpoint endpoint) {}

    private final List<ServiceRoute> routes = new ArrayList<>();
    
    public ServiceRoute find(UriInfo uriInfo) {
        var host = uriInfo.getBaseUri().getHost();
        return getServiceRoutes()
            .filter(r -> r.domain().equalsIgnoreCase(host.trim()))
            .findFirst()
            .orElseThrow(() -> new NoMatchingRoute(uriInfo.getAbsolutePath()));
    }

    public Stream<ServiceRoute> getServiceRoutes() {
        return routes.stream();
    }

    public void clearAndAddRoutes(List<ServiceRoute> routes) {
        this.routes.clear();
        this.routes.addAll(routes);
    }

    public void addRoute(ServiceRoute route) {
        routes.add(route);
    }

    public List<ServiceRoute> load(Path file) {
        try {
            JsonbConfig config = new JsonbConfig()
                .withFormatting(true); 
            Jsonb jsonb = JsonbBuilder.newBuilder()
                .withConfig(config)
                .build();

            var json = Files.readString(file, StandardCharsets.UTF_8);

            return jsonb.fromJson(json, new ArrayList<ServiceRoute>(){}.getClass().getGenericSuperclass());
        } catch (Exception e) {
            throw new StorageFailure(file, e);
        }
    }

    public void store(Path file) {
        try {
            JsonbConfig config = new JsonbConfig()
                .withFormatting(true);
            Jsonb jsonb = JsonbBuilder.newBuilder()
                .withConfig(config)
                .build();
            var json = jsonb.toJson(routes);
            log.info("Json: {}", json);

            Files.writeString(file, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            throw new StorageFailure(file, e);
        }
    }
    public static class StorageFailure extends ApplicationException {
        private final transient Path file;

        public StorageFailure(Path file, Exception e) {
            super(e.getMessage(), e);
            this.file = file;
        }

        public Path getFile() {
            return file;
        }
    }

    public static class NoMatchingEndpoint extends ApplicationException {
        private final String path;
        private final transient ServiceRoute route;


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
