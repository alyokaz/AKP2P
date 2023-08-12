package aktorrent;

import java.io.*;
import java.util.*;
import java.util.stream.IntStream;

public class FileUtils {

    static synchronized void  buildFile(PieceContainer container, Map<String, File> completedFiles) throws IOException {
        if (completedFiles.containsKey(container.getFilename()))
            return;

        File outputFile = new File(container.getFilename());

        outputFile.createNewFile();

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            container.getPieces().stream().sorted(Comparator.comparing(Piece::getId)).forEach(p -> {
                try {
                    out.write(p.getData());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            completedFiles.put(outputFile.getName(), outputFile);
        }
    }

    private static int getNoOfPieces(File file) {
        int numberOfPieces = (int) file.length() / AKTorrent.BUFFER_SIZE;
        if ((file.length() % AKTorrent.BUFFER_SIZE) != 0) {
            numberOfPieces++;
        }
        return numberOfPieces;
    }

    public static PieceContainer buildPieceContainer(File file) {
        SortedSet<Piece> pieces = new TreeSet<>();
        int numberOfPieces = getNoOfPieces(file);

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[AKTorrent.BUFFER_SIZE];
            IntStream.range(0, numberOfPieces).forEach(i -> {
                try {
                    int bytesRead = in.read(buffer);
                    pieces.add(new Piece(i,
                            // make last Piece correct length
                            bytesRead < AKTorrent.BUFFER_SIZE ? Arrays.copyOf(buffer, bytesRead) : buffer.clone()
                    ));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return new PieceContainer(FileUtils.getFileInfo(file), pieces);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FileInfo getFileInfo(File file) {
        return new FileInfo(file.getName(), getNoOfPieces(file), (int) file.length());
    }
}
