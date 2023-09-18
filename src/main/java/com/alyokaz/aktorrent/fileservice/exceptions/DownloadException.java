package com.alyokaz.aktorrent.fileservice.exceptions;

public class DownloadException extends RuntimeException {

    public DownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
