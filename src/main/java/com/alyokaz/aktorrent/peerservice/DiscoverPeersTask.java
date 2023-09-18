package com.alyokaz.aktorrent.peerservice;

import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;

public class DiscoverPeersTask implements Runnable {

    private final InetSocketAddress address;
    private final PeerService peerService;
    private final InetSocketAddress serverAddress;

    private Logger logger = LogManager.getLogger();

    public DiscoverPeersTask(InetSocketAddress address, PeerService peerService, InetSocketAddress serverAddress) {
        this.address = address;
        this.peerService = peerService;
        this.serverAddress = serverAddress;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(address.getHostName(), address.getPort());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(new Message(MessageType.REQUEST_PEERS, serverAddress));
            Object obj = in.readObject();
            if (obj instanceof Set<?>) {
                ((Set<InetSocketAddress>) obj).forEach(peerService::addPeer);
            }
        } catch (IOException | ClassNotFoundException e) {
            peerService.removeFromLivePeers(address);
            logger.error("Peer discovery failed at {} with {}", address, e.getMessage());
        }
    }

}
