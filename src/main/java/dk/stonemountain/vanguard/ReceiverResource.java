package dk.stonemountain.vanguard;

import java.io.InputStream;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import dk.stonemountain.vanguard.RequestInvoker.RequestException;
import dk.stonemountain.vanguard.domain.FilterManager;
import dk.stonemountain.vanguard.domain.RouteManager;
import dk.stonemountain.vanguard.domain.RouteManager.Method;
import dk.stonemountain.vanguard.domain.RouteManager.NoMatchingEndpoint;
import dk.stonemountain.vanguard.domain.UrlHelper;
import dk.stonemountain.vanguard.util.Log;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/")
public class ReceiverResource {
    @Inject
    Logger log;

    @Inject
    RouteManager manager;

    @Inject
    RequestInvoker requestHandler;

    @Inject
    FilterManager filterManager;

    @GET
    @Path("/{paths: .+}")
    @Produces(MediaType.MEDIA_TYPE_WILDCARD)
    public Response getRequest(@Context HttpHeaders headers, @Context HttpServerRequest incomingRequest, @Context UriInfo uriInfo) {
        try {
            // Security check on input
            filterManager.preRequestFilter(incomingRequest, uriInfo);

            // Construct backend request
            var route = manager.find(uriInfo);
            var endpoint = route.matchEndpoint(Method.GET, uriInfo);
            var url = new UrlHelper().constructURL(route.service(), endpoint, uriInfo);
            var backendHeaders = headers.getRequestHeaders().entrySet()
                .stream()
                .filter(e -> forwardHeader(e.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

            // Security check route/endpoint
            filterManager.preInvokeBackendFilter(url, route, endpoint, incomingRequest);

            // Invoke backend
            var response = requestHandler.invokeAndReturn(endpoint, url, backendHeaders, null);

            // return response
            var builder = Response
                .status(response.statusCode())
                .entity(response.body());

            response.headers().map().entrySet().stream()
                .filter(e -> respondHeader(e.getKey()))
                .flatMap(e -> headerValueStream(e.getKey(), e.getValue()))
                .forEach(e -> builder.header(e.getKey(), e.getValue()));

            return builder.build();
        } catch (NoMatchingEndpoint e) {
            log.error("No matched for reqeust", e);
            return Response
                .status(Response.Status.NOT_FOUND)
                .entity(e.getPath())
                .type(MediaType.TEXT_PLAIN)
                .build();
        } catch (RequestException e) {
            log.error("Failed to invoke post reqeust", e);
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(e.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
        }
    }

    private Stream<Map.Entry<String, String>> headerValueStream(String key, List<String> values) {
        return values.stream().collect(Collectors.toMap(v -> key, v -> v)).entrySet().stream();
    }

    @POST
    @Path("/{paths: .+}")
    @Consumes(MediaType.MEDIA_TYPE_WILDCARD)
    @Produces(MediaType.MEDIA_TYPE_WILDCARD)
    public Response postRequest(@Context HttpHeaders headers, @Context HttpServerRequest incomingRequest, @Context UriInfo uriInfo, InputStream body) {
        try {
            // Security check on input
            filterManager.preRequestFilter(incomingRequest, uriInfo);

            // Construct backend request
            var route = manager.find(uriInfo);
            var endpoint = route.matchEndpoint(Method.POST, uriInfo);
            var url = new UrlHelper().constructURL(route.service(), endpoint, uriInfo);
            var backendHeaders = headers.getRequestHeaders().entrySet()
                .stream()
                .filter(e -> forwardHeader(e.getKey()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

            // Security check route/endpoint
            filterManager.preInvokeBackendFilter(url, route, endpoint, incomingRequest);

            // Invoke backend

            var response = requestHandler.invokeAndReturn(endpoint, url, backendHeaders, BodyPublishers.ofInputStream(() -> body));

            // return response
            var builder = Response
                .status(response.statusCode())
                .entity(response.body());

            response.headers().map().entrySet().stream()
                .filter(e -> respondHeader(e.getKey()))
                .flatMap(e -> headerValueStream(e.getKey(), e.getValue()))
                .forEach(e -> builder.header(e.getKey(), e.getValue()));

            return builder.build();

        } catch (NoMatchingEndpoint e) {
            log.error("No matched for reqeust", e);
            return Response
                .status(Response.Status.NOT_FOUND)
                .entity(e.getPath())
                .type(MediaType.TEXT_PLAIN)
                .build();
        } catch (RequestException e) {
            log.error("Failed to invoke post reqeust", e);
            return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(e.getMessage())
                .type(MediaType.TEXT_PLAIN)
                .build();
        }
    }

    private boolean forwardHeader(String header) {
        return switch (header) {
            case "Postman-Token" -> false; 
            case "Host" -> false;
            case "Connection" -> false;
            case "Content-Length" -> false;
            case "x-quarkus-hot-deployment-done" -> false;
            default -> true;
        };
    }

    private boolean respondHeader(String header) {
        return switch (header) {
            case "content-length" -> false;
            case ":status" -> false;
            default -> true;
        };
    }

}
