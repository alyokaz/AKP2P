package com.alyokaz.akp2p.peerservice;

import com.alyokaz.akp2p.peerservice.exceptions.ContactBeaconException;
import com.alyokaz.akp2p.peerservice.exceptions.DiscoverPeersException;
import com.alyokaz.akp2p.peerservice.exceptions.PingPeerException;
import com.alyokaz.akp2p.peerservice.tasks.DiscoverPeersTask;
import com.alyokaz.akp2p.pingserver.PingServer;
import com.alyokaz.akp2p.server.message.BeaconMessage;
import com.alyokaz.akp2p.server.message.MessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    private static Logger logger = LogManager.getLogger();

    public PeerService() {
    }

    public void discoverPeers() throws PingPeerException {
        Set<Future<?>> futures = new HashSet<>();
        livePeers.forEach(address ->
                futures.add(executor.submit(new DiscoverPeersTask(address, this, serverAddress))));

        futures.forEach(future -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new DiscoverPeersException("Peer discovery failed", e);
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
        } catch (IllegalArgumentException e){
            logger.error("Ping timed out for {}", address);
            return false;
        } catch (IOException e) {
            logger.error("Ping for peer at {} failed", address);
            return false;
        }
    }

    public void contactBeacon(InetSocketAddress serverAddress, InetSocketAddress beaconAddress) {
       try(Socket socket  = new Socket(beaconAddress.getHostName(), beaconAddress.getPort());
           ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
           ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

           out.writeObject(new BeaconMessage(MessageType.REQUEST_PEERS, serverAddress));
           Object obj = in.readObject();
           ((List<InetSocketAddress>) obj).forEach(this::addPeer);

       } catch (IOException | ClassNotFoundException e) {
           throw new ContactBeaconException("Contacting Beacon failed", e);
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
