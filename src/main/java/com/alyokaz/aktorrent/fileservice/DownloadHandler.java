package com.alyokaz.aktorrent.fileservice;

import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;
import com.alyokaz.aktorrent.server.message.RequestPieceMessage;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;

public class DownloadHandler implements Runnable {

    private final InetSocketAddress address;
    private final FileService fileService;

    public DownloadHandler(InetSocketAddress address,
                           FileService fileService) {
        this.address = address;
        this.fileService = fileService;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(address.getAddress(), address.getPort());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            Thread.currentThread().setName(Thread.currentThread().getName() + " DownloadHandler");
            // request names of available files from peer
            out.writeObject(new Message(MessageType.REQUEST_FILENAMES));
            Set<String> filenames = (Set<String>) in.readObject();
            // check if we are interested in any of the available files and begin download
            filenames.stream().filter(fileService.getFiles()::containsKey).forEach(filename -> downloadPieces(filename, out, in));
            // signal to peer to close connection as we are finished
            out.writeObject(new Message(MessageType.END));
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void downloadPieces(String filename, ObjectOutputStream out, ObjectInputStream in) {
        // the container is potentially shared between multiple connections and is responsible for managing the
        // allocation of which pieces should be downloaded
        PieceContainer container = fileService.getFile(filename);
        if (container.complete()) return;
        try {
            while (!container.complete()) {
                // claim a piece that is not yet downloaded, or is not currently downloading, to prevent duplicate
                // downloads of the same piece
                int nextId = container.requestPiece();
                if (nextId == -1) break;
                // request piece from peer
                out.writeObject(new RequestPieceMessage(filename, nextId));
                Object readObject = in.readObject();
                if (readObject instanceof Piece) {
                    container.addPiece((Piece) readObject);
                }
                System.out.printf(Thread.currentThread().getName() + " - %.2f %%%n",
                        (container.getPieces().size() / (double) container.getTotalPieces()) * 100);
            }
            // build the file and save it to the local file system
            fileService.buildFile(container);
        } catch (EOFException e) {
            throw new RuntimeException();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
