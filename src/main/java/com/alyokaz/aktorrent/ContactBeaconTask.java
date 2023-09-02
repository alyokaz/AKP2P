package com.alyokaz.aktorrent;

import com.alyokaz.aktorrent.server.message.BeaconMessage;
import com.alyokaz.aktorrent.server.message.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.BiConsumer;

public class ContactBeaconTask extends GetPeersTaskBase {

    private final InetSocketAddress serverAddress;

    public ContactBeaconTask(InetSocketAddress beaconAddress, List<InetSocketAddress> peers, InetSocketAddress serverAddress) {
        super(beaconAddress, peers, (in, out) -> {
            try {
                out.writeObject(new BeaconMessage(MessageType.REQUEST_PEERS, serverAddress));
                Object obj = in.readObject();
                if(obj instanceof List<?>)
                    peers.addAll(((List<InetSocketAddress>)obj));
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        this.serverAddress = serverAddress;
    }

}
