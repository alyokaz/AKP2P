package com.alyokaz.aktorrent.peerservice;

import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

public class DiscoverPeersTask extends GetPeersTaskBase {

    public DiscoverPeersTask(InetSocketAddress address, List<InetSocketAddress> peers) {
        super(address, peers, (in, out) -> {
            try {
                out.writeObject(new Message(MessageType.REQUEST_PEERS));
                Object obj = in.readObject();
                List<InetSocketAddress> peerList;
                if (obj instanceof List<?>) {
                    peerList = (List<InetSocketAddress>) obj;
                    peers.addAll(peerList);
                }
            }catch(IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

}
