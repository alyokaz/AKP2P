package com.alyokaz.aktorrent.fileservice;

import com.alyokaz.aktorrent.peerservice.PeerService;
import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;
import com.alyokaz.aktorrent.server.message.RequestPieceMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormattedMessage;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;

public class DownloadHandler implements Runnable {

    private final InetSocketAddress address;
    private final FileService fileService;
    private final PeerService peerService;
    private static final Logger logger = LogManager.getLogger();

    public DownloadHandler(InetSocketAddress address,
                           FileService fileService, PeerService peerService) {
        this.address = address;
        this.fileService = fileService;
        this.peerService = peerService;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(address.getAddress(), address.getPort());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            // request names of available files from peer
            out.writeObject(new Message(MessageType.REQUEST_FILENAMES, peerService.getServerAddress()));
            Set<String> filenames = (Set<String>) in.readObject();

            // check if we are interested in any of the available files and begin download
            filenames.stream()
                    .filter(fileService.getFiles()::containsKey)
                    .forEach(filename -> downloadPieces(filename, out, in));

            // signal to peer to close connection as we are finished
            out.writeObject(new Message(MessageType.END, peerService.getServerAddress()));
        } catch (ConnectException e) {
            peerService.removeFromLivePeers(address);
            logger.error("Peer at {} is not reachable", address);
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
            logger.info("Beginning download of {} ... from {}", filename, address);
            while (!container.complete()) {
                // claim a piece that is not yet downloaded, or is not currently downloading, to prevent duplicate
                // downloads of the same piece
                int nextId = container.requestPiece();
                if (nextId == -1) break;

                // request piece from peer
                out.writeObject(new RequestPieceMessage(filename, nextId,
                        peerService.getServerAddress()));
                Object readObject = in.readObject();
                if (readObject instanceof Piece) {
                    container.addPiece((Piece) readObject);
                }

                StringFormattedMessage formattedMessage = new StringFormattedMessage("%.2f %%%n",
                        (container.getPieces().size() / (double) container.getTotalPieces()) * 100);
                logger.info(formattedMessage);
            }
            logger.info("Download of {} from {} complete.", filename, address);
            // build the file and save it to the local file system
            fileService.buildFile(container);
        } catch (EOFException e) {
            throw new RuntimeException();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
