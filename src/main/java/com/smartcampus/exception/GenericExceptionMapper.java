package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        LOGGER.severe("Unexpected error: " + exception.getClass().getName() + " - " + exception.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("INTERNAL_SERVER_ERROR",
                        "An unexpected error occurred. Please try again later."))
                .build();
    }
}
