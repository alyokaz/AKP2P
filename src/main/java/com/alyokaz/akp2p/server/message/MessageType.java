package com.alyokaz.akp2p.server.message;

/**
 * An enum representing the different types of {@code Message} that can be handled by a peer
 */
public enum MessageType {
    REQUEST_FILENAMES, REQUEST_PIECES, END, REQUEST_PIECE, REQUEST_PEERS, GET_COMPLETED_FILES, REQUSET_FILE_INFOS, REQUEST_AVAILABLE_FILES
}
