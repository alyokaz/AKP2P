package com.alyokaz.aktorrent.server.message;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class Message implements Serializable {

    private final MessageType type;
    private final InetSocketAddress serverAddress;

    public Message(MessageType type, InetSocketAddress serverAddress) {
        this.type = type;
        this.serverAddress = serverAddress;
    }

    public MessageType getType() {
        return type;
    }

    public InetSocketAddress getServerAddress() {
        return serverAddress;
    }
}
