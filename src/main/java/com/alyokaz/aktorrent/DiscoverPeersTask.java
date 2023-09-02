package com.alyokaz.aktorrent;

import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.function.BiConsumer;

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
