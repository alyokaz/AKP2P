package com.alyokaz.akp2p;

import com.alyokaz.akp2p.beacon.Beacon;
import com.alyokaz.akp2p.beacon.BeaconCLI;
import com.alyokaz.akp2p.cli.CLI;
import com.alyokaz.akp2p.fileservice.exceptions.SeedFileException;
import com.alyokaz.akp2p.fileservice.FileInfo;
import com.alyokaz.akp2p.fileservice.FileService;
import com.alyokaz.akp2p.peerservice.PeerService;
import com.alyokaz.akp2p.peerservice.exceptions.ContactBeaconException;
import com.alyokaz.akp2p.pingserver.PingServer;
import com.alyokaz.akp2p.server.NodeServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * Entry point for the system and all functionality.
 *
 * This class contains all construction and initialisation logic in
 * static factory methods as well as a set of methods exposing the
 * functionality of the system, such as seeding or downloading files.
 *
 */
public class AKP2P {
    private final NodeServer server;
    private final PingServer udpServer;
    private final PeerService peerService;
    private final FileService fileService;
    private final InetSocketAddress beaconAddress;
    private static final Logger logger = LogManager.getLogger();

    public AKP2P(NodeServer server, PingServer udpServer, PeerService peerService, FileService fileService,
                 InetSocketAddress beaconAddress) {
        this.server = server;
        this.udpServer = udpServer;
        this.peerService = peerService;
        this.fileService = fileService;
        this.beaconAddress = beaconAddress;
    }

    public AKP2P(NodeServer server, PingServer udpServer, PeerService peerService, FileService fileService) {
        this.server = server;
        this.udpServer = udpServer;
        this.peerService = peerService;
        this.fileService = fileService;
        this.beaconAddress = null;
    }

    /**
     * Makes the file available for download by other peers.
     *
     * @param file  the {@code File} to seed
     * @throws SeedFileException
     */
    public void seedFile(File file) throws SeedFileException {
        fileService.addFile(file);
    }

    /**
     * Attempts to download the file from current known peers.
     *
     * @param fileInfo  the {@code FileInfo} for the file to download.
     */
    public void downloadFile(FileInfo fileInfo) {
        fileService.downloadFileTarget(fileInfo);
    }

