package aktorrent;

import java.util.Set;

public class PieceContainer {

    private String filename;
    private int totalPieces;
    private Set<Piece> pieces;

    public PieceContainer(String filename, int totalPieces, Set<Piece> pieces) {
        this.filename = filename;
        this.totalPieces = totalPieces;
        this.pieces = pieces;
    }

    public String getFilename() {
        return filename;
    }

    public int getTotalPieces() {
        return totalPieces;
    }

    public Set<Piece> getPieces() {
        return pieces;
    }
}
