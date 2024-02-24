package dk.stonemountain.vanguard;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.MDC;

import dk.stonemountain.vanguard.util.Log;
import dk.stonemountain.vanguard.util.Log.LogType;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    public static final String UNIQUE_REQUEST_ID_KEY = "REQUEST_ID";
    private static final String UNIQUE_REQUEST_TIMESTAMP_KEY = "REQUEST_TIMESTAMP";

    @Inject
    @Log(LogType.REQUEST)
    Logger requestLog;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        try {
            var id = UUID.randomUUID().toString();
            ctx.setProperty(UNIQUE_REQUEST_ID_KEY, id);
            ctx.setProperty(UNIQUE_REQUEST_TIMESTAMP_KEY, LocalDateTime.now());
            MDC.put(UNIQUE_REQUEST_ID_KEY, id);
    
            requestLog.info("Request with id {} at {}. Url: {} Method: {}, Headers: {}n e) {", MDC.get(UNIQUE_REQUEST_ID_KEY), LocalDateTime.now(), ctx.getUriInfo().getRequestUri(), ctx.getMethod(), ctx.getHeaders().entrySet());
        } catch (Exception e) {
            requestLog.error("request failed", e);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        try {
            var time = requestContext.getProperty(UNIQUE_REQUEST_TIMESTAMP_KEY);
            var startTime = time instanceof LocalDateTime ? (LocalDateTime) time : null;
            var elapsedTime = startTime != null ? Duration.between(LocalDateTime.now(), startTime) : null;
            var url = requestContext.getUriInfo().getAbsolutePath();
            requestLog.info("Request with id {} to url {} ({}) completed at {} with status {}. Duration: {}, Request Headers: {}, Response Headers: {}", MDC.get(UNIQUE_REQUEST_ID_KEY), url, requestContext.getMethod(), LocalDateTime.now(), responseContext.getStatus(), elapsedTime, requestContext.getHeaders(), responseContext.getHeaders().entrySet());
    
            MDC.remove(UNIQUE_REQUEST_ID_KEY);
            requestContext.removeProperty(UNIQUE_REQUEST_ID_KEY);
        } catch (Exception e) {
            requestLog.error("request failed", e);
        }
    }
}
