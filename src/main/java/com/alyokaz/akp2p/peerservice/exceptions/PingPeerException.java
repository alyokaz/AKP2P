package com.alyokaz.akp2p.peerservice.exceptions;

/**
 * An {@code Exception} thrown during pinging between nodes
 */
public class PingPeerException extends RuntimeException {

    public PingPeerException(String errorMessage, Throwable e) {
        super(errorMessage, e);
    }
}
