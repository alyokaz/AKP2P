package com.alyokaz.aktorrent.peerservice;

import com.alyokaz.aktorrent.fileservice.FileService;
import com.alyokaz.aktorrent.pingserver.PingServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PeerService {
    private final List<InetSocketAddress> peers = Collections.synchronizedList(new ArrayList<>());
    private final Set<InetSocketAddress> livePeers = new HashSet<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    public PeerService() {
    }

    public void discoverPeers() {
        Set<Future<?>> futures = new HashSet<>();
        peers.forEach(address -> futures.add(executor.submit(new DiscoverPeersTask(address, this))));
        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public synchronized boolean addPeer(InetSocketAddress address) {
        peers.add(address);
        if(pingPeer(address)) {
            livePeers.add(address);
            return true;
        } else
            return false;
    }

    private boolean pingPeer(InetSocketAddress address) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] buf = PingServer.PING_PAYLOAD.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address.getAddress(), address.getPort());
            socket.send(packet);

            buf = new byte[PingServer.BUFFER_SIZE];
            packet = new DatagramPacket(buf, buf.length);
            socket.setSoTimeout(1000);
            socket.receive(packet);

            String payload = new String(packet.getData(), StandardCharsets.UTF_8).trim();
            return (payload.equals(PingServer.PONG_PAYLOAD));
        } catch (SocketTimeoutException e) {
            System.out.println("Ping timed out for: " + address.getHostName() + ":" + address.getPort());
            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void contactBeacon(InetSocketAddress serverAddress, InetSocketAddress beaconAddress) {
        try {
            executor.submit(new ContactBeaconTask(beaconAddress, this, serverAddress)).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<InetSocketAddress> getLivePeers() {
        return Collections.unmodifiableSet(this.livePeers);
    }

    public List<InetSocketAddress> getPeers() {
        return Collections.unmodifiableList(this.peers);
    }
}