    /**
     * Attempts to contact the peer at the given address and if successful adds it to a list of known live peers.
     *
     * @param address  the address of the peer
     * @return {@code true} if peer could be contacted
     */
    public boolean addPeer(InetSocketAddress address) {
        if(address != server.getServerAddress() && peerService.addPeer(address)) {
            peerService.discoverPeers();
            fileService.getConnectedPeersFiles();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Attempts to retrieve a fully downloaded file from the given filename.
     *
     * @param  filename the name of the file
     * @return  {@code Optional} with the requested file
     */
    public Optional<File> getFile(String filename) {
        return fileService.getCompletedFile(filename);
    }

    /**
     * Returns a {@code Set} of addresses for peers that are currently known to be live.
     *
     * @return  a {@code Set} of live peers addresses
     */
    public Set<InetSocketAddress> getLivePeers() {
        return this.peerService.getLivePeers();
    }

    /**
     * Returns a {@code Set} of files available to download from the current network of live peers.
     *
     * @return  a {@code Set<FileInfo>} of files available for download
     */
    public Set<FileInfo> getAvailableFiles() {
        if(beaconAddress != null)
            peerService.contactBeacon(server.getServerAddress(), beaconAddress);
        peerService.discoverPeers();
        fileService.getConnectedPeersFiles();
        return fileService.getFileAddressRegistry().keySet();
    }

    /**
     * Returns the address the system servers are listening on.
     *
     * @return  the address the system servers are listening on.
     */
    public InetSocketAddress getAddress() {
        return this.server.getServerAddress();
    }

    /**
     * Returns a {@code Set} of all peers known to be currently live.
     *
     * @return a {@code Set} of all peers known to be currently live
     */
    public Set<InetSocketAddress> getPeers() {
        return peerService.getPeers();
    }

    //TODO what if file isn't being downloaded?
    /**
     * Returns the download progress of the file with the given name.
     *
     * @param  name the name of the file to get progress for
     * @return  the download progress of the file represented as a {@code double}
     */
    public double getProgressOfDownload(String name) {
        return fileService.getProgress(name);
    }

    /**
     * Returns a {@code Map} of files to peers that host them.
     *
     * @return  a {@code Map} of files to peers that host them
     */
    public Map<FileInfo, Set<InetSocketAddress>> getFileRegistry() {
        return Map.copyOf(fileService.getFileAddressRegistry());
    }

    /**
     * Shutdown the system servers
     */
    public void shutDown() {
        if (server != null) server.shutdown();
        if (udpServer != null) udpServer.shutdown();
    }


    /**
     * Constructs an instance of {@code AKP2P} that attempts to contact a {@code Beacon}
     * node, at the given address, to download peer addresses that have registered with that {@code Beacon}.
     *
     * @param beaconAddress the address of the {@code beacon} node.
     * @return An instance of {@code AKP2P} registered and in contact with a {@code Beacon} node
     */
    public static AKP2P createAndInitialize(InetSocketAddress beaconAddress) {
        logger.atInfo().log("Initialising with Beacon at : " + beaconAddress);
        return init(beaconAddress, 0);
    }

    /**
     * Constructs an instance of {@code AKP2P} with its servers listening on the given {@code Port} and that attempts
     * to contact a {@code Beacon} node, at the given address, to download peer addresses that have registered with
     * that {@code Beacon}.
     *
     * @param port the port number the instances servers will listen on
     * @param beaconAddress the address of a beacon node
     * @return An instance of {@code AKP2P} with its servers listening on the given port and registered and in contact
     * with a {@code Beacon} node
     */
    public static AKP2P createAndInitialize(int port, InetSocketAddress beaconAddress) {
        logger.atInfo().log("Initialising with use defined port at {} and Beacon at {}", port, beaconAddress);
        return init(beaconAddress, port);
    }

    /**
     * Constructs and initialises an instance of AKP2P that does not attempt to contact a {@code Beacon} node.
     * <p>
     * Note this instance will be orphaned from the network until a peer address is supplied to the system.
     *
     * @return An instance of {@code AKP2P} not in contact with a {@code Beacon} node
     */
    public static AKP2P createAndInitializeNoBeacon() {
        logger.atInfo().log("Initialising without Beacon");
        return init(null, 0);
    }

    /**
     * Constructs and initialises an instance of AKP2P with its servers listen on the given port and that does not
     * attempt to contact a {@code Beacon} node.
     * <p>
     * Note this instance will be orphaned from the network until a peer address is supplied to the system.
     *
     * @param port the port number this instances servers will listen on
     * @return An instance of {@code AKP2P} listening on the given port and not in contact with a {@code Beacon} node
     */
    public static AKP2P createAndInitializeNoBeacon(int port) {
        logger.atInfo().log("Initialising with user defined port {}", port);
        return init(null, port);
    }

    private static AKP2P init(InetSocketAddress beaconAddress, int port)  {
        PeerService peerService = new PeerService();
        FileService fileService = new FileService(peerService);

        ServerSocket serverSocket = null;
        DatagramSocket datagramSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            datagramSocket = new DatagramSocket(serverSocket.getLocalPort());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        NodeServer server = new NodeServer(serverSocket, peerService, fileService);
        server.start();

        peerService.addExcluded(server.getServerAddress());
        peerService.setServerAddress(server.getServerAddress());

        PingServer pingServer = new PingServer(datagramSocket);
        pingServer.start();

        if(beaconAddress != null) {
            peerService.contactBeacon(server.getServerAddress(), beaconAddress);
            peerService.discoverPeers();
            fileService.getConnectedPeersFiles();
            return new AKP2P(server, pingServer, peerService, fileService,
                    beaconAddress);
        }

        return new AKP2P(server, pingServer, peerService, fileService);
    }


    /**
     * Main entry point for system.
     * <p>
     * Constructs an {@code AKP2P} node or a {@code Beacon} node depending on supplied arguments.
     * {@code AK2P2} nodes can be started with a {@code Beacon} address or in <i>orphaned</i> mode.
     *
     * @param args startup arguments
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        ArgumentParser.parseArguments(args);
    }


}
