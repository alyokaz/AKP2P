package com.alyokaz.aktorrent.fileservice;

import com.alyokaz.aktorrent.peerservice.AbstractPeersServiceTask;
import com.alyokaz.aktorrent.peerservice.PeerService;
import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.util.Set;

public class GetConnectedPeersFilesTask extends AbstractPeersServiceTask {

    private final FileService fileService;

    public GetConnectedPeersFilesTask(InetSocketAddress address, PeerService peerService, FileService fileService) {
        super(address, peerService);
        this.fileService = fileService;
    }

    @Override
    protected void process(ObjectInputStream in, ObjectOutputStream out)  {
        try {
            out.writeObject(new Message(MessageType.REQUSET_FILE_INFOS, peerService.getServerAddress()));
            Object obj = in.readObject();
            if(obj instanceof Set<?>) {
                Set<FileInfo> remoteFileInfos = (Set<FileInfo>) obj;
                remoteFileInfos.forEach(fileInfo -> fileService.registerFile(fileInfo, getAddress()));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }
}
