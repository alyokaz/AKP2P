package com.alyokaz.aktorrent.peerservice;

public class PingPeerException extends Exception {

    public PingPeerException(String errorMessage, Throwable e) {
        super(errorMessage, e);
    }
}
