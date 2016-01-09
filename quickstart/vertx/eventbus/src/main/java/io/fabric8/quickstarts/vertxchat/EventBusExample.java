package io.fabric8.quickstarts.vertxchat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.eventbus.EventBus;

/**
 *
 */
public class EventBusExample extends AbstractVerticle {

  private static final String HOST_NAME = System.getenv("HOSTNAME");

  public static void main(String[] args) {
    Launcher.main(new String[]{"run", EventBusExample.class.getName(), "-cluster"});
  }

  @Override
  public void start() throws Exception {

    EventBus eb = vertx.eventBus();

    // Register to listen for messages coming IN to the server
    eb.consumer("example.testeb").handler(message -> {
      System.out.println(EventBusExample.this + " received an incoming message " + message.body());
    });

    vertx.setPeriodic(1000, tid -> eb.publish("example.testeb", "Test message from " + HOST_NAME));

  }
}