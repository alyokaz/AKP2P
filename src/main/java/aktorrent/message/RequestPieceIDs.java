package aktorrent.message;

import java.io.Serializable;
import java.util.List;
public interface RequestPieceIDs extends Serializable {

    List<Integer> getIds();

    String getFilename();
}
