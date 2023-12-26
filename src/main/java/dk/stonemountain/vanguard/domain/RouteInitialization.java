package dk.stonemountain.vanguard.domain;

import java.io.File;
import java.nio.file.Paths;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;


@ApplicationScoped
public class RouteInitialization {
    private static final Logger LOG = LoggerFactory.getLogger(RouteInitialization.class);

    @ConfigProperty(name = "routes.file", defaultValue = "/home/steenbjerg/workspaces/vanguard/vanguard-gateway/endpoints-dev.json")                                    
    String routesFile;

    @Inject 
    RouteManager cache;

    void onStart(@Observes StartupEvent ev) { // NOSONAR
        LOG.info("The application is starting...");
        
        setupDomainModel();
    }

    void onStop(@Observes ShutdownEvent ev) {               
        LOG.info("The application is stopping... Standard shutdown: {}", ev.isStandardShutdown());
    }

    private void setupDomainModel() {
        var currentFolder = Paths.get(".").toFile();
        LOG.info("Current folder: {}, Routes file {} exists: {}", currentFolder.getAbsolutePath(), routesFile, new File(routesFile).exists());

        LOG.info("Initializing routes: {}", routesFile);
        var file = Paths.get(routesFile);
        var endpoints = cache.load(file);
        cache.clearAndAddRoutes(endpoints);        
        cache.store(Paths.get("/tmp/endpoints.json"));
    }
}