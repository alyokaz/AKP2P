package com.alyokaz.aktorrent.beacon;

import com.alyokaz.aktorrent.pingserver.PingServer;
import com.alyokaz.aktorrent.server.BeaconServer;
import com.alyokaz.aktorrent.server.Server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class Beacon {

    final private Server beaconServer;
    final private PingServer pingServer;

    public Beacon(Server beaconServer, PingServer pingServer) {
        this.beaconServer = beaconServer;
        this.pingServer = pingServer;
    }

    public void shutDown() {
        if(this.beaconServer != null)
            this.beaconServer.shutdown();
        if(this.pingServer != null)
            this.pingServer.shutdown();
    }

    public InetSocketAddress getAddress() {
        return beaconServer.getServerAddress();
    }

    public static Beacon createAndInitialise() {
        ServerSocket serverSocket = null;
        DatagramSocket datagramSocket = null;
        try {
            serverSocket = new ServerSocket(0);
            datagramSocket = new DatagramSocket(serverSocket.getLocalPort());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<InetSocketAddress> peers = new ArrayList<>();

        Server beaconServer = new BeaconServer(serverSocket, peers);
        beaconServer.start();

        PingServer pingServer = new PingServer(datagramSocket);
        pingServer.start();
        return new Beacon(beaconServer, pingServer);
    }

}
