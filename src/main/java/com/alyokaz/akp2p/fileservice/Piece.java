package com.alyokaz.akp2p.fileservice;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * This class a single <i>chunk</i> or <i>piece</i> of a file that can be downloaded interdependently of any other
 * {@code Piece}, allowing a file to be downloaded from multiple peers at once.
 */
public class Piece implements Serializable, Comparable<Piece> {
    private final int id;
    private final byte[] data;

    public Piece(int id, byte[] data) {
        this.id = id;
        this.data = data;
    }

    /**
     * Returns the {@code ID} of this piece.
     *
     * @return the {@code ID} of this piece.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the data in {@code Bytes} that this {@code Piece} contains.
     *
     * @return the data in {@code Bytes} that this {@code Piece} contains.
     */
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
