package aktorrent;

import aktorrent.message.Message;
import aktorrent.message.MessageType;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class PingHandler implements Runnable {

    final DatagramPacket packet;
    final DatagramSocket socket;

    public PingHandler(DatagramPacket packet, DatagramSocket socket) {
        this.packet = packet;
        this.socket = socket;
    }

    @Override
    public void run() {
        byte[] buf = PingServer.PONG_PAYLOAD.getBytes();
        DatagramPacket replyPacket = new DatagramPacket(buf, buf.length, packet.getSocketAddress());
        try {
            socket.send(replyPacket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
