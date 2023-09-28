package com.alyokaz.akp2p.server.message;

import java.net.InetSocketAddress;

/**
 * A subclass of {@code Message} used to request an individual {@code Piece} from a peer
 */
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

    /**
     * Returns the filename the requested {@code Piece} belongs to.
     *
     * @return the filename the requested {@code Piece} belongs to
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Returns the {@code ID} of the requested {@code Piece}.
     * @return the {@code ID} of the requested {@code Piece}
     */
    public int getPieceId() {
        return pieceId;
    }
}
