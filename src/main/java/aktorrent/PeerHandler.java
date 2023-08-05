package aktorrent;

import aktorrent.message.Message;
import aktorrent.message.MessageType;
import aktorrent.message.RequestPieceIDs;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Set;

public class PeerHandler implements Runnable {

    private Socket peerSocket;
    private Map<String, PieceContainer> files;

    public PeerHandler(Socket peerSocket, Map<String, PieceContainer> files) {
        this.peerSocket = peerSocket;
        this.files = files;
    }

    @Override
    public void run() {
        try(Socket peerSocket = this.peerSocket;
            ObjectOutputStream out = new ObjectOutputStream(peerSocket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(peerSocket.getInputStream())
        ){
            while(!Thread.currentThread().isInterrupted()) {
                Message message = (Message) in.readObject();
                switch (message.getType()) {
                    case REQUEST_FILENAMES -> out.writeObject(Set.copyOf(files.keySet()));
                    case REQUEST_PIECES -> processPiecesRequest((RequestPieceIDs) message, out);
                }
            }
        } catch (EOFException e){
            System.out.println("Client closed");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void processPiecesRequest(RequestPieceIDs request, ObjectOutputStream out) {
        PieceContainer container = files.get(request.getFilename());
        container.getPieces().stream()
                .filter(piece -> !request.getIds().contains(piece.getId()))
                .forEach(piece -> {
                    try {
                        out.writeObject(piece);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        try {
            out.writeObject(new Message(MessageType.END));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
