package aktorrent;

import aktorrent.message.Message;
import aktorrent.message.MessageType;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

import static aktorrent.FileUtils.buildPieceContainer;
import static aktorrent.FileUtils.getFileInfo;

public class AKTorrent {

    private final int PORT;
    private final List<InetSocketAddress> peers = Collections.synchronizedList(new ArrayList<>());

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final Map<String, PieceContainer> files = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, File> completedFiles = new HashMap<>();

    private final Set<FileInfo> availableFiles = Collections.synchronizedSet(new HashSet<>());

    static final int BUFFER_SIZE = 1000000;

    private Server server;

    public AKTorrent(int port) {
        this.PORT = port;
    }

    public void startClient() {
        discoverPeers();
        executor.execute(() -> {
            peers.forEach(address -> {
                executor.execute(new DownloadHandler(address, files, completedFiles));
            });
        });
    }

    private void discoverPeers() {
        CountDownLatch countDownLatch = new CountDownLatch(peers.size());

        peers.forEach(address -> executor.submit(() -> {
            try(Socket socket = new Socket(address.getHostName(), address.getPort());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                out.writeObject(new Message(MessageType.REQUEST_PEERS));
                Object obj = in.readObject();
                List<InetSocketAddress> peerList;
                if(obj instanceof List<?>) {
                    peerList = (List<InetSocketAddress>) obj;
                    peers.addAll(peerList);
                }
                countDownLatch.countDown();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }));

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void seedFile(File file) {
        this.files.put(file.getName(), buildPieceContainer(file));
        this.availableFiles.add(getFileInfo(file));
        startServer();
    }
    //TODO refactor to use FileInfo
    public void downloadFile(FileInfo fileInfo) {
        files.put(fileInfo.getFilename(), new PieceContainer(fileInfo));
        startClient();
    }

    //TODO Refactor to use hostname and port
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
        });
    }

    private void updateAvailableFiles() {
        try {
            this.availableFiles.addAll(getAvailableFiles().get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    public void startServer() {
        if(this.server == null) {
            try {
                this.server = new Server(new ServerSocket(PORT), files, peers, availableFiles);
                discoverPeers();
                updateAvailableFiles();
                server.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void shutDown() {
        if(server != null)
            server.shutdown();
    }

    public static void main(String[] args) throws IOException {
        AKTorrent node = new AKTorrent(Integer.parseInt(args[0]));
        CLI cli = new CLI(System.in, System.out, node);
        cli.start();
    }

}
