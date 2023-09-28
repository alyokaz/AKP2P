package com.alyokaz.akp2p.fileservice;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class contains and coordinates, in terms of downloading, the {@code Piece}s for a single file.
 */
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

    /**
     * Returns the filename of the file this class contains the {@code Piece}s for.
     *
     * @return the filename of the file this class contains the {@code Piece}s for.
     */
    public String getFilename() {
        return this.fileInfo.getFilename();
    }

    /**
     * Returns the total number of {@code Piece}s the file related to this class has been divided into.
     *
     * @return the total number of {@code Piece}s the file related to this class has been divided into.
     */
    public int getTotalPieces() {
        return this.fileInfo.getTotalPieces();
    }

    /**
     * Returns a {@code SortedSet} of the {@code Piece}s this class contains.
     *
     * @return a {@code SortedSet} of the {@code Piece}s this class contains.
     */
    public SortedSet<Piece> getPieces() {
        return this.pieces;
    }

    /**
     * Returns true if all the {@code Piece}s for the file related to this class have been downloaded.
     *
     * @return true if all the {@code Piece}s for the file related to this class have been downloaded.
     */
    public boolean complete() {
        return this.fileInfo.getTotalPieces() == pieces.size();
    }

    /**
     * Add a downloaded {@code Piece} to this container.
     *
     * @param piece the {@code Piece} to be added
     */
    public synchronized void addPiece(Piece piece) {
        this.downloadingPieces.remove(piece.getId());
        this.pieces.add(piece);
    }

    /**
     * Returns an {@code ID} of a {@code Piece} that has neither already been assigned for download nor has already
     * been downloaded.
     *
     * @return an {@code ID} of a {@code Piece} that has neither already been assigned for download nor has already
     * been downloaded.
     */
    public synchronized int requestPiece() {
        if(pieces.size() == this.fileInfo.getTotalPieces())
            return -1;

        List<Integer> ids = this.pieces.stream().map(Piece::getId).collect(Collectors.toList());
        List<Integer> candidateIds = IntStream.range(0, this.fileInfo.getTotalPieces())
                .filter(i -> !ids.contains(i)).boxed().collect(Collectors.toList());
        int chosenId = candidateIds.get(new Random().nextInt(candidateIds.size()));
        this.downloadingPieces.add(chosenId);
        return chosenId;
    }

    /**
     * Returns the {@code FileInfo} for the file related to this {@code PieceContainer}.
     *
     * @return the {@code FileInfo} for the file related to this {@code PieceContainer}
     */
    public FileInfo getFileInfo() {
        return this.fileInfo;
    }
}
