package com.alyokaz.akp2p.peerservice.exceptions;

/**
 * An {@code Exception} to thrown whilst attempting to contact a {@cond Beacon} node.
 */
public class ContactBeaconException extends RuntimeException {

    public ContactBeaconException(String message, Throwable t) {
        super(message, t);
    }
}
