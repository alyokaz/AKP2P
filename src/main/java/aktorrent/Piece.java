package aktorrent;

import java.io.Serializable;
import java.security.PrivateKey;

public class Piece implements Serializable {
    private int id;
    private byte[] data;

    private int totalPieces;

    private Status status;

    public Piece(int id, byte[] data, int totalPieces) {
        this.id = id;
        this.data = data;
        this.totalPieces = totalPieces;
        this.status = Status.EMPTY;
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

    public void setStatus(Status status) {
        this.status = status;
    }

    public enum Status {
        DOWNLOADING, EMPTY, COMPLETE
    }

    public Status getStatus() {
        return this.status;
    }
}
