package com.alyokaz.akp2p.argumentparser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ArgumentParserTest {
    public static final String BEACON_HOST = "localhost";
    public static final int BEACON_PORT = 4444;
    public static final int PORT = 4443;
    NodeFactory nodeFactory;
    ArgumentParser argumentParser;

    @BeforeEach
    void setup() {
        nodeFactory = mock(NodeFactory.class);
        argumentParser = new ArgumentParser(nodeFactory);
    }


    @Test
    public void canBuildBeacon() throws IOException {
        argumentParser.parseArguments(buildArguments(ArgumentParser.BEACON_OPTION));
        verify(nodeFactory).buildBeacon();
    }

    @Test
    void canBuildBeaconWithPort() throws IOException {
        int port = 4444;
        argumentParser.parseArguments(
                buildArguments(ArgumentParser.PORT_OPTION, Integer.toString(port), ArgumentParser.BEACON_OPTION));
        verify(nodeFactory).buildBeacon(port);
    }

    @Test
    void canBuildNode() throws IOException {
        argumentParser.parseArguments(new String[]{});
        verify(nodeFactory).build();
    }

    @Test
    void canBuildNodeWithPort() throws IOException {
        int port = 444;
        argumentParser.parseArguments(buildArguments(ArgumentParser.PORT_OPTION, Integer.toString(port)));
        verify(nodeFactory).build(port);
    }

    @Test
    void canBuildNodeWithBeacon() throws IOException {
        argumentParser.parseArguments(buildArguments(
                ArgumentParser.BEACON_ADDRESS_OPTION,
                BEACON_HOST,
                Integer.toString(BEACON_PORT)));
        verify(nodeFactory).build(BEACON_HOST + " " + BEACON_PORT);
    }

    @Test
    void canBuildNodeWithPortAndBeacon() throws IOException {
        argumentParser.parseArguments(buildArguments(
                ArgumentParser.PORT_OPTION, Integer.toString(PORT),
                ArgumentParser.BEACON_ADDRESS_OPTION, BEACON_HOST, Integer.toString(BEACON_PORT)
        ));
        verify(nodeFactory).build(BEACON_HOST + " " + BEACON_PORT, PORT);
    }

    @Test
    void throwsExceptionOnUnknownArgument() {
        assertThrows(IllegalArgumentException.class, () -> {
            argumentParser.parseArguments(buildArguments("-unknown-option"));
        });
    }

    @Test
    void throwsExceptionOnIllegalArgumentCombination() {
        assertThrows(IllegalArgumentException.class, () -> {
            argumentParser.parseArguments(buildArguments(ArgumentParser.BEACON_OPTION,
                    ArgumentParser.BEACON_ADDRESS_OPTION, BEACON_HOST, Integer.toString(BEACON_PORT)));
        });
    }

    private String[] buildArguments(String... args) {
        return args;
    }

}
