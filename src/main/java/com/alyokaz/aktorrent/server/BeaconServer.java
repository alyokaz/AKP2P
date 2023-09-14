package com.alyokaz.aktorrent.server;

import com.alyokaz.aktorrent.beacon.BeaconTask;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class BeaconServer extends AbstractServer {

    private final List<InetSocketAddress> peers;

    public BeaconServer(ServerSocket serverSocket, List<InetSocketAddress> peers) {
        super(serverSocket);
        this.peers = peers;
    }

    @Override
    public Runnable process(Socket socket) {
        return new BeaconTask(socket, peers);
    }

}