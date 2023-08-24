package com.alyokaz.aktorrent.pingserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PingServer {

    public static final int BUFFER_SIZE = 1400;
    public static final String PING_PAYLOAD = "ping";
    public static final String PONG_PAYLOAD = "pong";
    private final DatagramSocket socket;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public PingServer(DatagramSocket socket) {
        this.socket = socket;
    }

    public void start() {
        executor.execute(() -> {
            try (DatagramSocket inSocket = socket) {
                while (!Thread.currentThread().isInterrupted()) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    inSocket.receive(packet);
                    String payload = new String(packet.getData(), StandardCharsets.UTF_8).trim();
                    if (payload.equals(PING_PAYLOAD))
                        executor.execute(new PingHandler(packet, inSocket));
                }
            } catch (SocketException e) {
                System.out.println("Ping Server closed");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void shutdown() {
        System.out.println("Ping Server Shutdown");
        Thread.currentThread().interrupt();
        this.socket.close();
    }


}
