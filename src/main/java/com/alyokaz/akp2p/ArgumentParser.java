package com.alyokaz.akp2p;

import com.alyokaz.akp2p.beacon.Beacon;
import com.alyokaz.akp2p.beacon.BeaconCLI;
import com.alyokaz.akp2p.cli.CLI;
import com.alyokaz.akp2p.peerservice.exceptions.ContactBeaconException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public class ArgumentParser {

    public final static String BEACON_OPTION = "-beacon";
    public final static String PORT_OPTION = "-port";
    public final static String BEACON_ADDRESS_OPTION = "-beacon-address";


    public static void parseArguments(String[] args) {
        Map<String, String> argumentMap = new HashMap<>();
        try {
            if (args.length > 0) {
                Iterator<String> it = Arrays.stream(args).iterator();

                while(it.hasNext()) {
                    String command = it.next();
                    switch (command.trim()) {
                        case BEACON_OPTION -> argumentMap.put(BEACON_OPTION, "");
                        case PORT_OPTION -> argumentMap.put(PORT_OPTION, it.next());
                        case BEACON_ADDRESS_OPTION -> argumentMap.put(BEACON_ADDRESS_OPTION, it.next() + " " + it.next());
                        default -> throw new IllegalArgumentException("Unknown option " + command);
                    }
                }

                if(argumentMap.containsKey(BEACON_OPTION) && argumentMap.containsKey(BEACON_ADDRESS_OPTION))
                    throw new IllegalArgumentException(BEACON_ADDRESS_OPTION + " is not applicable to a Beacon instance");

                if(argumentMap.containsKey(BEACON_OPTION)) {

                    if (argumentMap.containsKey(PORT_OPTION)) {
                        buildBeacon(Integer.parseInt(argumentMap.get(PORT_OPTION)));
                    } else {
                        buildBeacon();
                    }

                } else if(argumentMap.containsKey(PORT_OPTION)) {

                    if (argumentMap.containsKey(BEACON_ADDRESS_OPTION)) {
                        build(argumentMap.get(BEACON_ADDRESS_OPTION), Integer.parseInt(argumentMap.get((PORT_OPTION))));
                    } else {
                        build(Integer.parseInt(argumentMap.get(PORT_OPTION)));
                    }

                } else if(argumentMap.containsKey(BEACON_ADDRESS_OPTION)){
                    build(argumentMap.get(BEACON_ADDRESS_OPTION));
                } else {
                    build();
                }

            } else {
                AKP2P node = AKP2P.createAndInitializeNoBeacon();
                CLI cli = new CLI(System.in, System.out, node);
                cli.start();
            }
        } catch(ContactBeaconException | IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }

    private static void buildBeacon() {
        buildBeacon(0);
    }

    private static void buildBeacon(int port) {
        Beacon beacon = Beacon.createAndInitialise(port);
        BeaconCLI cli = new BeaconCLI(beacon, System.in, System.out);
        cli.start();
    }

    private static void build() throws IOException {
        build(0);
    }
    private static void build(int port) throws IOException {
        buildAndStartCli(AKP2P.createAndInitializeNoBeacon(port));
    }

    public static void build(String beaconAddress, int port) throws IOException {
        StringTokenizer tokenizer = new StringTokenizer(beaconAddress);
        InetSocketAddress beaconSocketAddress =
                new InetSocketAddress(tokenizer.nextToken(), Integer.parseInt(tokenizer.nextToken()));
        buildAndStartCli(AKP2P.createAndInitialize(port, beaconSocketAddress));
    }

    public static void build(String beaconAddress) throws IOException {
        build(beaconAddress, 0);
    }

    private static void buildAndStartCli(AKP2P node) throws IOException {
        CLI cli = new CLI(System.in, System.out, node);
        cli.start();
    }



}
