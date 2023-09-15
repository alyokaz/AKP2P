package com.alyokaz.aktorrent.fileservice;

import com.alyokaz.aktorrent.peerservice.PeerService;
import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;
import com.alyokaz.aktorrent.server.message.RequestPieceMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormattedMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;

public abstract class AbstractDownloadHandler implements Runnable {

    protected final InetSocketAddress address;
    protected final FileService fileService;
    protected final PeerService peerService;
    protected static final Logger logger = LogManager.getLogger();

    public AbstractDownloadHandler(InetSocketAddress address,
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
            process(out, in);
        } catch (ConnectException e) {
            peerService.removeFromLivePeers(address);
            logger.error("Peer at {} is not reachable", address);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void process(ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException;

    protected void downloadPieces(String filename, ObjectOutputStream out, ObjectInputStream in) {
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
