package aktorrent;

import aktorrent.message.Message;
import aktorrent.message.MessageType;
import aktorrent.message.RequestPieceIDs;
import aktorrent.message.RequestPieceMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;
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
                    case REQUEST_PIECE -> processPiecesRequest((RequestPieceMessage) message, out);
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

    private void processPiecesRequest(RequestPieceMessage request, ObjectOutputStream out) {
        PieceContainer container = files.get(request.getFilename());
        Optional<Piece> piece = container.getPieces().stream().filter(p -> p.getId() == request.getPieceId()).findFirst();

        if(piece.isEmpty())
            return;

        try {
            out.writeObject(piece.get());
            //TODO Replace with download speed settings option
            Thread.sleep(0); //simulate download speed limit / connection speed
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        /*try {
            out.writeObject(new Message(MessageType.END));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
    }
}
