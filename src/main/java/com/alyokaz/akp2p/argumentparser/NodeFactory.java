package com.alyokaz.akp2p.argumentparser;

import com.alyokaz.akp2p.AKP2P;
import com.alyokaz.akp2p.beacon.Beacon;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.StringTokenizer;

public class NodeFactory {

    private CLIFactory cliFactory;

    public NodeFactory(CLIFactory cliFactory) {this.cliFactory = cliFactory;}

    public Beacon buildBeacon() {
        return buildBeacon(0);
    }

    public Beacon buildBeacon(int port) {
        Beacon beacon = Beacon.createAndInitialise(port);
        cliFactory.buildCLI(beacon).start();
        return beacon;
    }

    public AKP2P build() throws IOException {
        return build(0);
    }
    public AKP2P build(int port) throws IOException {
        AKP2P akp2P = AKP2P.createAndInitializeNoBeacon(port);
        cliFactory.buildCLI(akp2P).start();
        return akp2P;
    }

    public AKP2P build(String beaconAddress, int port) throws IOException {
        StringTokenizer tokenizer = new StringTokenizer(beaconAddress);
        InetSocketAddress beaconSocketAddress =
                new InetSocketAddress(tokenizer.nextToken(), Integer.parseInt(tokenizer.nextToken()));
        AKP2P akp2P = AKP2P.createAndInitialize(port, beaconSocketAddress);
        cliFactory.buildCLI(akp2P).start();
        return akp2P;
    }

    public void build(String beaconAddress) throws IOException {
        build(beaconAddress, 0);
    }

}
