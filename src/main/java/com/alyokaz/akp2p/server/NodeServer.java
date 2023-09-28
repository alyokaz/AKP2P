package com.alyokaz.akp2p.server;

import com.alyokaz.akp2p.fileservice.FileService;
import com.alyokaz.akp2p.peerservice.PeerService;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * A {@code Server} implementation to be used in non-{@code Beacon} nodes.
 */
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
