package aktorrent;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

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
}
