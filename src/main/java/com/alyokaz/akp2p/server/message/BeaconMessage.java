package com.alyokaz.akp2p.server.message;

import java.net.InetSocketAddress;

/**
 * A subclass of {@code Message} used to register a peer with a {@code Beacon}
 */
public class BeaconMessage extends Message {

    private final InetSocketAddress serverAddress;

    public BeaconMessage(MessageType type, InetSocketAddress serverAddress) {
        super(type, serverAddress);
        this.serverAddress = serverAddress;
    }

    /**
     * Returns the address of the peer that sent this message.
     *
     * @return the address of the peer that sent this message
     */
    public InetSocketAddress getServerAddress() {
        return this.serverAddress;
    }
}
