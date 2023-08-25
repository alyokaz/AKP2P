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

import static com.alyokaz.aktorrent.FileUtils.buildPieceContainer;
import static com.alyokaz.aktorrent.FileUtils.getFileInfo;

public class AKTorrent {

    static final int BUFFER_SIZE = 1000000;
    private final int PORT;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, PieceContainer> files = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, File> completedFiles = new HashMap<>();
    private final Set<FileInfo> availableFiles = Collections.synchronizedSet(new HashSet<>());
    private Server server;
    private PingServer udpServer;
    private final PeerService peerService = new PeerService();

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
                executor.execute(new DownloadHandler(address, files, completedFiles))));
    }

    public void seedFile(File file) {
        this.files.put(file.getName(), buildPieceContainer(file));
        this.availableFiles.add(getFileInfo(file));
        startServer();
    }

    public void downloadFile(FileInfo fileInfo) {
        files.put(fileInfo.getFilename(), new PieceContainer(fileInfo));
        startClient();
    }

    public void addPeer(String hostName, int port) {
        this.peerService.addPeer(hostName, port);
    }

    public Optional<File> getFile(String filename) {
        File file = completedFiles.get(filename);
        if (file == null) return Optional.empty();
        return Optional.of(file);
    }

    public Set<FileInfo> getAvailableFiles() {
        Set<FileInfo> remoteAvailableFiles = new HashSet<>();
        Set<Future<Set<FileInfo>>> futures = new HashSet<>();
        this.peerService.getPeers().forEach(address -> futures.add(executor.submit(new GetAvailableFilesTask(address))));
        futures.forEach(f -> {
            try {
                remoteAvailableFiles.addAll(f.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        return remoteAvailableFiles;
    }

    private void updateAvailableFiles() {
        this.availableFiles.addAll(getAvailableFiles());
    }

    public void startServer() {
        if (this.server == null) {
            try {
                this.server = new Server(new ServerSocket(PORT), files, peerService.getPeers(), availableFiles);
                this.peerService.discoverPeers();
                updateAvailableFiles();
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

}
