package aktorrent;

import java.io.Serializable;

public class Piece implements Serializable {
    private int id;
    private byte[] data = new byte[1024];

    private int totalPieces;

    public Piece(int id, byte[] data, int totalPieces) {
        this.id = id;
        this.data = data;
        this.totalPieces = totalPieces;
    }

    public int getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    public int getTotalPieces() {
        return totalPieces;
    }
}
