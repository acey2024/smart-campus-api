package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse("LINKED_RESOURCE_NOT_FOUND", exception.getMessage()))
                .build();
    }
}
