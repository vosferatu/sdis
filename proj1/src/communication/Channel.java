package communication;

import java.net.*;
import java.util.Arrays;

import server.Peer;

import java.io.IOException;


public class Channel implements Runnable {
	
    @Override
	public String toString() {
		return "Channel [address=" + address + ", port=" + port + "]";
	}

	private InetAddress address;
    private int port;

    public Channel(InetAddress address, int port) {
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
                Mailman task = new Mailman(msg);
                Peer.getPool().execute(task);
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
