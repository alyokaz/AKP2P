package com.alyokaz.aktorrent.beacon;

import com.alyokaz.aktorrent.pingserver.PingServer;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class Beacon {

    final private BeaconServer beaconServer;
    final private PingServer pingServer;

    public Beacon(BeaconServer beaconServer, PingServer pingServer) {
        this.beaconServer = beaconServer;
        this.pingServer = pingServer;
    }

    public void shutDown() {
        if(this.beaconServer != null)
            this.beaconServer.shutDown();
        if(this.pingServer != null)
            this.pingServer.shutdown();
    }

    public InetSocketAddress getAddress() {
        return beaconServer.getAddress();
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

        BeaconServer beaconServer = new BeaconServer(serverSocket, peers);
        beaconServer.start();

        PingServer pingServer = new PingServer(datagramSocket);
        pingServer.start();
        return new Beacon(beaconServer, pingServer);
    }

}
