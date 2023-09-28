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

/**
 * Service class for dealing with file related logic.
 */
public class FileService {

    public static final int BUFFER_SIZE = 1000000;

    private final Map<String, PieceContainer> files = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, File> completedFiles = new HashMap<>();
    private final Set<FileInfo> availableFiles = Collections.synchronizedSet(new HashSet<>());
    private final PeerService peerService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final Map<FileInfo, Set<InetSocketAddress>> fileAddressRegistry = new ConcurrentHashMap<>();

    /**
     * Construct a new {@code FileService} with the given {@code PeerService}.
     * @param peerService
     */
    public FileService(PeerService peerService) {
        this.peerService = peerService;
    }

    /**
     * Returns a {@code Map} of filename to {@code PieceContainer}s
     * @return a {@code Map} of filenames to {@code PieceContainer}
     */
    public Map<String, PieceContainer> getFiles() {
        return this.files;
    }

    /**
     * Makes a {@code File} for available for download by other peers.
     * @param file - the {@code File} to be prepared for download
     * @throws SeedFileException
     */
    public void addFile(File file) throws SeedFileException {
        this.files.put(file.getName(), buildPieceContainer(file));
    }

    /**
     * {@return an {@code Optional} that represents the presence of
     * a fully downloaded file with the given name.}
     * @param filename the name of the file to attempt to find
     */
    public Optional<File> getCompletedFile(String filename) {
        if(completedFiles.containsKey(filename))
            return Optional.of(completedFiles.get(filename));
        else
            return Optional.empty();
    }

    /**
     * Return a {@code Set} of {@code FileInfo}s representing the files that are available to be downloaded
     * from the current known live peers.
     *
     * @return a {@code Set} of {@code FileInfo}s representing the files that are available to be downloaded
     * from the current known live peers.
     */
    public Set<FileInfo> getAvailableFiles() {
        return Set.copyOf(availableFiles);
    }

    /**
     * Returns the {@code PieceContainer} for the file from the {@code Set} of
     * files awaiting to be, or are partially, downloaded.
     *
     * @param filename  the filename under which {@code PieceContainer} is stored.
     * @return the {@code PieceContainer} for the file from the {@code Set} of
     * files awaiting to be, or are partially, downloaded.
     */
    public PieceContainer getFile(String filename) {
        return this.files.get(filename);
    }

    /**
     * Build and outputs a {@code File} to local file system for the given completed {@code PieceContainer}.
     *
     * @param container the completed {@code PieceContainer} of the {@code File} to be built.
     */
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

    /**
     * Finds the number of {@code Piece}s a {@code File} can be divided into.
     *
     * @param file  the {@code File} to be divided into {@code Piece}s
     * @return  the number of {@code Piece}s the {@codeFile} can be divided
     * into.
     */
    private static int getNoOfPieces(File file) {
        int numberOfPieces = (int) file.length() / BUFFER_SIZE;
        if ((file.length() % BUFFER_SIZE) != 0) {
            numberOfPieces++;
        }
        return numberOfPieces;
    }

    /**
     * Builds a {@code PieceContainer} for the given {@code File}.
     *
     * @param file  the {@code File} for which the {@code PieceContainer} will be built.
     * @return  a {@code PieceContainer} built from the given {@code File}
     * @throws SeedFileException
     */
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

    /**
     * Returns a {@code FileInfo} for the given {@code File}.
     *
     * @return  a {@code FileInfo} for the given {@code File}.
     * @param file  the {@code File} for which the {@code FileInfo} will be built.
     */
    public static FileInfo getFileInfo(File file) {
        return new FileInfo(file.getName(), getNoOfPieces(file), (int) file.length());
    }

    /**
     * Contacts each of the known live peers and requests a list of all the files they are currently seeding.
     */
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

    /**
     * Returns a {@code Map} representing a registry of files to peers that are seeding them.
     * @return a {@code Map} representing a registry of files to peers that are seeding them.
     */
    public Map<FileInfo, Set<InetSocketAddress>> getFileAddressRegistry() {
        return fileAddressRegistry;
    }

    /**
     * Places the given peer address into a register of file to peer addresses.
     *
     * @param fileInfo  the key under which the address will be registered.
     * @param address  the address to be registered.
     */
    public void registerFile(FileInfo fileInfo, InetSocketAddress address) {
        fileAddressRegistry.putIfAbsent(fileInfo, Collections.synchronizedSet(new HashSet<>()));
        fileAddressRegistry.get(fileInfo).add(address);
    }

    /**
     * Downloads the file for the given {@code FileInfo}.
     * <p>
     * Each known live peer from the register of file to seeding peer addresses will be sent a request for download.
     *
     * @param fileInfo  the {@code FileInfo} for the file to be downloaded.
     */
    public void downloadFileTarget(FileInfo fileInfo) {
        if(fileAddressRegistry.containsKey(fileInfo)) {
            files.put(fileInfo.getFilename(), new PieceContainer(fileInfo));
            fileAddressRegistry.get(fileInfo).forEach(address ->
                    executor.execute(new DownloadFileTargetTask(address, this, peerService, fileInfo)));
        }
    }


    /**
     * Returns the current download progress of the file for the given name.
     *
     * @param name  the name of the file for which the progress will be returned.
     * @return  a {@code double} representing the current download progress of the file.
     */
    public double getProgress(String name) {
        PieceContainer container = files.get(name);
        return container.getPieces().size() / (double) container.getTotalPieces();

    }
}
