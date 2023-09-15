package com.alyokaz.aktorrent.fileservice;

import com.alyokaz.aktorrent.peerservice.PeerService;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;


public class FileService {

    public static final int BUFFER_SIZE = 1000000;

    private final Map<String, PieceContainer> files = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, File> completedFiles = new HashMap<>();
    private final Set<FileInfo> availableFiles = Collections.synchronizedSet(new HashSet<>());
    private final PeerService peerService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final Map<FileInfo, Set<InetSocketAddress>> fileAddressRegistry = new HashMap<>();

    public FileService(PeerService peerService) {
        this.peerService = peerService;
    }
    public Map<String, PieceContainer> getFiles() {
        return this.files;
    }

    public void addFile(File file) throws SeedFileException {
        this.files.put(file.getName(), buildPieceContainer(file));
        this.availableFiles.add(getFileInfo(file));
    }

    public void addFile(FileInfo fileInfo) {
        files.put(fileInfo.getFilename(), new PieceContainer(fileInfo));
    }

    public Optional<File> getCompletedFile(String filename) {
        if(completedFiles.containsKey(filename))
            return Optional.of(completedFiles.get(filename));
        else
            return Optional.empty();
    }

    public Set<FileInfo> updateAndGetAvailableFiles() {
        peerService.discoverPeers();
        updateAvailableFiles();
        return availableFiles;
    }

    public Set<FileInfo> getAvailableFiles() {
        return Set.copyOf(availableFiles);
    }

    public void updateAvailableFiles() {
        Set<Future<Set<FileInfo>>> futures = new HashSet<>();
        this.peerService.getLivePeers().forEach(address ->
                futures.add(executor.submit(new GetAvailableFilesTask(address,
                        peerService.getServerAddress()))));
        futures.forEach(f -> {
            try {
                availableFiles.addAll(f.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }
    public void downloadAllFiles() {
        peerService.discoverPeers();
        //TODO do we need to signal that a download is or is not in progress for a quick return from the getFile method?
        executor.execute(() -> this.peerService.getLivePeers().forEach(address ->
                executor.execute(new DownloadHandler(address, this, peerService))));
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
            container.getPieces().stream()
                    .sorted(Comparator.comparing(Piece::getId))
                    .forEach(p -> {
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
        int numberOfPieces = (int) file.length() / BUFFER_SIZE;
        if ((file.length() % BUFFER_SIZE) != 0) {
            numberOfPieces++;
        }
        return numberOfPieces;
    }

    public static PieceContainer buildPieceContainer(File file) throws SeedFileException {
        SortedSet<Piece> pieces = new TreeSet<>();
        int numberOfPieces = getNoOfPieces(file);
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            IntStream.range(0, numberOfPieces).forEach(i -> {
                try {
                    int bytesRead = in.read(buffer);
                    pieces.add(new Piece(i,
                            // make last Piece correct length
                            bytesRead < BUFFER_SIZE ? Arrays.copyOf(buffer, bytesRead) : buffer.clone()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            return new PieceContainer(getFileInfo(file), pieces);
        } catch (IOException | UncheckedIOException e) {
            throw new SeedFileException("Seeding file " + file.getName() + " failed", e);
        }
    }

    public static FileInfo getFileInfo(File file) {
        return new FileInfo(file.getName(), getNoOfPieces(file), (int) file.length());
    }

    public void getConnectedPeersFiles() {
        Set<Future> futures = new HashSet<>();
        peerService.getLivePeers().forEach(address ->
                futures.add(executor.submit(new GetConnectedPeersFilesTask(address, peerService, this))));
        futures.forEach(f -> {
            try {
                f.get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

    }

    public Map<FileInfo, Set<InetSocketAddress>> getFileAddressRegistry() {
        return fileAddressRegistry;
    }

    public void registerFile(FileInfo fileInfo, InetSocketAddress address) {
        if(fileAddressRegistry.containsKey(fileInfo))
            fileAddressRegistry.get(fileInfo).add(address);
        else
            fileAddressRegistry.put(fileInfo, Set.of(address));
    }

    public void downloadFileTarget(FileInfo fileInfo) {
        if(fileAddressRegistry.containsKey(fileInfo)) {
            addFile(fileInfo);
            fileAddressRegistry.get(fileInfo).forEach(address ->
                    executor.execute(new DownloadFileTargetTask(address, this, peerService, fileInfo)));
        }
    }


}
