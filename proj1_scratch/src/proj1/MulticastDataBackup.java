package proj1;

import java.io.IOException;
import java.net.DatagramPacket;

public class MulticastDataBackup implements Runnable {
	@Override
	public void run() {
	    while(true) {
	    	byte[] message_bytes = new byte[Peer.MAX_SIZE];
			
			DatagramPacket message = new DatagramPacket(message_bytes, message_bytes.length);
			try {
				Peer.mdb_mcast.receive(message);
			} catch (IOException e1) {
				System.err.println("Error on receiving a packet in MDB!");
			}
			
			String message_str = new String(message.getData(), 0, message.getLength());

			/* 
			 * - Is it necessary to know who the protocol version?
			 */
			String[] msg_args = message_str.split(" ");
			String pt_vers = msg_args[1];
			String sender_id = msg_args[2];
			if(!sender_id.equals(Peer.server_id)) {
				String fileId = msg_args[3];
				String chunkNo = msg_args[4];
				String repDeg = msg_args[5];
				int size = 0; //TODO: Replace this by the real size
				
				/* real replication degree starts at one */
				Peer.file_chunk_list.put(fileId + "_" + chunkNo, "S_" + size + "_" + repDeg + "_" + 1);
				System.out.println("BACKUP -> Peer " + Peer.server_id + " has put " + "<" + fileId + "_" + chunkNo + "," + 'S' + "_" + size + "_" + repDeg + "_" + 1 + ">");
				
				// STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
				String response = "STORED " + pt_vers + " " + Peer.server_id + " " + fileId + " " + chunkNo + "\r\n\r\n";
				
				byte[] response_bytes = response.getBytes();
				DatagramPacket packet = new DatagramPacket(response_bytes, response_bytes.length, Peer.mc_inet, Peer.mc_port);
				
				/* random delay */
				try {
					Thread.sleep((long) (Math.random() * 400));
					Peer.mc_mcast.send(packet);
				} catch (IOException e) {
					System.err.println("Error on sending packet to MDB!");
				} catch (InterruptedException e) {
					System.err.println("Error on sleeping in MC!");
				}
			}
	    }		
	}

}
