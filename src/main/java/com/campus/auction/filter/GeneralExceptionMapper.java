package com.campus.auction.filter;

import com.campus.auction.common.Result;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JAX-RS catch-all counterpart of the Exception branch in GlobalExceptionHandler.
 *
 * Catches any unhandled Throwable that escapes a Jersey resource method and
 * returns a 500 Internal Server Error with a Result.fail(...) JSON body.
 * ServiceException is handled first by the more specific ServiceExceptionMapper.
 */
@Slf4j
@Provider
@Component
public class GeneralExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        String detail = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(Result.fail(500, detail))
                .build();
    }
}
