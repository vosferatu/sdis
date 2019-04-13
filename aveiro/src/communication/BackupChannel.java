package communication;

import java.net.*;
import java.util.Arrays;

import server.Peer;

import java.io.IOException;


public class BackupChannel implements Runnable {
    private InetAddress address;
    private int port;

    public BackupChannel(InetAddress address, int port) {
            this.port = port;
            this.address = address;
    }

    public void run() {

        byte[] buffer = new byte[65000];

        try {
            MulticastSocket socket = new MulticastSocket(port);
            socket.joinGroup(address);

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] msg = Arrays.copyOf(buffer, packet.getLength());
                Peer.getPool().execute(new Mailman(msg));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void message(byte[] msg) {

        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket packet = new DatagramPacket(msg, msg.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
