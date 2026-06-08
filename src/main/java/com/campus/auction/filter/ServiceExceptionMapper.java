package com.campus.auction.filter;

import com.campus.auction.common.Result;
import com.campus.auction.exception.ServiceException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.springframework.stereotype.Component;

/**
 * JAX-RS counterpart of the ServiceException branch in GlobalExceptionHandler.
 *
 * Translates ServiceException (which carries a Spring HttpStatus) into a
 * JAX-RS Response with the same status code and a Result.fail(...) JSON body.
 */
@Provider
@Component
public class ServiceExceptionMapper implements ExceptionMapper<ServiceException> {

    @Override
    public Response toResponse(ServiceException ex) {
        int status = ex.getStatus().value();
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(Result.fail(status, ex.getMessage()))
                .build();
    }
}
