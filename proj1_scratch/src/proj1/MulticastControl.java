package proj1;

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Map;
import java.util.Map.Entry;

public class MulticastControl implements Runnable {
	void increaseStoredMessages(String key) {		
		String value = Peer.file_chunk_list.get(key);
		System.out.println(value);
		
		String[] value_args = value.split("_");
		
		Integer real_replication_degree = Integer.parseInt(value_args[3]);
		real_replication_degree++;
		
		String new_value = "";
		
		for (int arg_i = 0; arg_i < value_args.length - 1; arg_i++) {
			new_value += value_args[arg_i] + "_";
		}
		
		new_value += real_replication_degree;
		
		Peer.file_chunk_list.put(key, new_value);
	}
	
	@Override
	public void run() {
		while(true) {
	    	byte[] message_bytes = new byte[Peer.MAX_SIZE];
			
			DatagramPacket message = new DatagramPacket(message_bytes, message_bytes.length);
			try {
				Peer.mc_mcast.receive(message);
			} catch (IOException e) {
				System.err.println("Error on receiving a packet in MC!");
			}
			
			String message_str = new String(message.getData(), 0, message.getLength());
			
			String[] msg_args = message_str.split("\\s+");
			String type = msg_args[0];
			String sender_id = msg_args[2];
			if(type.equals("STORED")) {
				if(!sender_id.equals(Peer.server_id)) {
					System.out.println(message_str + " received.");
					String key = msg_args[3] + "_" + msg_args[4];
					String value = Peer.file_chunk_list.get(key);
					increaseStoredMessages(key);
				}
			}
			else {
				if(type.equals("GETCHUNK")) {
					if(!sender_id.equals(Peer.server_id)) {
						String file_id = msg_args[3];
						String chunk_no = msg_args[4];
						
						String value = Peer.file_chunk_list.get(file_id + "_" + chunk_no);
						
						if(value != null) {	//peer has a copy of that chunk
							/* Send it to MDR */
							String response = "CHUNK " + Peer.protocol_version + " " + Peer.server_id + " " + file_id + " " + chunk_no + " /r/n/r/n";
							
							//TODO: Attach body to the message
							byte[] response_bytes = response.getBytes();
							DatagramPacket packet = new DatagramPacket(response_bytes, response_bytes.length, Peer.mdr_inet, Peer.mdr_port);
							
							//TODO: If it receives a CHUNK message before the time expires, it will not send the CHUNK message.
							/* random delay */
							try {
								Thread.sleep((long) (Math.random() * 400));
								Peer.mdr_mcast.send(packet);
							} catch (InterruptedException e) {
								System.err.println("Error on sleeping in MC!");
							} catch (IOException e) {
								System.err.println("Error on sending packet to MDR!");
							}
						}
					}
				}
			}
	    	
	    }
	}
}
