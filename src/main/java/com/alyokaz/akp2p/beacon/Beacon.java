package com.alyokaz.akp2p.beacon;

import com.alyokaz.akp2p.pingserver.PingServer;
import com.alyokaz.akp2p.server.BeaconServer;
import com.alyokaz.akp2p.server.Server;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a {@code Beacon} node that other nodes in the network can register and receive the
 * addresses of other nodes who have also registered.
 */
public class Beacon {

    final private Server beaconServer;
    final private PingServer pingServer;

    public Beacon(Server beaconServer, PingServer pingServer) {
        this.beaconServer = beaconServer;
        this.pingServer = pingServer;
    }

    /**
     * Static factory method for creation and initialisation of a {@code Beacon} node.
     *
     * @return a fully initialised {@code Beacon} node
     */
    public static Beacon createAndInitialise() {
        return createAndInitialise(0);
    }

    /**
     * Static factory method for creation and initialisation of a {@code Beacon} node listening on the given port.
     *
     * @return a fully initialised {@code Beacon} node listening of the given port.
     */
    public static Beacon createAndInitialise(int port) {
        ServerSocket serverSocket = null;
        DatagramSocket datagramSocket = null;
        try {
            serverSocket = new ServerSocket(port);
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

    /**
     * Shuts down this {@code Beacon}
     */
    public void shutDown() {
        if (this.beaconServer != null)
            this.beaconServer.shutdown();
        if (this.pingServer != null)
            this.pingServer.shutdown();
    }

    /**
     * Returns the address this {@code Beacon} is listening on.
     *
     * @return the address this {@code Beacon} is listening on
     */
    public InetSocketAddress getAddress() {
        return beaconServer.getServerAddress();
    }

}
