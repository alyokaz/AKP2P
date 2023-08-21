package aktorrent;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

public class CLI {

    private final InputStream inputStream;

    private final PrintStream outputStream;

    private final AKTorrent node;

    public static final String WELCOME_MESSAGE = "Welcome to AKTorrent";
    public static final String MAIN_MENU = "1: seed, 2: download, 3: see files";

    public static final String INPUT_PROMPT = "Input Path:";

    public static final String DOWNLOAD_INPUT_PROMPT = "Input file number: ";

    public static final String NOT_A_NUMBER_ERROR = "Please select a menu number.";


    public CLI(InputStream inputStream, PrintStream outputStream, AKTorrent node) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.node = node;
    }

    public void start() throws IOException {
        outputStream.println(WELCOME_MESSAGE);
        outputStream.println(MAIN_MENU);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while((line = reader.readLine()) != null) {
            String[] tokens = line.split(" ");
            Integer selection;
            try {
                selection = Integer.parseInt(tokens[0]);
                switch(selection) {
                    case 1 -> processSeedFile(reader, outputStream);
                    case 2 -> processDownloadFile(reader, outputStream);
                    case 3 -> processDisplayFiles(reader, outputStream);
                }
            } catch (NumberFormatException e) {
              outputStream.println(NOT_A_NUMBER_ERROR);
            }
            outputStream.println(MAIN_MENU);
        }
        node.shutDown();
    }

    private void processDisplayFiles(BufferedReader reader, PrintStream outputStream) throws IOException {
        try {
            List<FileInfo> files = new ArrayList<>(node.getAvailableFiles().get());
            IntStream.range(0, files.size()).forEach(i -> outputStream.println((i + 1) + ": " + files.get(i).getFilename()));
            outputStream.println(DOWNLOAD_INPUT_PROMPT);
            String line = reader.readLine();
            if(line == null)
                return;
            int fileNumber = Integer.parseInt(line) - 1;
            node.downloadFile(files.get(fileNumber));
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void processDownloadFile(BufferedReader reader, PrintStream out) throws IOException {
        out.println(INPUT_PROMPT);
        String filename = reader.readLine().trim();
        //TODO Temp for test: FileInfo will have to be supplied at command line or derived from filename
        File file = new File(AKTorrent.class.getResource("/" + filename).getFile());
        node.downloadFile(FileUtils.getFileInfo(file));
    }

    private void processSeedFile(BufferedReader reader, PrintStream out) throws IOException {
        out.println(INPUT_PROMPT);
        String filename = reader.readLine().trim();
        File file = new File(AKTorrent.class.getResource("/" + filename).getFile());
        node.seedFile(file);
    }
}
