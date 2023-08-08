package aktorrent;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PieceContainer {

    private final String filename;
    private final int totalPieces;
    private final SortedSet<Piece> pieces;

    private Set<Integer> downloadingPieces;

    public PieceContainer(String filename, int totalPieces) {
        this.filename = filename;
        this.totalPieces = totalPieces;
        this.pieces = new TreeSet<>(Comparator.comparingInt(Piece::getId));
        this.downloadingPieces = new HashSet<>();
    }

    public PieceContainer(String filename, int totalPieces, SortedSet pieces) {
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

    public SortedSet<Piece> getPieces() {
        return this.pieces;
    }

    public boolean complete() {
        return totalPieces == pieces.size();
    }

    public synchronized boolean addPiece(Piece piece) {
        this.downloadingPieces.remove(piece.getId());
        return this.pieces.add(piece);
    }

    public synchronized int requestPiece() {
        if(pieces.size() == totalPieces)
            return -1;

        List<Integer> ids = this.pieces.stream().map(Piece::getId).collect(Collectors.toList());
        List<Integer> candidateIds = IntStream.range(0, totalPieces).filter(i -> !ids.contains(i)).boxed().collect(Collectors.toList());
        int chosenId = candidateIds.get(new Random().nextInt(candidateIds.size()));
        this.downloadingPieces.add(chosenId);
        return chosenId;
    }
}
