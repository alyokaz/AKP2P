package com.alyokaz.aktorrent.peerservice;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

public abstract class AbstractPeersServiceTask implements Runnable {

    private final InetSocketAddress address;
    protected final PeerService peerService;
    private final static Logger logger = LogManager.getLogger();

    public AbstractPeersServiceTask(InetSocketAddress address, PeerService peerService) {
        this.address = address;
        this.peerService = peerService;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(address.getHostName(), address.getPort());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            process(in, out);
        } catch (ConnectException e) {
            logger.error("Could not connect to peer at {}", address);
            peerService.removeFromLivePeers(address);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    abstract void process(ObjectInputStream in, ObjectOutputStream out);
}
