package com.alyokaz.aktorrent;

import com.alyokaz.aktorrent.cli.CLI;
import com.alyokaz.aktorrent.fileservice.FileInfo;
import com.alyokaz.aktorrent.fileservice.FileService;
import com.alyokaz.aktorrent.peerservice.PeerService;
import com.alyokaz.aktorrent.pingserver.PingServer;
import com.alyokaz.aktorrent.server.Server;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;


public class AKTorrent {

    private  int port;
    private Server server;
    private PingServer udpServer;
    private final PeerService peerService = new PeerService();
    private final FileService fileService = new FileService(peerService);

    public AKTorrent(int port) {
        this.port = port;
    }

    public AKTorrent() {
        this(0);
    }

    public void downloadAllFiles() {
        fileService.downloadAllFiles();
    }

    public int seedFile(File file) {
        fileService.addFile(file);
        return startServer();
    }

    public void downloadFile(FileInfo fileInfo) {
        fileService.addFile(fileInfo);
        downloadAllFiles();
    }

    public void addPeer(String hostName, int port) {
        this.peerService.addPeer(hostName, port);
    }

    public Optional<File> getFile(String filename) {
        File file = fileService.getCompletedFile(filename);
        if (file == null) return Optional.empty();
        return Optional.of(file);
    }

    public int startServer() {
        if (this.server == null) {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                this.port = serverSocket.getLocalPort();
                this.server = new Server(serverSocket, peerService, fileService);
                server.start();
                peerService.discoverPeers();

                if (this.udpServer == null) {
                    this.udpServer = new PingServer(new DatagramSocket(port));
                    this.udpServer.start();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return this.port;
    }

    public void shutDown() {
        if (server != null) server.shutdown();
        if (udpServer != null) udpServer.shutdown();
    }

    public Set<InetSocketAddress> getLivePeers() {
        return this.peerService.getLivePeers();
    }

    public Set<FileInfo> getAvailableFiles() {
        return fileService.getAvailableFiles();
    }

    public static AKTorrent createAndInitialize(InetSocketAddress beaconAddress) {
        AKTorrent akTorrent = new AKTorrent();
        akTorrent.startServer();
        akTorrent.peerService.contactBeacon(akTorrent.server.getServerAddress(), beaconAddress);
        akTorrent.peerService.discoverPeers();
        return akTorrent;
    }

    public static void main(String[] args) throws IOException {
        AKTorrent node = new AKTorrent(Integer.parseInt(args[0]));
        CLI cli = new CLI(System.in, System.out, node);
        cli.start();
    }
}
