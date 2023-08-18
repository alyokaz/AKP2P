package aktorrent;

import java.io.*;

public class CLI {

    private final InputStream inputStream;

    private final OutputStream outputStream;

    private final AKTorrent node;

    public CLI(InputStream inputStream, OutputStream outputStream, AKTorrent node) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.node = node;
    }

    public void start() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while((line = reader.readLine()) != null) {
            String[] tokens = line.split(" ");
            if(tokens[0].equals("seed")) {
                String filename = tokens[1];
                File file = new File(AKTorrent.class.getResource("/" + filename).getFile());
                node.seedFile(file);
            } else if(tokens[0].equals("download")) {
                String filename = tokens[1];
                //TODO Temp for test: FileInfo will have to be supplied at command line or derived from filename
                File file = new File(AKTorrent.class.getResource("/" + filename).getFile());
                node.downloadFile(FileUtils.getFileInfo(file));
            }
        }
        node.shutDown();
    }
}
