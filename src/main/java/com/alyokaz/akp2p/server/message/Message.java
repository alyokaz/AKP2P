package com.alyokaz.akp2p.server.message;

import java.io.Serializable;
import java.net.InetSocketAddress;

/**
 * This class is used for communication between peers.
 */
public class Message implements Serializable {

    private final MessageType type;
    private final InetSocketAddress serverAddress;

    public Message(MessageType type, InetSocketAddress serverAddress) {
        this.type = type;
        this.serverAddress = serverAddress;
    }

    /**
     * Returns the {@code MessageType} of this {@code Message}.
     *
     * @return the {@code MessageType} of this {@code Message}
     */
    public MessageType getType() {
        return type;
    }

    /**
     * Returns the address of the peer that sent this {@code Message}.
     *
     * @return the address of the peer that send this {@code Message}
     */
    public InetSocketAddress getServerAddress() {
        return serverAddress;
    }
}
