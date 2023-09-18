package com.alyokaz.aktorrent;

import com.alyokaz.aktorrent.cli.CLI;
import com.alyokaz.aktorrent.fileservice.exceptions.SeedFileException;
import com.alyokaz.aktorrent.fileservice.FileInfo;
import com.alyokaz.aktorrent.fileservice.FileService;
import com.alyokaz.aktorrent.peerservice.PeerService;
import com.alyokaz.aktorrent.pingserver.PingServer;
import com.alyokaz.aktorrent.server.NodeServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.*;


//TODO This class should be refactored to get rid of the null argument for
// beacon address. It seems a lot of the logic that requires communication between
// the services could be simplified by pulling it up into this class.
public class AKTorrent {
    private final NodeServer server;
    private final PingServer udpServer;
    private final PeerService peerService;
    private final FileService fileService;
    private final InetSocketAddress beaconAddress;
    private static final Logger logger = LogManager.getLogger();
    public AKTorrent(NodeServer server, PingServer udpServer, PeerService peerService, FileService fileService,
                     InetSocketAddress beaconAddress) {
        this.server = server;
        this.udpServer = udpServer;
        this.peerService = peerService;
        this.fileService = fileService;
        this.beaconAddress = beaconAddress;
    }

    public AKTorrent(NodeServer server, PingServer udpServer, PeerService peerService, FileService fileService) {
        this.server = server;
        this.udpServer = udpServer;
        this.peerService = peerService;
        this.fileService = fileService;
        this.beaconAddress = null;
    }

    public void seedFile(File file) throws SeedFileException {
        fileService.addFile(file);
    }

    public void downloadFile(FileInfo fileInfo) {
        fileService.downloadFileTarget(fileInfo);
    }

    public boolean addPeer(InetSocketAddress address) {
        if(address != server.getServerAddress() && peerService.addPeer(address)) {
            peerService.discoverPeers();
            fileService.getConnectedPeersFiles();
            return true;
        } else {
            return false;
        }
    }

    public Optional<File> getFile(String filename) {
        return fileService.getCompletedFile(filename);
    }

    public void shutDown() {
        if (server != null) server.shutdown();
        if (udpServer != null) udpServer.shutdown();
    }

    public Set<InetSocketAddress> getLivePeers() {
        return this.peerService.getLivePeers();
    }

    public Set<FileInfo> getAvailableFiles() {
        if(beaconAddress != null)
            peerService.contactBeacon(server.getServerAddress(), beaconAddress);
        peerService.discoverPeers();
        fileService.getConnectedPeersFiles();
        return fileService.getFileAddressRegistry().keySet();
    }

    public InetSocketAddress getAddress() {
        return this.server.getServerAddress();
    }

    public Map<FileInfo, Set<InetSocketAddress>> getFileRegistry() {
        return Map.copyOf(fileService.getFileAddressRegistry());
    }


    public static AKTorrent createAndInitialize(InetSocketAddress beaconAddress) {
        logger.atInfo().log("Initialising with Beacon at : " + beaconAddress);
        return init(beaconAddress);
    }

    public static AKTorrent createAndInitializeNoBeacon() {
        logger.atInfo().log("Initialising without Beacon");
        return init(null);
    }

    private static AKTorrent init(InetSocketAddress beaconAddress)  {
        PeerService peerService = new PeerService();
        FileService fileService = new FileService(peerService);

        ServerSocket serverSocket = null;
        DatagramSocket datagramSocket = null;
        try {
            serverSocket = new ServerSocket(0);
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
            return new AKTorrent(server, pingServer, peerService, fileService,
                    beaconAddress);
        }

        return new AKTorrent(server, pingServer, peerService, fileService);
    }


    public static void main(String[] args) throws IOException {
        InetSocketAddress beaconAddress =
                new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        AKTorrent node = AKTorrent.createAndInitialize(beaconAddress);
        CLI cli = new CLI(System.in, System.out, node);
        cli.start();
    }

    public Set<InetSocketAddress> getPeers() {
        return peerService.getPeers();
    }
}
