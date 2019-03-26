package proj1;

import java.io.IOException;
import java.net.DatagramPacket;

public class MulticastDataRecovery implements Runnable {

	@Override
	public void run() {
		while(true) {
			/* MDR Thread */
	    	byte[] message_bytes = new byte[Peer.MAX_SIZE];
			
			DatagramPacket message = new DatagramPacket(message_bytes, message_bytes.length);
			try {
				Peer.mc_mcast.receive(message);
			} catch (IOException e) {
				System.err.println("Error on receiving a packet in MDR!");
			}
			
			String message_str = new String(message.getData(), 0, message.getLength());
			
			/* Process the message */
		}
	}

}
