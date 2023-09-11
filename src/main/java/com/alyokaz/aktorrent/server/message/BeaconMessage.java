package com.alyokaz.aktorrent.server.message;

import java.net.InetSocketAddress;

public class BeaconMessage extends Message {

    private InetSocketAddress serverAddress;

    public BeaconMessage(MessageType type, InetSocketAddress serverAddress) {
        super(type, serverAddress);
        this.serverAddress = serverAddress;
    }

    public InetSocketAddress getServerAddress() {
        return this.serverAddress;
    }
}
