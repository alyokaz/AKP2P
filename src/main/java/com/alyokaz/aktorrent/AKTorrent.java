package com.alyokaz.aktorrent;

import com.alyokaz.aktorrent.cli.CLI;
import com.alyokaz.aktorrent.pingserver.PingServer;
import com.alyokaz.aktorrent.server.Server;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;


public class AKTorrent {

    static final int BUFFER_SIZE = 1000000;
    private final int PORT;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private Server server;
    private PingServer udpServer;
    private final PeerService peerService = new PeerService();
    private final FileService fileService = new FileService(peerService);

    public AKTorrent(int port) {
        this.PORT = port;
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

    public void seedFile(File file) {
        fileService.addFile(file);
        startServer();
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

    public void startServer() {
        if (this.server == null) {
            try {
                this.server = new Server(new ServerSocket(PORT), peerService, fileService);
                this.peerService.discoverPeers();
                this.fileService.updateAvailableFiles();
                server.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (this.udpServer == null) {
            try {
                this.udpServer = new PingServer(new DatagramSocket(PORT));
                this.udpServer.start();
            } catch (SocketException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void shutDown() {
        if (server != null) server.shutdown();

        if (udpServer != null) udpServer.shutdown();
    }

    public Set<InetSocketAddress> getConnectedPeers() {
        return this.peerService.getConnectedPeers();
    }

    public Set<FileInfo> getAvailableFiles() {
        return fileService.getAvailableFiles();
    }
}
