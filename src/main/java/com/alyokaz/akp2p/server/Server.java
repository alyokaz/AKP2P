package com.alyokaz.akp2p.server;

import java.net.InetSocketAddress;
import java.net.Socket;

public interface Server {

    public void start();

    public void shutdown();

    public InetSocketAddress getServerAddress();

    public Runnable process(Socket socket);
}
