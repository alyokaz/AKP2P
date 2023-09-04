package com.alyokaz.aktorrent.peerservice;

import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.List;

public class DiscoverPeersTask extends AbstractPeersServiceTask {

    public DiscoverPeersTask(InetSocketAddress address, PeerService peerService) {
        super(address, peerService);
    }

    @Override
    void process(ObjectInputStream in, ObjectOutputStream out) {
        try {
            out.writeObject(new Message(MessageType.REQUEST_PEERS));
            Object obj = in.readObject();
            if (obj instanceof List<?>) {
                ((List<InetSocketAddress>) obj).forEach(peerService::addPeer);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
