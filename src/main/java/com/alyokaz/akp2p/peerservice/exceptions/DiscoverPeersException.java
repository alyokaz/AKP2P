package com.alyokaz.akp2p.peerservice.exceptions;

/**
 * An {@code Exception} thrown whilst attempting to discover new peers from known live peers.
 */
public class DiscoverPeersException extends RuntimeException {

    public DiscoverPeersException(String message, Throwable t) {
        super(message, t);
    }
}
