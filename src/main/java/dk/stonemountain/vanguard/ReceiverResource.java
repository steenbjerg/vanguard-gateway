package dk.stonemountain.vanguard;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
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
import dk.stonemountain.vanguard.domain.RouteManager.Match;
import dk.stonemountain.vanguard.domain.RouteManager.Method;
import dk.stonemountain.vanguard.domain.UrlHelper;
import io.vertx.core.http.HttpServerRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
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

    @PUT
    @Path("/{paths: .+}")
    @Consumes(MediaType.MEDIA_TYPE_WILDCARD)
    @Produces(MediaType.MEDIA_TYPE_WILDCARD)
    public Response putRequest(@Context HttpHeaders headers, @Context HttpServerRequest incomingRequest, @Context UriInfo uriInfo, InputStream body) {
        return handleRequest(Method.PUT, headers, incomingRequest, uriInfo, body);
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
        var match = route.matchEndpoint(method, uriInfo);
        var url = new UrlHelper().constructURL(match.service(), match.endpoint(), uriInfo);
        var backendHeaders = headers.getRequestHeaders().entrySet()
            .stream()
            .filter(e -> forwardHeader(e.getKey().toLowerCase()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        backendHeaders.put("X-Forwarded-For", List.of(incomingRequest.remoteAddress().hostAddress()));
        backendHeaders.put("X-Forwarded-Host", List.of(incomingRequest.authority().host()));
        backendHeaders.put("X-Forwarded-Port", List.of(Integer.toString(incomingRequest.authority().port())));
        backendHeaders.put("X-Forwarded-Proto", List.of(incomingRequest.scheme()));

        // Security check route/endpoint
        filterManager.preInvokeBackendFilter(url, route, match.endpoint(), incomingRequest);

        // Invoke backend

        var response = requestHandler.invokeAndReturn(match.endpoint(), url, backendHeaders, BodyPublishers.ofInputStream(() -> body));

        // Find streaming output handler
        var contentTypes = response.headers().map().get("content-type");
        var contentType = contentTypes == null || contentTypes.isEmpty() ? "" : contentTypes.getFirst();
        var output = switch (contentType) {
            case "text/event-stream" -> getSseStreamingOutput(response); 
            default -> getDefaultStreamingOutput(response);
        };

        // return response
        var builder = Response
            .status(response.statusCode())
            .entity(output);

        builder.replaceAll(null);
        response.headers().map().entrySet().stream()
            .filter(e -> respondHeader(e.getKey().toLowerCase()))
            .map(e -> redirectCheck(e, incomingRequest, response, match))
            .forEach(e -> addHeader(builder, e));

        return builder.build();
    }

    private void addHeader(ResponseBuilder builder, Entry<String, List<String>> e) {
        e.getValue().stream()
            .peek(v -> log.info("Adding header {} with value {}", e.getKey(), v))
            .forEach(v -> builder.header(e.getKey(), v));
    }

    private Entry<String, List<String>> redirectCheck(Entry<String, List<String>> header, HttpServerRequest request, HttpResponse<InputStream> response, Match match) {
        if (response.statusCode() >= 300 && response.statusCode() < 400 && "location".equalsIgnoreCase(header.getKey())) {
            var location = !header.getValue().isEmpty() ? header.getValue().get(0) : "";
            var backendHost = match.service().host();
            var backendPort = match.service().port();

            try {
                var redirectUrl = new URI(location);
                if (backendHost.equalsIgnoreCase(redirectUrl.getHost()) && backendPort == redirectUrl.getPort()) {
                    var hostAndPort = request.authority();
                    var redirectedUrl = new URI(request.scheme(), redirectUrl.getUserInfo(), hostAndPort.host(), hostAndPort.port(), redirectUrl.getPath(), redirectUrl.getQuery(), redirectUrl.getFragment());
                    header.setValue(List.of(redirectedUrl.toString()));
                }
            } catch (Exception e) {
                log.error("Faild checking for redirect for header {} in url {}", header, location);
            }
        }
        return header;
    }

    private boolean forwardHeader(String header) {
        return switch (header) {
            case "postman-token" -> false; 
            case "host" -> false;
            case "connection" -> false;
            case "content-length" -> false;
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
}
