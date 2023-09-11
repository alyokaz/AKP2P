package com.alyokaz.aktorrent.peerservice;

import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.Set;

public class DiscoverPeersTask extends AbstractPeersServiceTask {

    private InetSocketAddress serverAddress;

    public DiscoverPeersTask(InetSocketAddress address, PeerService peerService,
                             InetSocketAddress serverAddress) {
        super(address, peerService);
        this.serverAddress = serverAddress;
    }

    @Override
    void process(ObjectInputStream in, ObjectOutputStream out) {
        try {
            out.writeObject(new Message(MessageType.REQUEST_PEERS, serverAddress));
            Object obj = in.readObject();
            if (obj instanceof Set<?>) {
                ((Set<InetSocketAddress>) obj).forEach(peerService::addPeer);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
