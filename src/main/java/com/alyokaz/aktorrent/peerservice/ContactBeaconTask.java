package com.alyokaz.aktorrent.peerservice;

import com.alyokaz.aktorrent.server.message.BeaconMessage;
import com.alyokaz.aktorrent.server.message.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.List;

public class ContactBeaconTask extends AbstractPeersServiceTask {

    private final InetSocketAddress serverAddress;

    public ContactBeaconTask(InetSocketAddress address, PeerService peerService, InetSocketAddress serverAddress) {
        super(address, peerService);
        this.serverAddress = serverAddress;
    }

    @Override
    void process(ObjectInputStream in, ObjectOutputStream out) {
        try {
            out.writeObject(new BeaconMessage(MessageType.REQUEST_PEERS, serverAddress));
            Object obj = in.readObject();
            if(obj instanceof List<?>)
                ((List<InetSocketAddress>)obj).forEach(peerService::addPeer);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
