package aktorrent;

import aktorrent.message.Message;
import aktorrent.message.MessageType;
import aktorrent.message.RequestPieceMessage;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DownloadHandler implements Runnable {

    private final InetSocketAddress address;

    private final Map<String, PieceContainer> files;

    private final Map<String, File> completedFiles;

    public DownloadHandler(InetSocketAddress address, Map<String, PieceContainer> files,
                           Map<String, File> completedFiles) {
        this.address = address;
        this.files = files;
        this.completedFiles = completedFiles;
    }


    @Override
    public void run() {
        try (Socket socket = new Socket(address.getAddress(), address.getPort());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
        ) {
            Thread.currentThread().setName(Thread.currentThread().getName() + " DownloadHandler");
            out.writeObject(new Message(MessageType.REQUEST_FILENAMES));
            Set<String> filenames = (Set<String>) in.readObject();
            filenames.stream().filter(files::containsKey)
                    .forEach(filename -> downloadPieces(filename, out, in));
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void downloadPieces(String filename, ObjectOutputStream out, ObjectInputStream in) {
        PieceContainer container  = files.get(filename);
        if(container.complete())
            return;

        try {
            while(!container.complete()) {
                int nextId = container.requestPiece();

                if(nextId == -1)
                    break;
                // request piece for file
                out.writeObject(new RequestPieceMessage(filename, nextId));

                // read in each requested piece
                Object readObject = in.readObject(); // Locked
                if (readObject instanceof Piece) {
                    container.addPiece((Piece) readObject);
                }
                System.out.println(String.format(Thread.currentThread().getName() + " - %.2f %%", (container.getPieces().size() / (double) container.getTotalPieces()) * 100));
            }
            buildFile(container);
        } catch (EOFException e) {
            throw new RuntimeException();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildFile(PieceContainer container) throws IOException {
        File outputFile = new File(container.getFilename());

        outputFile.createNewFile();

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            container.getPieces().stream().sorted(Comparator.comparing(Piece::getId)).forEach(p -> {
                try {
                    out.write(p.getData());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            completedFiles.put(outputFile.getName(), outputFile);
        }
    }

}
