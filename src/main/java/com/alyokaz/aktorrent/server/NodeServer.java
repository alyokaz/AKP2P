package com.alyokaz.aktorrent.server;

import com.alyokaz.aktorrent.fileservice.FileService;
import com.alyokaz.aktorrent.peerservice.PeerService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NodeServer extends AbstractServer {

    private final PeerService peerService;
    private final FileService fileService;

    public NodeServer(ServerSocket serverSocket, PeerService peerService,
                      FileService fileService) {
        super(serverSocket);
        this.peerService = peerService;
        this.fileService = fileService;
    }

    @Override
    public Runnable process(Socket socket) {
        return  new PeerHandler(socket, peerService, fileService);
    }
}
