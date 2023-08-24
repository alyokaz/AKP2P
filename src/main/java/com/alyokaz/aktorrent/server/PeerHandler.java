package com.alyokaz.aktorrent.server;

import com.alyokaz.aktorrent.FileInfo;
import com.alyokaz.aktorrent.Piece;
import com.alyokaz.aktorrent.PieceContainer;
import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;
import com.alyokaz.aktorrent.server.message.RequestPieceMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PeerHandler implements Runnable {

    private final Socket peerSocket;
    private final Map<String, PieceContainer> files;

    private final List<InetSocketAddress> peers;

    private final Set<FileInfo> availableFiles;

    public PeerHandler(Socket peerSocket, Map<String, PieceContainer> files, List<InetSocketAddress> peers,
                       Set<FileInfo> availableFiles) {
        this.peerSocket = peerSocket;
        this.files = files;
        this.peers = peers;
        this.availableFiles = availableFiles;
    }

    @Override
    public void run() {
        try(Socket peerSocket = this.peerSocket;
            ObjectOutputStream out = new ObjectOutputStream(peerSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(peerSocket.getInputStream())
        ){
            Thread.currentThread().setName(Thread.currentThread().getName() + " PeerHandler");
            System.out.println(Thread.currentThread().getName() + " Client Connected");
            boolean end = false;
            while(!Thread.currentThread().isInterrupted() && !end) {
                Message message = (Message) in.readObject();
                switch (message.getType()) {
                    case REQUEST_FILENAMES -> out.writeObject(Set.copyOf(files.keySet()));
                    case REQUEST_PIECE -> processPieceRequest((RequestPieceMessage) message, out);
                    case REQUEST_AVAILABLE_FILES -> processAvailableFilesRequest(out);
                    case REQUEST_PEERS -> processRequestPeers(out);
                    case END -> end = true;
                }
            }
        } catch (EOFException e){
            System.out.println(Thread.currentThread().getName() + " - Client closed");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Client Closed");
    }

    private void processRequestPeers(ObjectOutputStream out) throws IOException {
        out.writeObject(peers);
    }

    private void processAvailableFilesRequest(ObjectOutputStream out) throws IOException {
        out.writeObject(availableFiles);
    }

    private void processPieceRequest(RequestPieceMessage request, ObjectOutputStream out) {
        PieceContainer container = files.get(request.getFilename());
        Optional<Piece> piece = container.getPieces().stream().filter(p -> p.getId() == request.getPieceId()).findFirst();

        try {
            if(piece.isEmpty())
                out.writeObject(new Message(MessageType.END));
            else
                out.writeObject(piece.get());
            //TODO Replace with download speed settings option
            Thread.sleep(1); //simulate download speed limit / connection speed
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
