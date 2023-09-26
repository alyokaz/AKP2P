package com.alyokaz.akp2p.cli;

import java.io.PrintStream;
import java.util.List;

public class Spinner implements Runnable {

    private final PrintStream outputStream;
    private final int SPEED = 100;
    private final List<Character> blades = List.of('/', '-', '\\', '-');

    public Spinner(PrintStream outputStream) {this.outputStream = outputStream;}

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                for (char blade : blades) {
                    spin(blade);
                }
            }
        } catch (InterruptedException e) {
            //no action needed
        }
    }

    private void spin(char blade) throws InterruptedException {
        outputStream.printf("\r" + CLI.GREEN + "Connecting " + blade + CLI.RESET);
        Thread.sleep(SPEED);
    }
}
