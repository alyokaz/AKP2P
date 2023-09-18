package com.alyokaz.aktorrent.fileservice;

import com.alyokaz.aktorrent.peerservice.PeerService;
import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;

public class GetConnectedPeersFilesTask implements Runnable {

    private final FileService fileService;
    private final InetSocketAddress address;
    private final PeerService peerService;
    private final static Logger logger = LogManager.getLogger();

    public GetConnectedPeersFilesTask(FileService fileService, InetSocketAddress address, PeerService peerService) {
        this.fileService = fileService;
        this.address = address;
        this.peerService = peerService;
    }


    @Override
    public void run() {
        try (Socket socket = new Socket(address.getHostName(), address.getPort());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(new Message(MessageType.REQUSET_FILE_INFOS, peerService.getServerAddress()));
            Object obj = in.readObject();
            if(obj instanceof Set<?>) {
                Set<FileInfo> remoteFileInfos = (Set<FileInfo>) obj;
                remoteFileInfos.forEach(fileInfo -> fileService.registerFile(fileInfo, address));
            }
        } catch (IOException | ClassNotFoundException e) {
            peerService.removeFromLivePeers(address);
            logger.error("Downloading of file info list from {} failed with {}", address, e.getMessage());
        }
    }
}
