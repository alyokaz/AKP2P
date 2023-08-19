package aktorrent;

import java.io.*;

public class CLI {

    private final InputStream inputStream;

    private final PrintStream outputStream;

    private final AKTorrent node;

    public static final String WELCOME_MESSAGE = "Welcome to AKTorrent";
    public static final String MAIN_MENU = "1: seed, 2: download, 3: see files";

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
            Integer selection = Integer.parseInt(tokens[0]);
            switch(selection) {
                case 1 -> processSeedFile(reader, outputStream);
                case 2 -> processDownloadFile(reader, outputStream);
            }
        }
        node.shutDown();
    }

    private void processDownloadFile(BufferedReader reader, PrintStream out) throws IOException {
        out.println("Input path");
        String filename = reader.readLine().trim();
        //TODO Temp for test: FileInfo will have to be supplied at command line or derived from filename
        File file = new File(AKTorrent.class.getResource("/" + filename).getFile());
        node.downloadFile(FileUtils.getFileInfo(file));
    }

    private void processSeedFile(BufferedReader reader, PrintStream out) throws IOException {
        out.println("Input path");
        String filename = reader.readLine().trim();
        File file = new File(AKTorrent.class.getResource("/" + filename).getFile());
        node.seedFile(file);
    }
}
