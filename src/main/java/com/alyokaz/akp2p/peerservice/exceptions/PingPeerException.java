package com.alyokaz.akp2p.peerservice.exceptions;

public class PingPeerException extends RuntimeException {

    public PingPeerException(String errorMessage, Throwable e) {
        super(errorMessage, e);
    }
}
