package com.alyokaz.aktorrent.beacon;

import java.io.*;

public class BeaconCLI {

    public static final String SERVER_SHUTDOWN_MESSAGE = "Beacon shutting down";
    private final Beacon beacon;
    private final InputStream in;
    private final PrintStream out;

    public static final String SERVER_ADDRESS_MESSAGE = "Beacon started on %s%n";
    public static final String EXIT_TOKEN = "exit";


    public BeaconCLI(Beacon beacon, InputStream in, PrintStream out) {
        this.beacon = beacon;
        this.in = in;
        this.out = out;
    }

    public void start() {
        out.printf(SERVER_ADDRESS_MESSAGE, beacon.getAddress().getPort());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        while (true) {
            try {
                if (reader.readLine().equals(EXIT_TOKEN)) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        beacon.shutDown();
        out.println(SERVER_SHUTDOWN_MESSAGE);
    }
}
