package com.alyokaz.aktorrent;

import com.alyokaz.aktorrent.cli.CLI;
import com.alyokaz.aktorrent.pingserver.PingServer;
import com.alyokaz.aktorrent.server.Server;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class AKTorrent {

    static final int BUFFER_SIZE = 1000000;
    private  int port;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private Server server;
    private PingServer udpServer;
    private InetSocketAddress beaconAddress;
    private final PeerService peerService = new PeerService(beaconAddress);
    private final FileService fileService = new FileService(peerService);

    public AKTorrent(int port) {
        this.port = port;
    }

    public AKTorrent(int port, InetSocketAddress beaconAddress) {
        this.port = port;
        this.beaconAddress = beaconAddress;
    }

    public AKTorrent() {
        this.port = 0;
    }

    public static void main(String[] args) throws IOException {
        AKTorrent node = new AKTorrent(Integer.parseInt(args[0]));
        CLI cli = new CLI(System.in, System.out, node);
        cli.start();
    }

    public void startClient() {
        peerService.discoverPeers();
        executor.execute(() -> this.peerService.getPeers().forEach(address ->
                executor.execute(new DownloadHandler(address, fileService))));
    }

    public int seedFile(File file) {
        fileService.addFile(file);
        return startServer();
    }

    public void downloadFile(FileInfo fileInfo) {
        fileService.addFile(fileInfo);
        startClient();
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
                if(this.beaconAddress != null)
                    this.peerService.contactBeacon(new InetSocketAddress(serverSocket.getInetAddress(), serverSocket.getLocalPort()), this.beaconAddress);
                this.peerService.discoverPeers();
                this.fileService.updateAvailableFiles();
                server.start();
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

    public Set<InetSocketAddress> getConnectedPeers() {
        return this.peerService.getConnectedPeers();
    }

    public Set<FileInfo> getAvailableFiles() {
        peerService.discoverPeers();
        return fileService.getAvailableFiles();
    }

    public void setBeaconAddress(String hostname, int port) {
        this.beaconAddress = new InetSocketAddress(hostname, port);
    }
}
