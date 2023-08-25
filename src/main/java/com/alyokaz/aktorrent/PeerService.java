package com.alyokaz.aktorrent;

import com.alyokaz.aktorrent.pingserver.PingServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PeerService {
    private final List<InetSocketAddress> peers = Collections.synchronizedList(new ArrayList<>());
    private final Set<InetSocketAddress> connectedPeers = new HashSet<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public PeerService() {
    }

    public void discoverPeers() {
        Set<Future<?>> futures = new HashSet<>();
        peers.forEach(address -> futures.add(executor.submit(new DiscoverPeersTask(address, peers))));
        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void addPeer(String hostName, int port) {
        InetSocketAddress address = new InetSocketAddress(hostName, port);
        peers.add(address);
        pingPeer(address);
    }

    private void pingPeer(InetSocketAddress address) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] buf = PingServer.PING_PAYLOAD.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address.getAddress(), address.getPort());
            socket.send(packet);

            buf = new byte[PingServer.BUFFER_SIZE];
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);

            String payload = new String(packet.getData(), StandardCharsets.UTF_8).trim();
            if (payload.equals(PingServer.PONG_PAYLOAD)) this.connectedPeers.add(address);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<InetSocketAddress> getConnectedPeers() {
        return this.connectedPeers;
    }

    public List<InetSocketAddress> getPeers() {
        return this.peers;
    }
}
