package com.alyokaz.aktorrent.peerservice;

public class PingPeerException extends RuntimeException {

    public PingPeerException(String errorMessage, Throwable e) {
        super(errorMessage, e);
    }
}
