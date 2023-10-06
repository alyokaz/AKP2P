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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Service class for dealing with peer related logic,
 */
public class PeerService {
    private static final Logger logger = LogManager.getLogger();
    private final Set<InetSocketAddress> peers = Collections.synchronizedSet(new HashSet<>());
    private final Set<InetSocketAddress> livePeers = new HashSet<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Set<InetSocketAddress> excluded = new HashSet<>();
    private InetSocketAddress serverAddress;

    public PeerService() {
    }

    /**
     * Attempts to discover new peers by contacting all known live peers
     * and requesting the address of all known live peers that peer holds.
     *
     * @throws PingPeerException
     */
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

    /**
     * Adds peer to list of live peers if it responds to ping
     *
     * @param address address of peer to add
     * @return true if peer is added to live peers
     */
    public synchronized boolean addPeer(InetSocketAddress address) {
        if (excluded.contains(address) || !peers.add(address))
            return false;
        if (pingPeer(address)) {
            livePeers.add(address);
            peers.remove(address);
            return true;
        } else
            return false;
    }


    /**
     * Attempts to ping the peer at the given address
     *
     * @param address the address of the peer to ping
     * @return {@code true} if peer is successfully pinged.
     */
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
        } catch (IllegalArgumentException e) {
            logger.error("Ping timed out for {}", address);
            return false;
        } catch (IOException e) {
            logger.error("Ping for peer at {} failed", address);
            return false;
        }
    }

    /**
     * Attempts to contact the {@code Beacon} at the supplied address.
     *
     * @param serverAddress the address given to register with the {@code beacon}
     * @param beaconAddress the address of the {@code Beacon}
     */
    public void contactBeacon(InetSocketAddress serverAddress, InetSocketAddress beaconAddress) {
        try (Socket socket = new Socket(beaconAddress.getHostName(), beaconAddress.getPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(new BeaconMessage(MessageType.REQUEST_PEERS, serverAddress));
            Object obj = in.readObject();
            ((List<InetSocketAddress>) obj).forEach(this::addPeer);

        } catch (IOException | ClassNotFoundException e) {
            throw new ContactBeaconException("Contacting Beacon failed", e);
        }
    }

    /**
     * {@return a {@code Set} of addresses of peers currently know to be live}
     */
    public Set<InetSocketAddress> getLivePeers() {
        return Collections.unmodifiableSet(this.livePeers);
    }

    /**
     * {@return a {@code Set} of peer addresses not currently know to be live}
     */
    public Set<InetSocketAddress> getPeers() {
        return Collections.unmodifiableSet(this.peers);
    }

    /**
     * Removes the peers at the given address from the {@code Set} of peers
     * currently known to be live.
     *
     * @param address - address of peers to be removed
     */
    public void removeFromLivePeers(InetSocketAddress address) {
        livePeers.remove(address);
        peers.add(address);
    }

    /**
     * Adds peers at the given address to a {@code Set} of address that
     * will not be attempted to be contacted by this instance.
     *
     * @param address address of peer to ping.
     * @return {@code true} if peer is not already excluded.
     */
    public boolean addExcluded(InetSocketAddress address) {
        return excluded.add(address);
    }

    /**
     * Removes the peer at the given address from a {@code Set} of address
     * that will not be attempted to be contacted by this instance.
     *
     * @param address - address for peers to be removed
     * @return {@code true} if peer was removed
     */
    public boolean removeExcluded(InetSocketAddress address) {
        return excluded.remove(address);
    }

    //TODO should this be private?

    /**
     * {@return the address for the servers of this instance.}
     */
    public InetSocketAddress getServerAddress() {
        return serverAddress;
    }

    /**
     * Sets the address of the servers for this instance.
     *
     * @param serverAddress - the address of the servers.
     */
    public void setServerAddress(InetSocketAddress serverAddress) {
        this.serverAddress = serverAddress;
    }
}
