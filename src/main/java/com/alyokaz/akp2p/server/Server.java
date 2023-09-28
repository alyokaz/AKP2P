package com.alyokaz.akp2p.server;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * An interface for the behaviour common to all Server implementations
 */
public interface Server {

    /**
     * Start this server.
     */
    void start();

    /**
     * Shutdown this server.
     */
    void shutdown();

    /**
     * Returns the address this server is listening on.
     * @return the address this server is listening on
     */
    InetSocketAddress getServerAddress();
}
