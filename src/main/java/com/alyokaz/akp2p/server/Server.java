package com.alyokaz.akp2p.server;

import java.net.InetSocketAddress;
import java.net.Socket;

public interface Server {

    void start();

    void shutdown();

    InetSocketAddress getServerAddress();

}
