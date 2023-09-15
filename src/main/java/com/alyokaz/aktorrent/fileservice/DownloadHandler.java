package com.alyokaz.aktorrent.fileservice;

import com.alyokaz.aktorrent.peerservice.PeerService;
import com.alyokaz.aktorrent.server.message.Message;
import com.alyokaz.aktorrent.server.message.MessageType;
import com.alyokaz.aktorrent.server.message.RequestPieceMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormattedMessage;

import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;

public class DownloadHandler extends AbstractDownloadHandler{


    public DownloadHandler(InetSocketAddress address, FileService fileService, PeerService peerService) {
        super(address, fileService, peerService);
    }

    @Override
    protected void process(ObjectOutputStream out, ObjectInputStream in) throws IOException, ClassNotFoundException {
        // request names of available files from peer
        out.writeObject(new Message(MessageType.REQUEST_FILENAMES, peerService.getServerAddress()));
        Set<String> filenames = (Set<String>) in.readObject();

        // check if we are interested in any of the available files and begin download
        filenames.stream()
                .filter(fileService.getFiles()::containsKey)
                .forEach(filename -> downloadPieces(filename, out, in));

        // signal to peer to close connection as we are finished
        out.writeObject(new Message(MessageType.END, peerService.getServerAddress()));
    }


}
