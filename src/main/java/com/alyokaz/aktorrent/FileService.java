package com.alyokaz.aktorrent;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

public class FileService {

    private final Map<String, PieceContainer> files = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, File> completedFiles = new HashMap<>();
    private final Set<FileInfo> availableFiles = Collections.synchronizedSet(new HashSet<>());
    private final PeerService peerService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public FileService(PeerService peerService) {
        this.peerService = peerService;
    }

    public Map<String, PieceContainer> getFiles() {
        return this.files;
    }

    public Map<String, File> getCompletedFiles() {
        return this.completedFiles;
    }

    public void addFile(File file) {
        this.files.put(file.getName(), buildPieceContainer(file));
        this.availableFiles.add(getFileInfo(file));
    }

    public void addFile(FileInfo fileInfo) {
        files.put(fileInfo.getFilename(), new PieceContainer(fileInfo));
    }

    public File getCompletedFile(String filename) {
        return this.completedFiles.get(filename);
    }

    public Set<FileInfo> getAvailableFiles() {
        updateAvailableFiles();
        return availableFiles;
    }

    public void updateAvailableFiles() {
        Set<Future<Set<FileInfo>>> futures = new HashSet<>();
        this.peerService.getPeers().forEach(address -> futures.add(executor.submit(new GetAvailableFilesTask(address))));
        futures.forEach(f -> {
            try {
                availableFiles.addAll(f.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public PieceContainer getFile(String filename) {
        return this.files.get(filename);
    }

    public synchronized void buildFile(PieceContainer container) throws IOException {
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
            return new PieceContainer(getFileInfo(file), pieces);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static FileInfo getFileInfo(File file) {
        return new FileInfo(file.getName(), getNoOfPieces(file), (int) file.length());
    }

}
