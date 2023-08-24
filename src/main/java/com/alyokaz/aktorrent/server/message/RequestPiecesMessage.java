package com.alyokaz.aktorrent.server.message;

import java.util.List;

public class RequestPiecesMessage extends Message implements RequestPieceIDs {

    private final List<Integer> pieceIDs;

    private final String filename;

    public RequestPiecesMessage(String filename, List<Integer> pieceIDs) {
        super(MessageType.REQUEST_PIECES);
        this.filename = filename;
        this.pieceIDs = pieceIDs;
    }

}
