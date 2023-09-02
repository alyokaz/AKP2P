package com.alyokaz.aktorrent.peerservice;

import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.List;

public class DiscoverPeersTask extends AbstractPeersServiceTask {

    public DiscoverPeersTask(InetSocketAddress address, List<InetSocketAddress> peers) {
        super(address, peers);
    }

    @Override
    void process(ObjectInputStream in, ObjectOutputStream out) {
        try {
            out.writeObject(new Message(MessageType.REQUEST_PEERS));
            Object obj = in.readObject();
            List<InetSocketAddress> peerList;
            if (obj instanceof List<?>) {
                peerList = (List<InetSocketAddress>) obj;
                peers.addAll(peerList);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
