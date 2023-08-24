package aktorrent;

import aktorrent.message.Message;
import aktorrent.message.MessageType;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

import static aktorrent.FileUtils.buildPieceContainer;
import static aktorrent.FileUtils.getFileInfo;

public class AKTorrent {

    static final int BUFFER_SIZE = 1000000;
    private final int PORT;
    private final List<InetSocketAddress> peers = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, PieceContainer> files = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, File> completedFiles = new HashMap<>();
    private final Set<FileInfo> availableFiles = Collections.synchronizedSet(new HashSet<>());
    private Server server;
    private PingServer udpServer;
    private final Set<InetSocketAddress> connectedPeers = new HashSet<>();

    public AKTorrent(int port) {
        this.PORT = port;
    }

    public static void main(String[] args) throws IOException {
        AKTorrent node = new AKTorrent(Integer.parseInt(args[0]));
        CLI cli = new CLI(System.in, System.out, node);
        cli.start();
    }

    public void startClient() {
        discoverPeers();
        executor.execute(() -> peers.forEach(address ->
                executor.execute(new DownloadHandler(address, files, completedFiles))));
    }

    private void discoverPeers() {
        Set<Future<?>> futures = new HashSet<>();
        peers.forEach(address -> futures.add(executor.submit(new DiscoverPeersTask(address, peers))));
        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
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
        InetSocketAddress address = new InetSocketAddress(hostName, port);
        peers.add(address);
        pingPeer(address);
    }

    private void pingPeer(InetSocketAddress address) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] buf = PingServer.PING_PAYLOAD.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address.getAddress(), address.getPort());
            socket.send(packet);

            buf = new byte[PingServer.BUFFER_SIZE];
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            String payload = new String(packet.getData(), StandardCharsets.UTF_8).trim();
            if (payload.equals(PingServer.PONG_PAYLOAD)) this.connectedPeers.add(address);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<File> getFile(String filename) {
        File file = completedFiles.get(filename);
        if (file == null) return Optional.empty();
        return Optional.of(file);
    }

    public Set<FileInfo> getAvailableFiles() {
        Set<FileInfo> remoteAvailableFiles = new HashSet<>();
        Set<Future<Set<FileInfo>>> futures = new HashSet<>();
        peers.forEach(address -> {
            Future<Set<FileInfo>> future = executor.submit(() -> {

                try (Socket socket = new Socket(address.getHostName(), address.getPort());
                     ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                     ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                    out.writeObject(new Message(MessageType.REQUEST_AVAILABLE_FILES));
                    Object obj = in.readObject();
                    return (Set<FileInfo>) obj;

                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        });
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
                this.server = new Server(new ServerSocket(PORT), files, peers, availableFiles);
                discoverPeers();
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
        return this.connectedPeers;
    }

}
