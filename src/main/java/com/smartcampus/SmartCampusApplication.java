package com.smartcampus;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    private static final Logger LOGGER = Logger.getLogger(SmartCampusApplication.class.getName());
    private static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static void main(String[] args) throws IOException {
        final ResourceConfig config = new ResourceConfig().packages("com.smartcampus");
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);

        LOGGER.info("Smart Campus API started at " + BASE_URI);
        System.out.println("==============================================");
        System.out.println("  Smart Campus API running at " + BASE_URI);
        System.out.println("==============================================");
        System.out.println("Press ENTER to stop the server...");
        System.in.read();
        server.shutdownNow();
    }
}
