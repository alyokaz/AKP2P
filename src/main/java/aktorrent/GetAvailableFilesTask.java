package aktorrent;

import aktorrent.message.Message;
import aktorrent.message.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.Callable;

public class GetAvailableFilesTask implements Callable<Set<FileInfo>> {

    InetSocketAddress address;

    public GetAvailableFilesTask(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    public Set<FileInfo> call() {
        try (Socket socket = new Socket(address.getHostName(), address.getPort());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(new Message(MessageType.REQUEST_AVAILABLE_FILES));
            Object obj = in.readObject();
            return (Set<FileInfo>) obj;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
