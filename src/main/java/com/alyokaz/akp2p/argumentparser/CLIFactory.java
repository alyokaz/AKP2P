package com.alyokaz.akp2p.argumentparser;

import com.alyokaz.akp2p.AKP2P;
import com.alyokaz.akp2p.beacon.Beacon;
import com.alyokaz.akp2p.beacon.BeaconCLI;
import com.alyokaz.akp2p.cli.CLI;

public class CLIFactory {

    public BeaconCLI buildCLI(Beacon beacon) {
        return new BeaconCLI(beacon, System.in, System.out);
    }

    public CLI buildCLI(AKP2P akp2P) {
        return new CLI(System.in, System.out, akp2P);
    }

}
