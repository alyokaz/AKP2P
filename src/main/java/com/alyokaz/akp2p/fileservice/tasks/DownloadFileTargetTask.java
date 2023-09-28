package com.alyokaz.akp2p.fileservice.tasks;

import com.alyokaz.akp2p.fileservice.FileInfo;
import com.alyokaz.akp2p.fileservice.FileService;
import com.alyokaz.akp2p.fileservice.Piece;
import com.alyokaz.akp2p.fileservice.PieceContainer;
import com.alyokaz.akp2p.fileservice.exceptions.DownloadException;
import com.alyokaz.akp2p.peerservice.PeerService;
import com.alyokaz.akp2p.server.message.RequestPieceMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormattedMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * This class handles the downloading of {@code Pieces} for a single file from a single peer.
 */
public class DownloadFileTargetTask implements Runnable {

    protected final InetSocketAddress address;
    protected final FileService fileService;
    protected final PeerService peerService;
    private final FileInfo fileInfo;
    protected static final Logger logger = LogManager.getLogger();

    public DownloadFileTargetTask(InetSocketAddress address,
                                  FileService fileService, PeerService peerService, FileInfo fileInfo) {
        this.address = address;
        this.fileService = fileService;
        this.peerService = peerService;
        this.fileInfo = fileInfo;
    }

    /**
     * This method establishes a connection to the peer at the given address and then hands off the {@code InputStream}
     * and {@code OutputStream} to the {@link #downloadPieces} method.
     */
    @Override
    public void run() {
        try (Socket socket = new Socket(address.getAddress(), address.getPort());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            downloadPieces(fileInfo.getFilename(), out, in);
        } catch (IOException | DownloadException e) {
            // We handle the exceptions here to allow any other peer connections to continue
            peerService.removeFromLivePeers(address);
            logger.error("Download from peer at {} failed for {}", address, e.getMessage());
        }
    }

    /**
     * This method handles the downloading of {@code Piece}s from the peer connection.
     *
     *
     * @param filename the filename the {@code PieceContainer} for the related file is stored under
     * @param out the {@code OutputStream} for the peer connection
     * @param in the {@code InputStream} for the peer connection
     */
    protected void downloadPieces(String filename, ObjectOutputStream out, ObjectInputStream in) {
        // the container is potentially shared between multiple connections and is responsible for managing the
        // allocation of which pieces should be downloaded
        PieceContainer container = fileService.getFile(filename);
        if (container.complete()) return;


        logger.info("Beginning download of {} ... from {}", filename, address);
        while (!container.complete()) {
            // claim a piece that is not yet downloaded, or is not currently downloading, to prevent duplicate
            // downloads of the same piece
            int nextId = container.requestPiece();
            if (nextId == -1) break;

            try {
                // request piece from peer
                out.writeObject(new RequestPieceMessage(filename, nextId,
                        peerService.getServerAddress()));

                Object readObject = null;
                readObject = in.readObject();

                if (readObject instanceof Piece) {
                    container.addPiece((Piece) readObject);
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new DownloadException(e.getMessage(), e);
            }

            StringFormattedMessage formattedMessage = new StringFormattedMessage("%.2f %%%n",
                    (container.getPieces().size() / (double) container.getTotalPieces()) * 100);
            logger.debug(formattedMessage);
        }
        logger.info("Download of {} from {} complete.", filename, address);
        // build the file and save it to the local file system
        fileService.buildFile(container);

    }
}
