package io.vertx.example;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

public class HelloWorldEmbedded {

    public static void main(String[] args) {
        // enable logging
        System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4jLogDelegateFactory");

        final String podName = System.getenv("HOSTNAME") != null ? System.getenv("HOSTNAME") : "localhost";

        // Create an HTTP server which simply returns "Hello World!" to each request.
        HttpServer server = Vertx.vertx().createHttpServer()
            .requestHandler(req ->
                req.response().end("Hello World! from pod: " + podName + "\n"));

        System.out.println("vert.x HTTP server running on port 8080");
        // start server which automatic closes when jvm terminates
        server.listen(8080);
    }

}
