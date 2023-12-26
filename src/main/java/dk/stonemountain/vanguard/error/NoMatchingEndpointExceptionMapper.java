package dk.stonemountain.vanguard.error;

import dk.stonemountain.vanguard.domain.RouteManager.NoMatchingEndpoint;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NoMatchingEndpointExceptionMapper  implements ExceptionMapper<NoMatchingEndpoint> {

    @Override
    public Response toResponse(NoMatchingEndpoint e) {
        return Response
            .status(Response.Status.NOT_FOUND)
            .entity(new Message(e.getMessage(), e.getPath()))
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    public static record Message(String messageCause, String path) {
    }
}
