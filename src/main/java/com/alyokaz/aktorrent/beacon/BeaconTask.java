package com.alyokaz.aktorrent.beacon;

import com.alyokaz.aktorrent.server.message.BeaconMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class BeaconTask implements Runnable {

    private final Socket socket;
    private final List<InetSocketAddress> peers;

    public BeaconTask(Socket socket, List<InetSocketAddress> peers) {
        this.socket = socket;
        this.peers = peers;
    }

    @Override
    public void run() {
        try(Socket socket = this.socket;
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            Object obj = in.readObject();
            if(obj instanceof BeaconMessage) {
                BeaconMessage message = (BeaconMessage) obj;
                out.writeObject(peers);
                peers.add(message.getServerAddress());
            }
        } catch (IOException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}
