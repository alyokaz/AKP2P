package com.alyokaz.akp2p.server.message;

import java.net.InetSocketAddress;

public class RequestPieceMessage extends Message {

    private final String filename;

    private final int pieceId;
    private final InetSocketAddress serverAddress;

    public RequestPieceMessage(String filename, int pieceId, InetSocketAddress serverAddress) {
        super(MessageType.REQUEST_PIECE, serverAddress);
        this.filename = filename;
        this.pieceId = pieceId;
        this.serverAddress = serverAddress;
    }

    public String getFilename() {
        return filename;
    }

    public int getPieceId() {
        return pieceId;
    }
}
