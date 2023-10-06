package com.alyokaz.akp2p.beacon;

import com.alyokaz.akp2p.server.message.BeaconMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

/**
 * This class handles a request for a peer to register with a {@code Beacon} node.
 */
public class BeaconTask implements Runnable {

    private static final Logger logger = LogManager.getLogger();
    private final Socket socket;
    private final List<InetSocketAddress> peers;


    public BeaconTask(Socket socket, List<InetSocketAddress> peers) {
        this.socket = socket;
        this.peers = peers;
    }

    /**
     * Registers the peer from the connection with this {@code Beacon}.
     */
    @Override
    public void run() {
        try (Socket socket = this.socket;
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            Object obj = in.readObject();
            if (obj instanceof BeaconMessage) {
                BeaconMessage message = (BeaconMessage) obj;
                out.writeObject(peers);
                peers.add(message.getServerAddress());
                logger.info("Peer at {} registered", message.getServerAddress());
            }
        } catch (IOException | ClassNotFoundException ex) {
            logger.error("Handling beacon connection from {} failed with {}",
                    socket.getInetAddress().getHostAddress() + socket.getLocalPort(),
                    ex.getMessage());
        }
    }
}
