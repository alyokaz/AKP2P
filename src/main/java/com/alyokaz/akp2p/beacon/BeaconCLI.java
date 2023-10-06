package com.alyokaz.akp2p.beacon;

import java.io.*;

/**
 * This class a CLI for a {@code Beacon} node.
 */
public class BeaconCLI {

    public static final String SERVER_SHUTDOWN_MESSAGE = "Beacon shutting down";
    public static final String EXIT_PROMPT = "Type 'exit' to shutdown beacon>";
    public static final String SERVER_ADDRESS_MESSAGE = "Beacon started on %s%n";
    public static final String EXIT_TOKEN = "exit";
    private final Beacon beacon;
    private final InputStream in;
    private final PrintStream out;


    public BeaconCLI(Beacon beacon, InputStream in, PrintStream out) {
        this.beacon = beacon;
        this.in = in;
        this.out = out;
    }

    public void start() {
        out.printf(SERVER_ADDRESS_MESSAGE, beacon.getAddress().getPort());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        out.printf(EXIT_PROMPT);
        while (true) {
            try {
                if (reader.readLine().equals(EXIT_TOKEN)) break;
                out.printf(EXIT_PROMPT);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        beacon.shutDown();
        out.println(SERVER_SHUTDOWN_MESSAGE);
    }
}
