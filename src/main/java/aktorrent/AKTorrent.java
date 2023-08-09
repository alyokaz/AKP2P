package aktorrent;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

public class AKTorrent {

    private final int PORT;
    private List<InetSocketAddress> peers = new ArrayList<>();

    private ExecutorService executor = Executors.newCachedThreadPool();

    private Map<String, PieceContainer> files = Collections.synchronizedMap(new HashMap<>());

    private int totalPieces = 0;

    private final Map<String, File> completedFiles = new HashMap<>();

    private static final int BUFFER_SIZE = 1000000;

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

    private PieceContainer buildPieceContainer(File file) {
        SortedSet<Piece> pieces = new TreeSet<>();
        int numberOfPieces = getNoOfPieces(file);

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            IntStream.range(0, numberOfPieces).forEach(i -> {
                try {
                    int bytesRead = in.read(buffer);
                    pieces.add(new Piece(i,
                            // make last Piece correct length
                            bytesRead < BUFFER_SIZE ? Arrays.copyOf(buffer, bytesRead) : buffer.clone()
                    ));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return new PieceContainer(file.getName(), numberOfPieces, pieces);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int getNoOfPieces(File file) {
        int numberOfPieces = (int) file.length() / BUFFER_SIZE;
        if ((file.length() % BUFFER_SIZE) != 0) {
            numberOfPieces++;
        }
        return numberOfPieces;
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


}
