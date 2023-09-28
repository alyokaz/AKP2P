package com.alyokaz.akp2p.fileservice;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class represents the necessary metadata to download the related file.
 */
public class FileInfo implements Serializable {

    private final String filename;
    private final int totalPieces;
    private final int size;

    public FileInfo(String filename, int totalPieces, int size) {
        this.filename = filename;
        this.totalPieces = totalPieces;
        this.size = size;
    }

    /**
     * Returns the filename for the file this class represents.
     *
     * @return the filename for the file this class represents
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Returns the total number of {@code Piece}s the file this class represents has been divided into.
     *
     * @return the total number of {@code Piece}s the file this class represents has been divided into
     */
    public int getTotalPieces() {
        return totalPieces;
    }

    /**
     * Returns the size of the file this class represents in {@code bytes}.
     *
     * @return the size of the file this class represents in {@code bytes}
     */
    public int getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo = (FileInfo) o;
        return totalPieces == fileInfo.totalPieces && size == fileInfo.size && filename.equals(fileInfo.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, totalPieces, size);
    }
}
