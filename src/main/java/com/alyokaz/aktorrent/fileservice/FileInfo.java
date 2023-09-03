package com.alyokaz.aktorrent.fileservice;

import java.io.Serializable;
import java.util.Objects;

public class FileInfo implements Serializable {

    private final String filename;
    private final int totalPieces;
    private final int size;

    public FileInfo(String filename, int totalPieces, int size) {
        this.filename = filename;
        this.totalPieces = totalPieces;
        this.size = size;
    }

    public String getFilename() {
        return filename;
    }

    public int getTotalPieces() {
        return totalPieces;
    }

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
