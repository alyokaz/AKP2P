package com.alyokaz.aktorrent.fileservice;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PieceContainer {


    private final SortedSet<Piece> pieces;

    private final Set<Integer> downloadingPieces;

    private final FileInfo fileInfo;

    public PieceContainer(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
        this.pieces = new TreeSet<>(Comparator.comparingInt(Piece::getId));
        this.downloadingPieces = new HashSet<>();
    }

    public PieceContainer(FileInfo fileInfo, SortedSet<Piece> pieces) {
        this.fileInfo = fileInfo;
        this.pieces = pieces;
        this.downloadingPieces = new HashSet<>();
    }

    public String getFilename() {
        return this.fileInfo.getFilename();
    }

    public int getTotalPieces() {
        return this.fileInfo.getTotalPieces();
    }

    public SortedSet<Piece> getPieces() {
        return this.pieces;
    }

    public boolean complete() {
        return this.fileInfo.getTotalPieces() == pieces.size();
    }

    public synchronized void addPiece(Piece piece) {
        this.downloadingPieces.remove(piece.getId());
        this.pieces.add(piece);
    }

    public synchronized int requestPiece() {
        if(pieces.size() == this.fileInfo.getTotalPieces())
            return -1;

        List<Integer> ids = this.pieces.stream().map(Piece::getId).collect(Collectors.toList());
        List<Integer> candidateIds = IntStream.range(0, this.fileInfo.getTotalPieces()).filter(i -> !ids.contains(i)).boxed().collect(Collectors.toList());
        int chosenId = candidateIds.get(new Random().nextInt(candidateIds.size()));
        this.downloadingPieces.add(chosenId);
        return chosenId;
    }
}
