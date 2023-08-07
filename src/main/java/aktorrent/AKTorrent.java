package aktorrent;

import aktorrent.message.Message;
import aktorrent.message.MessageType;
import aktorrent.message.RequestPieceMessage;

import javax.swing.text.html.Option;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
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

    private Map<String, PieceContainer> files = new HashMap<>();

    private int totalPieces = 0;

    private Map<String, File> completedFiles = new HashMap<>();

    private static final int BUFFER_SIZE = 1000000;

    public AKTorrent(int port) {
        this.PORT = port;
    }


    public Future startClient() {
        return executor.submit(() -> {
            List<Future> futures = new ArrayList<>();
            peers.forEach(address -> {
                    Future future = executor.submit(() -> {
                        try (Socket socket = new Socket(address.getAddress(), address.getPort());
                             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
                        ) {
                            out.writeObject(new Message(MessageType.REQUEST_FILENAMES));
                            Set<String> filenames = (Set<String>) in.readObject();
                            filenames.stream().filter(files::containsKey)
                                    .forEach(filename -> downloadPieces(filename, out, in));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    futures.add(future);
                });
            futures.forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private void downloadPieces(String filename, ObjectOutputStream out, ObjectInputStream in) {
        // TODO Change to Download each piece separately then check which pieces are still needed again
        PieceContainer container  = files.get(filename);

        if(container.complete())
            return;

        try {
            while(!container.complete()) {
                // request piece for file
                out.writeObject(new RequestPieceMessage(filename, container.getPieces().size()));

                // read in each requested piece
                Object readObject = in.readObject();
                if (readObject instanceof Piece) {
                    container.getPieces().add((Piece) readObject);
                }
                System.out.println(String.format("%.2f %%", (container.getPieces().size() / (double) container.getTotalPieces() ) * 100));
                if (container.getPieces().size() == container.getTotalPieces())
                    buildFile(container);
            }
        } catch (EOFException e) {
            if(container.getPieces().size() == container.getTotalPieces())
                buildFile(container);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private void buildFile(PieceContainer container) {
        File outputFile = new File(container.getFilename());
        try {
            outputFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try(BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            container.getPieces().stream().sorted(Comparator.comparing(Piece::getId)).forEach((piece) -> {
                try {
                    out.write(piece.getData());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        completedFiles.put(outputFile.getName(), outputFile);
    }

    public void seedFile(File file) {
        this.files.put(file.getName(), buildPieceContainer(file));
        Server server = new Server(this.PORT, files);
        server.start();
    }

    public Future downloadFile(PieceContainer container) {
        files.put(container.getFilename(), container);
        return startClient();
    }

    private PieceContainer buildPieceContainer(File file) {
        Set<Piece> pieces = new HashSet<>();
        int numberOfPieces = getNoOfPieces(file);
        try(BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            IntStream.range(0, numberOfPieces).forEach(i -> {
                try {
                    int bytesRead = in.read(buffer);
                    pieces.add(new Piece(i,
                            // make last Piece correct length
                            bytesRead < BUFFER_SIZE? Arrays.copyOf(buffer, bytesRead) : buffer.clone(),
                            numberOfPieces));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            });
            return new PieceContainer(file.getName(), numberOfPieces, pieces);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int getNoOfPieces(File file) {
        int numberOfPieces = (int) file.length() / BUFFER_SIZE;
        if((file.length() % BUFFER_SIZE) != 0) {
            numberOfPieces++;
        }
        return numberOfPieces;
    }

    public void addPeer(InetSocketAddress address) {
        peers.add(address);
    }

    public Optional<File> getFile(String filename) {
        File file = completedFiles.get(filename);
        if(file == null)
            return Optional.empty();
        return Optional.of(file);
    }


}
