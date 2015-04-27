package io.fabric8.mq.controller.protocol.stomp;

import org.apache.activemq.command.Response;

import java.io.IOException;

interface ResponseHandler {
    void onResponse(StompProtocolConverter converter, Response response) throws IOException;
}

