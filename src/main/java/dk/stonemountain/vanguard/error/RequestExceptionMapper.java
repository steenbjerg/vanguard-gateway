package dk.stonemountain.vanguard.error;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;

import dk.stonemountain.vanguard.RequestInvoker.RequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RequestExceptionMapper implements ExceptionMapper<RequestException> {

    @Override
    public Response toResponse(RequestException e) {
        return Response
            .status(getStatus(e))
            // .entity(new Message(e.getUrl() != null ? e.getUrl().toString() : "", e.getCause() != null ? e.getCause().getClass().getName() : "", e.getMessage() != null ? e.getMessage() : ""))
            // .type(MediaType.APPLICATION_JSON)
            .build();
    }

    public static record Message(String url, String failure, String message) {
    }

    protected Response.Status getStatus(RequestException e) {
        return switch(e.getCause()) {
            case null -> Response.Status.INTERNAL_SERVER_ERROR;
            case ConnectException exp -> Response.Status.NOT_FOUND;
            case HttpTimeoutException exp -> Response.Status.GATEWAY_TIMEOUT;
            default -> Response.Status.INTERNAL_SERVER_ERROR;
        };
    }
}
