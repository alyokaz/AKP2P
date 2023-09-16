package com.alyokaz.aktorrent.fileservice;

import com.alyokaz.aktorrent.peerservice.PeerService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;

public class DownloadFileTargetTask extends AbstractDownloadHandler {

    private final FileInfo fileInfo;

    public DownloadFileTargetTask(InetSocketAddress address, FileService fileService, PeerService peerService,
                                  FileInfo fileInfo) {
        super(address, fileService, peerService);
        this.fileInfo = fileInfo;
    }

    @Override
    protected void process(ObjectOutputStream out, ObjectInputStream in) {
            downloadPieces(fileInfo.getFilename(), out, in);
    }
}
