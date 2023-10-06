package com.alyokaz.akp2p.cli;

import com.alyokaz.akp2p.beacon.Beacon;
import com.alyokaz.akp2p.beacon.BeaconCLI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BeaconCLIUnitTests {

    public static final String DELIMITER = ">";
    private Beacon beacon;
    private InputStream in;
    private PrintStream out;
    private ByteArrayOutputStream bytes;

    private static final int PORT = 4444;


    @BeforeEach
    public void setup() {
        beacon = mock(Beacon.class);
        when(beacon.getAddress()).thenReturn(new InetSocketAddress(PORT));
    }


    @Test
    public void canExitBeacon() {
        BeaconCLI cli = setUpCLI("foo\nbar\nexit\n");
        cli.start();
    }

    @Test
    public void displaysBeaconAddressAndShutDownPromptAndExitMessage() {
        BeaconCLI cli = setUpCLI("exit\n");
        cli.start();
        Scanner scanner = new Scanner(bytes.toString());
        assertEquals(String.format(BeaconCLI.SERVER_ADDRESS_MESSAGE.replace("%n", ""), PORT),
                scanner.nextLine());
        assertEquals(BeaconCLI.EXIT_PROMPT, extractPrompt(scanner));
        assertEquals(BeaconCLI.SERVER_SHUTDOWN_MESSAGE, scanner.nextLine());
    }

    private String extractPrompt(Scanner scanner) {
        scanner.useDelimiter(DELIMITER);
        return scanner.next() + scanner.findInLine(DELIMITER);
    }

    private BeaconCLI setUpCLI(String command) {
        in = new ByteArrayInputStream(command.getBytes());
        bytes = new ByteArrayOutputStream();
        out = new PrintStream(bytes);
        return new BeaconCLI(beacon, in, out);
    }

}
