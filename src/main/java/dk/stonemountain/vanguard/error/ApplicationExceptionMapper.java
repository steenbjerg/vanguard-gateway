package dk.stonemountain.vanguard.error;

import dk.stonemountain.vanguard.util.ApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApplicationExceptionMapper implements ExceptionMapper<ApplicationException> {

    @Override
    public Response toResponse(ApplicationException e) {
        return Response
            .status(Response.Status.NOT_FOUND)
            // .type(MediaType.APPLICATION_JSON)
            .build();
    }

    public static record Message(String messageCause) {
    }
}
