package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        LOGGER.info("REQUEST  >> " + requestContext.getMethod()
                + " " + requestContext.getUriInfo().getRequestUri());
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        LOGGER.info("RESPONSE << " + responseContext.getStatus()
                + " for " + requestContext.getMethod()
                + " " + requestContext.getUriInfo().getRequestUri());
    }
}
