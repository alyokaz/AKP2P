package com.alyokaz.akp2p.fileservice;

import com.alyokaz.akp2p.fileservice.exceptions.BuildFileException;
import com.alyokaz.akp2p.fileservice.exceptions.GetPeersFileInfoException;
import com.alyokaz.akp2p.fileservice.exceptions.SeedFileException;
import com.alyokaz.akp2p.fileservice.tasks.DownloadFileTargetTask;
import com.alyokaz.akp2p.fileservice.tasks.GetConnectedPeersFilesTask;
import com.alyokaz.akp2p.peerservice.PeerService;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;


public class FileService {

    public static final int BUFFER_SIZE = 1000000;

    private final Map<String, PieceContainer> files = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, File> completedFiles = new HashMap<>();
    private final Set<FileInfo> availableFiles = Collections.synchronizedSet(new HashSet<>());
    private final PeerService peerService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final Map<FileInfo, Set<InetSocketAddress>> fileAddressRegistry = new ConcurrentHashMap<>();

    public FileService(PeerService peerService) {
        this.peerService = peerService;
    }
    public Map<String, PieceContainer> getFiles() {
        return this.files;
    }

    public void addFile(File file) throws SeedFileException {
        this.files.put(file.getName(), buildPieceContainer(file));
    }

    public Optional<File> getCompletedFile(String filename) {
        if(completedFiles.containsKey(filename))
            return Optional.of(completedFiles.get(filename));
        else
            return Optional.empty();
    }

    public Set<FileInfo> getAvailableFiles() {
        return Set.copyOf(availableFiles);
    }

    public PieceContainer getFile(String filename) {
        return this.files.get(filename);
    }

    public synchronized void buildFile(PieceContainer container)  {
        if (completedFiles.containsKey(container.getFilename()))
            return;

        File outputFile = new File(container.getFilename());
        try {
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
        } catch (IOException e) {
            throw new BuildFileException("Building file" + container.getFilename() + " failed with " + e.getMessage(), e);
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
                futures.add(executor.submit(new GetConnectedPeersFilesTask(this, address, peerService))));
        futures.forEach(f -> {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new GetPeersFileInfoException("Getting File Info from connected peers failed", e);
            }
        });

    }

    public Map<FileInfo, Set<InetSocketAddress>> getFileAddressRegistry() {
        return fileAddressRegistry;
    }

    public void registerFile(FileInfo fileInfo, InetSocketAddress address) {
        fileAddressRegistry.putIfAbsent(fileInfo, Collections.synchronizedSet(new HashSet<>()));
        fileAddressRegistry.get(fileInfo).add(address);
    }

    public void downloadFileTarget(FileInfo fileInfo) {
        if(fileAddressRegistry.containsKey(fileInfo)) {
            files.put(fileInfo.getFilename(), new PieceContainer(fileInfo));
            fileAddressRegistry.get(fileInfo).forEach(address ->
                    executor.execute(new DownloadFileTargetTask(address, this, peerService, fileInfo)));
        }
    }


    public double getProgress(String name) {
        PieceContainer container = files.get(name);
        return container.getPieces().size() / (double) container.getTotalPieces();

    }
}
