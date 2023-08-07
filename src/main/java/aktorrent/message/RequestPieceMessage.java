package aktorrent.message;

public class RequestPieceMessage extends Message {

    private String filename;

    private int pieceId;

    public RequestPieceMessage(String filename, int pieceId) {
        super(MessageType.REQUEST_PIECE);
        this.filename = filename;
        this.pieceId = pieceId;
    }

    public String getFilename() {
        return filename;
    }

    public int getPieceId() {
        return pieceId;
    }
}
