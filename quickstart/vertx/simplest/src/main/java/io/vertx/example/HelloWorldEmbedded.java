package io.vertx.example;

import io.vertx.core.Vertx;


/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class HelloWorldEmbedded {

  public static void main(String[] args) {
    // Create an HTTP server which simply returns "Hello World!" to each request.
    String podName = System.getenv("HOSTNAME");
    Vertx.vertx().createHttpServer().requestHandler(req -> req.response().end("Hello World! from pod: " + podName + "\n")).listen(8080);
  }

}
