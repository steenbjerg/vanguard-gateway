package dk.stonemountain.vanguard.domain;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.stonemountain.vanguard.domain.RouteManager.Method;
import dk.stonemountain.vanguard.domain.RouteManager.Service;
import dk.stonemountain.vanguard.domain.RouteManager.ServiceEndpoint;
import dk.stonemountain.vanguard.domain.RouteManager.ServiceRoute;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.time.Duration;


@ApplicationScoped
public class RouteInitialization {
    private static final Logger LOG = LoggerFactory.getLogger(RouteInitialization.class);

    @Inject RouteManager cache; // NOSONAR

    void onStart(@Observes StartupEvent ev) {               
        LOG.info("The application is starting...");
        setupDomainModel();
    }

    void onStop(@Observes ShutdownEvent ev) {               
        LOG.info("The application is stopping...");
    }

    private void setupDomainModel() {
        LOG.info("Initializing routes");

        var service = new Service("localhost", 8280, List.of(
            new ServiceEndpoint("/tests", Method.GET, false, false, Duration.ofSeconds(2), Duration.ofSeconds(2)),
            new ServiceEndpoint("/sse-test", Method.GET, false, true, Duration.ofSeconds(2), Duration.ofSeconds(2)),
            new ServiceEndpoint("/tests", Method.POST, false, false, Duration.ofSeconds(2), Duration.ofSeconds(2)))
        );
        var route = new ServiceRoute("localhost", service);
        LOG.info("Ading route: {}", route);
        cache.addEndpoint(route);
    }
}