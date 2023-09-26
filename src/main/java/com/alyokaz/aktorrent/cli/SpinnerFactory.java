package com.alyokaz.aktorrent.cli;

import java.io.PrintStream;

public class SpinnerFactory {

    private final PrintStream outputStream;

    public SpinnerFactory(PrintStream outputStream) {this.outputStream = outputStream;}

    public Thread buildSpinnerThread() {
        return new Thread(new Spinner(outputStream));
    }
}
