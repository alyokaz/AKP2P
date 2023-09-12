package com.alyokaz.aktorrent.peerservice;

import com.alyokaz.aktorrent.fileservice.FileService;
import com.alyokaz.aktorrent.pingserver.PingServer;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class PeerService {
    private final Set<InetSocketAddress> peers = Collections.synchronizedSet(new HashSet<>());
    private final Set<InetSocketAddress> livePeers = new HashSet<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private Set<InetSocketAddress> excluded = new HashSet<>();
    private InetSocketAddress serverAddress;

    public PeerService() {
    }

    public void discoverPeers() {
        Set<Future<?>> futures = new HashSet<>();
        livePeers.forEach(address ->
                futures.add(executor.submit(new DiscoverPeersTask(address, this, serverAddress))));

        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public synchronized boolean addPeer(InetSocketAddress address) {
        if(excluded.contains(address) || !peers.add(address))
            return false;
        if(pingPeer(address)) {
            livePeers.add(address);
            peers.remove(address);
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
            System.out.println("Ping timed out for: " + address.getHostName()
                    + ":" + address.getPort());
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

    public Set<InetSocketAddress> getPeers() {
        return Collections.unmodifiableSet(this.peers);
    }

    public void removeFromLivePeers(InetSocketAddress address) {
        livePeers.remove(address);
        peers.add(address);
    }

    public boolean addExcluded(InetSocketAddress address) {
        return excluded.add(address);
    }

    public boolean removeExcluded(InetSocketAddress address) {
        return excluded.remove(address);
    }

    public void setServerAddress(InetSocketAddress serverAddress) {
        this.serverAddress = serverAddress;
    }

    public InetSocketAddress getServerAddress() {
        return serverAddress;
    }
}
