package com.alyokaz.aktorrent;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class Piece implements Serializable, Comparable<Piece> {
    private final int id;
    private final byte[] data;

    public Piece(int id, byte[] data) {
        this.id = id;
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Piece piece = (Piece) o;
        return id == piece.id && Arrays.equals(data, piece.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public int compareTo(Piece other) {
        return this.id - other.getId();
    }
}
