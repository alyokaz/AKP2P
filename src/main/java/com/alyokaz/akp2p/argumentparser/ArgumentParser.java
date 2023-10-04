package com.alyokaz.akp2p.argumentparser;

import com.alyokaz.akp2p.peerservice.exceptions.ContactBeaconException;

import java.io.IOException;
import java.util.*;

public class ArgumentParser {

    public final static String BEACON_OPTION = "-beacon";
    public final static String PORT_OPTION = "-port";
    public final static String BEACON_ADDRESS_OPTION = "-beacon-address";

    private final NodeFactory nodeFactory;

    public ArgumentParser(NodeFactory nodeFactory) {this.nodeFactory = nodeFactory;}


    public void parseArguments(String[] args) {
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
                        nodeFactory.buildBeacon(Integer.parseInt(argumentMap.get(PORT_OPTION)));
                    } else {
                        nodeFactory.buildBeacon();
                    }

                } else if(argumentMap.containsKey(PORT_OPTION)) {

                    if (argumentMap.containsKey(BEACON_ADDRESS_OPTION)) {
                        nodeFactory.build(argumentMap.get(BEACON_ADDRESS_OPTION), Integer.parseInt(argumentMap.get((PORT_OPTION))));
                    } else {
                        nodeFactory.build(Integer.parseInt(argumentMap.get(PORT_OPTION)));
                    }

                } else if(argumentMap.containsKey(BEACON_ADDRESS_OPTION)){
                    nodeFactory.build(argumentMap.get(BEACON_ADDRESS_OPTION));
                } else {
                    nodeFactory.build();
                }

            } else {
                nodeFactory.build();
            }
        } catch(ContactBeaconException | IOException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }





}
