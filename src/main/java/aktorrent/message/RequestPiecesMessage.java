package aktorrent.message;

import java.util.List;

public class RequestPiecesMessage extends Message implements RequestPieceIDs {

    private List<Integer> pieceIDs;

    private String filename;

    public RequestPiecesMessage(String filename, List<Integer> pieceIDs) {
        super(MessageType.REQUEST_PIECES);
        this.filename = filename;
        this.pieceIDs = pieceIDs;
    }
    @Override
    public List<Integer> getIds() {
        return pieceIDs;
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
