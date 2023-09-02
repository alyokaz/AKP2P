package com.alyokaz.aktorrent.beacon;

import com.alyokaz.aktorrent.pingserver.PingServer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class Beacon {

    final private List<InetSocketAddress> peers = new ArrayList<>();
    private BeaconServer beaconServer;
    private PingServer pingServer;

    public int start() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            beaconServer = new BeaconServer(serverSocket, peers);
            beaconServer.start();
            pingServer = new PingServer(new DatagramSocket(serverSocket.getLocalPort()));
            pingServer.start();
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutDown() {
        if(this.beaconServer != null)
            this.beaconServer.shutDown();
        if(this.pingServer != null)
            this.pingServer.shutdown();
    }

}
