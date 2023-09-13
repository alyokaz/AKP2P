package com.alyokaz.aktorrent.server;

import com.alyokaz.aktorrent.fileservice.FileService;
import com.alyokaz.aktorrent.fileservice.Piece;
import com.alyokaz.aktorrent.fileservice.PieceContainer;
import com.alyokaz.aktorrent.peerservice.PeerService;
import com.alyokaz.aktorrent.peerservice.PingPeerException;
import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;
import com.alyokaz.aktorrent.server.message.RequestPieceMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;
import java.util.Set;

public class PeerHandler implements Runnable {

    private final Socket peerSocket;
    private final PeerService peerService;
    private final FileService fileService;
    private static final Logger logger = LogManager.getLogger();

    public PeerHandler(Socket peerSocket, PeerService peerService, FileService fileService) {
        this.peerSocket = peerSocket;
        this.peerService = peerService;
        this.fileService = fileService;
    }

    @Override
    public void run() {
        try(Socket peerSocket = this.peerSocket;
            ObjectOutputStream out = new ObjectOutputStream(peerSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(peerSocket.getInputStream())
        ){
            logger.info("Client connected to server at {}", peerService.getServerAddress());

            boolean end = false;
            while(!Thread.currentThread().isInterrupted() && !end) {
                Message message = (Message) in.readObject();
                peerService.addPeer(message.getServerAddress());
                switch (message.getType()) {
                    case REQUEST_FILENAMES -> out.writeObject(Set.copyOf(fileService.getFiles().keySet()));
                    case REQUEST_PIECE -> processPieceRequest((RequestPieceMessage) message, out);
                    case REQUEST_AVAILABLE_FILES -> processAvailableFilesRequest(out);
                    case REQUEST_PEERS -> processRequestPeers(out);
                    case END -> end = true;
                }
            }
        } catch (EOFException e){
            logger.error("Client connected at {} closed with {}", peerService.getServerAddress(), e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (PingPeerException e) {
            //TODO write tests and implementation
        }
        logger.info("Client connect to server at {} closed", peerService.getServerAddress());
    }

    private void processRequestPeers(ObjectOutputStream out) throws IOException {
        out.writeObject(peerService.getLivePeers());
    }

    private void processAvailableFilesRequest(ObjectOutputStream out) throws IOException {
        out.writeObject(fileService.getAvailableFiles());
    }

    private void processPieceRequest(RequestPieceMessage request, ObjectOutputStream out) {
        PieceContainer container = fileService.getFile(request.getFilename());
        Optional<Piece> piece = container.getPieces().stream().filter(p -> p.getId() == request.getPieceId()).findFirst();

        try {
            if(piece.isEmpty())
                out.writeObject(new Message(MessageType.END, null));
            else
                out.writeObject(piece.get());
            //TODO Replace with download speed settings option
            Thread.sleep(0); //simulate download speed limit / connection speed
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
