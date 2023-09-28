package com.alyokaz.akp2p.peerservice.tasks;

import com.alyokaz.akp2p.peerservice.PeerService;
import com.alyokaz.akp2p.server.message.Message;
import com.alyokaz.akp2p.server.message.MessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;

/**
 * This class is for send request for and handling the results of a request for a {@code Set} of know live peers from
 * known live peers.
 */
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

    /**
     * Contacts the peer at the given address, requests a {@code Set} of known live peers and then attempts to add
     * each received peer to this nodes {@code Set} of known live peers.
     */
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
