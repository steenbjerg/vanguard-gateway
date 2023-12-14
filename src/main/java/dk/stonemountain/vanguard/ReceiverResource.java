package dk.stonemountain.vanguard;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import dk.stonemountain.vanguard.domain.FilterManager;
import dk.stonemountain.vanguard.domain.RouteManager;
import dk.stonemountain.vanguard.domain.RouteManager.Method;
import dk.stonemountain.vanguard.domain.UrlHelper;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
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
        return handleRequest(Method.GET, headers, incomingRequest, uriInfo, null);
    }

    @POST
    @Path("/{paths: .+}")
    @Consumes(MediaType.MEDIA_TYPE_WILDCARD)
    @Produces(MediaType.MEDIA_TYPE_WILDCARD)
    public Response postRequest(@Context HttpHeaders headers, @Context HttpServerRequest incomingRequest, @Context UriInfo uriInfo, InputStream body) {
        return handleRequest(Method.POST, headers, incomingRequest, uriInfo, body);
    }

    @DELETE
    @Path("/{paths: .+}")
    @Consumes(MediaType.MEDIA_TYPE_WILDCARD)
    @Produces(MediaType.MEDIA_TYPE_WILDCARD)
    public Response deleteRequest(@Context HttpHeaders headers, @Context HttpServerRequest incomingRequest, @Context UriInfo uriInfo, InputStream body) {
        return handleRequest(Method.DELETE, headers, incomingRequest, uriInfo, body);
    }

    private Response handleRequest(Method method, HttpHeaders headers, HttpServerRequest incomingRequest, UriInfo uriInfo, InputStream body) {
        // Security check on input
        filterManager.preRequestFilter(incomingRequest, uriInfo);

        // Construct backend request
        var route = manager.find(uriInfo);
        var endpoint = route.matchEndpoint(method, uriInfo);
        var url = new UrlHelper().constructURL(route.service(), endpoint, uriInfo);
        var backendHeaders = headers.getRequestHeaders().entrySet()
            .stream()
            .filter(e -> forwardHeader(e.getKey()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

        // Security check route/endpoint
        filterManager.preInvokeBackendFilter(url, route, endpoint, incomingRequest);

        // Invoke backend

        var response = requestHandler.invokeAndReturn(endpoint, url, backendHeaders, BodyPublishers.ofInputStream(() -> body));

        // Find streaming output handler
        var contentType = response.headers().map().get("content-type").getFirst();
        var output = switch (contentType) {
            case "text/event-stream" -> getSseStreamingOutput(response); 
            default -> getDefaultStreamingOutput(response);
        };

        // return response
        var builder = Response
            .status(response.statusCode())
            .entity(output);

        response.headers().map().entrySet().stream()
            .filter(e -> respondHeader(e.getKey()))
            .flatMap(e -> headerValueStream(e.getKey(), e.getValue()))
            .forEach(e -> builder.header(e.getKey(), e.getValue()));

        return builder.build();
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

    private StreamingOutput getDefaultStreamingOutput(HttpResponse<InputStream> response) {
        return o -> response.body().transferTo(o);
    }

    private StreamingOutput getSseStreamingOutput(HttpResponse<InputStream> response) {
        return o -> {
            var out = new OutputStreamWriter(o);
            try (var r = new BufferedReader(new InputStreamReader(response.body()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    out.write(line + "\n");
                    out.flush();
                }
            }
        };
    }

    private Stream<Map.Entry<String, String>> headerValueStream(String key, List<String> values) {
        return values.stream()
            .collect(Collectors.toMap(v -> key, v -> v))
            .entrySet()
            .stream();
    }


}
