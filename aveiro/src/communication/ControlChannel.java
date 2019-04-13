package communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;

import server.Peer;

public class ControlChannel implements Runnable {
	
    private InetAddress address;
    private int port;

    public ControlChannel(InetAddress address, int port) {
        this.port = port;
        this.address = address;
    }

    public void message(byte[] msg) {

        try (DatagramSocket socket = new DatagramSocket()) {

            DatagramPacket packet = new DatagramPacket(msg, msg.length, address, port);
            socket.send(packet);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {

        byte[] buffer = new byte[65000];

        try {
            MulticastSocket socket = new MulticastSocket(port);

            socket.joinGroup(address);

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] buffed = Arrays.copyOf(buffer, packet.getLength());
                Peer.getPool().execute(new Mailman(buffed));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
