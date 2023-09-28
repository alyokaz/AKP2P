package com.alyokaz.akp2p.cli;

import java.io.PrintStream;

/**
 * Factory for creating a {@link Spinner}.
 * <p>
 * Used in unit testing for the CLI.
 */
public class SpinnerFactory {

    private final PrintStream outputStream;

    public SpinnerFactory(PrintStream outputStream) {this.outputStream = outputStream;}

    public Thread buildSpinnerThread() {
        return new Thread(new Spinner(outputStream));
    }
}
