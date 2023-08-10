package aktorrent;

import aktorrent.message.Message;
import aktorrent.message.MessageType;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static aktorrent.FileUtils.buildPieceContainer;

public class AKTorrent {

    private final int PORT;
    private final List<InetSocketAddress> peers = new ArrayList<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final Map<String, PieceContainer> files = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, File> completedFiles = new HashMap<>();

    static final int BUFFER_SIZE = 1000000;

    public AKTorrent(int port) {
        this.PORT = port;
    }

    public void startClient() {
        executor.submit(() -> {
            peers.forEach(address -> {
                executor.submit(new DownloadHandler(address, files, completedFiles));
            });
        });
    }

    public void seedFile(File file) {
        this.files.put(file.getName(), buildPieceContainer(file));
        Server server = new Server(this.PORT, files);
        server.start();
    }

    public void downloadFile(PieceContainer container) {
        files.put(container.getFilename(), container);
        startClient();
    }

    public void addPeer(InetSocketAddress address) {
        peers.add(address);
    }

    public Optional<File> getFile(String filename) {
        File file = completedFiles.get(filename);
        if (file == null)
            return Optional.empty();
        return Optional.of(file);
    }

    public Future<Set<FileInfo>> getAvailableFiles() {
        return executor.submit(() -> {
            Set<FileInfo> availableFiles = new HashSet<>();
            Set<Future<Set<FileInfo>>> futures = new HashSet<>();
            peers.forEach(address -> {
                Future<Set<FileInfo>> future = executor.submit(() -> {

                    try (Socket socket = new Socket(address.getHostName(), address.getPort());
                         ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                         ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                        out.writeObject(new Message(MessageType.REQUEST_AVAILABLE_FILES));
                        Object obj = in.readObject();
                        return ((Set<FileInfo>) obj);

                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                });
                futures.add(future);
            });
            futures.forEach(f -> {
                try {
                    availableFiles.addAll(f.get());
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
            return availableFiles;
        });
    }
}
