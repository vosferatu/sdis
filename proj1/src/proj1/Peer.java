package proj1;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Peer implements OpMethods {
	/* TODO: DEBUG: Delete */
	private static enum Debugger {JOAO, BRUNO};
	static Debugger dev = Debugger.BRUNO;
	/*				*/

	private static String server_id;
	private static String protocol_version;
	private static final int MAX_SIZE = 64000;

	static int mc_port;
	static InetAddress mc_inet;
	static MulticastSocket mc_mcast;
	static int mdb_port;
	static InetAddress mdb_inet;
	static MulticastSocket mdb_mcast;
	static int mdr_port;
	static InetAddress mdr_inet;
	static MulticastSocket mdr_mcast;
	
	/*
	 * Variables for STORED reception control
	 */
	int attemp_no = 0;
	static int stored_msgs = 0;
	
	/*
	 * Records information about the chunks it stores
	 */
	static ConcurrentHashMap<String, String> fileChunkList = new ConcurrentHashMap<String, String>();

	public Peer() {}

	static HashMap<String, String> database = new HashMap<String, String>();

	public static void main(String args[]) throws RemoteException {

		if(args.length != 9) {
			System.out.println("USAGE: java Peer <protocol_version> <server_id> <access_point> <mc_ip> <mp_port> <mdb_ip> <mdb_port> <mdr_ip> <mdr_port>");
			System.exit(1);
		}

		String codebase;

		/* TODO: Delete - To be done by command line */
		if(dev.equals(Debugger.BRUNO)) {
			codebase = "file:///C:/Users/bmsp2/Documents/GitHub/sdis/proj1/bin/";
		}
		else {
			codebase = "file:///home/vosferatu/Desktop/sdis/proj1/bin/";
		}
		/*				*/
		System.setProperty("java.rmi.server.codebase",codebase);

		//Check for errors
		protocol_version = args[0];
		server_id = args[1];
		String access_point = args[2];

		try {
			String mc_ip = args[3];
			mc_port = Integer.parseInt(args[4]);
			mc_inet = InetAddress.getByName(mc_ip);
			mc_mcast = new MulticastSocket(mc_port);
			mc_mcast.joinGroup(mc_inet);

			String mdb_ip = args[5];
			mdb_port = Integer.parseInt(args[6]);
			mdb_inet = InetAddress.getByName(mdb_ip);
			mdb_mcast = new MulticastSocket(mdb_port);
			mdb_mcast.joinGroup(mdb_inet);

			String mdr_ip = args[7];
			mdr_port = Integer.parseInt(args[8]);
			mdr_inet = InetAddress.getByName(mdr_ip);
			mdr_mcast = new MulticastSocket(mdr_port);
			mdr_mcast.joinGroup(mdr_inet);
		} catch (IOException e1) {
			System.err.println("Error on creating/joining multicast sockets!");
			System.exit(1);
		}
		
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
		executor.submit(() -> {
		    while(true) {
		    	byte[] message_bytes = new byte[MAX_SIZE];
				
				DatagramPacket message = new DatagramPacket(message_bytes, message_bytes.length);
				mdb_mcast.receive(message);
				
				String message_str = new String(message.getData(), 0, message.getLength());

				/* 
				 * - Is it necessary to know who the protocol version?
				 */
				String[] msg_args = message_str.split(" ");
				String pt_vers = msg_args[1];
				String sender_id = msg_args[2];
				if(!sender_id.equals(server_id)) {
					String fileId = msg_args[3];
					String chunkNo = msg_args[4];
					String repDeg = msg_args[5];
					int size = 0; //TODO: Replace this by the real size
					
					//TODO: After listening to the confirmation messages on MC, it shall sum it to the repDeg (like "repDeg_realRepDeg")
					/* real replication degree starts at one */
					fileChunkList.put(fileId + "_" + chunkNo, 'S' + "_" + size + "_" + repDeg + "_" + 1);
					System.out.println("BACKUP -> Peer " + server_id + " has put " + "<" + fileId + "_" + chunkNo + "," + 'S' + "_" + size + "_" + repDeg + "_" + 1 + ">");
					
					// STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
					String response = "STORED " + pt_vers + " " + server_id + " " + fileId + " " + chunkNo + "\r\n\r\n";
					
					byte[] response_bytes = response.getBytes();
					DatagramPacket packet = new DatagramPacket(response_bytes, response_bytes.length, mc_inet, mc_port);
					
					/* random delay */
					Thread.sleep((long) (Math.random() * 400));
					mc_mcast.send(packet);
				}
		    }
		});
		executor.submit(() -> {
		    while(true) {
		    	byte[] message_bytes = new byte[MAX_SIZE];
				
				DatagramPacket message = new DatagramPacket(message_bytes, message_bytes.length);
				mc_mcast.receive(message);
				
				String message_str = new String(message.getData(), 0, message.getLength());
				
				String[] msg_args = message_str.split(" ");
				String type = msg_args[0];
				if(type.equals("STORED")) {
					String sender_id = msg_args[2];
					if(!sender_id.equals(server_id)) {
						stored_msgs++;
						System.out.println("STORED received on MC!");
					}
				}
				else {
					if(type.equals("GETCHUNK")) {
						String sender_id = msg_args[2];
						if(!sender_id.equals(server_id)) {
							String file_id = msg_args[3];
							String chunk_no = msg_args[4];
							
							String value = fileChunkList.get(file_id + "_" + chunk_no);
							
							if(value != null) {	//peer has a copy of that chunk
								/* Send it to MDR */
								String response = "CHUNK " + protocol_version + " " + server_id + " " + file_id + " " + chunk_no + " /r/n/r/n";
								
								//TODO: Attach body to the message
								byte[] response_bytes = response.getBytes();
								DatagramPacket packet = new DatagramPacket(response_bytes, response_bytes.length, mdr_inet, mdr_port);
								
								//TODO: If it receives a CHUNK message the time expires, it will not send the CHUNK message.
								/* random delay */
								Thread.sleep((long) (Math.random() * 400));
								mdr_mcast.send(packet);
							}
						}
					}
				}
		    	
		    }
		});
		executor.submit(() -> {
			/* MDR Thread */
	    	byte[] message_bytes = new byte[MAX_SIZE];
			
			DatagramPacket message = new DatagramPacket(message_bytes, message_bytes.length);
			mc_mcast.receive(message);
			
			String message_str = new String(message.getData(), 0, message.getLength());
			
			/* Process the message */
		    return null;
		});

		try {
            Peer obj = new Peer();
            OpMethods stub = (OpMethods) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(access_point, stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            System.exit(1);
        }

	}

	public void backup(String filepath, int repDeg) {
		/*
		 * send to the MDB multicast data backup channel
		 * PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF><Body>
		 */

		File file = new File(filepath);
		if (!file.isFile()) {
			System.err.println("File to backup doesn't exist!");
			System.exit(1);
		}

		String fileId = "";

		/* Retrieves  a fileId from the hash of the given filepath */
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(filepath.getBytes(StandardCharsets.UTF_8));
			fileId = hash.toString();
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Error getting the file path hash!");
			System.exit(1);
		}

		/* Splits the file in chunks */
		ArrayList<byte[]> chunks = new ArrayList<byte[]>();
		try {
			FileInputStream sourceStream = new FileInputStream(file);
			byte[] buf = new byte[MAX_SIZE];
			int read;
			
			while ((read = sourceStream.read(buf)) > 0) {
				chunks.add(Arrays.copyOf(buf, read));
			}

			sourceStream.close();
		} catch (IOException e) {
			System.err.println("Error on splitting file to backup!");
			System.exit(1);
		}
		
		//  If the file size is a multiple of the chunk size, the last chunk has size 0.

		/*
		 * TODO:	-	Append the actual chunk to the messageHeader (To be sent on threads?)
		 */

		
		for(int chunkNo = 0; chunkNo < chunks.size(); chunkNo++){
			String messageHeader = "PUTCHUNK " + protocol_version + " " + server_id + " " + fileId + " " + chunkNo + " " + repDeg + " " + chunks.get(chunkNo).length + " \r\n\r\n";
			byte[] message_bytes = messageHeader.getBytes();
			DatagramPacket packet = new DatagramPacket(message_bytes, message_bytes.length, mdb_inet, mdb_port);
		
			while(attemp_no != 5) {
				try {
					stored_msgs = 0;
					mdb_mcast.send(packet);
					System.out.println("Going to wait " + (int) Math.pow(2, attemp_no) + " seconds...");
					Thread.sleep((int) Math.pow(2, attemp_no) * 1000);
					
					if(stored_msgs == repDeg) {
						fileChunkList.put(fileId + "_" + chunkNo, 'B' + "_" + filepath + "_" + repDeg + "_" + repDeg);
						System.out.println("BACKUP -> Peer " + server_id + " has put " + "<" + fileId + "_" + chunkNo + "," + repDeg + "_" + repDeg + ">");
						attemp_no = 0;
						stored_msgs = 0;
						break;
					}
					else {
						attemp_no++;
					}
					
				} catch (InterruptedException e) {
					System.out.println("Error on waiting for STORED messages reception!");
					e.printStackTrace();
				} catch (IOException e) {
					System.out.println("Error on sending packet to MDB!");
					e.printStackTrace();
				}
			}
			
			if(attemp_no == 5) {
				fileChunkList.put(fileId + "_" + chunkNo,  'B' + "_" + filepath + "_" + repDeg + "_" + stored_msgs);
				System.out.println("BACKUP -> Peer " + server_id + " has put " + "<" + fileId + "_" + chunkNo + "," + 'B' + "_" + filepath + "_" +  repDeg + "_" + stored_msgs + ">");
				
				attemp_no = 0;
				stored_msgs = 0;
				
				System.out.println("Couldn't achieve the desired replication degree");
				
			}
		}

		/* ------------------------------- */
	};

	public void restore(String filename) {
		/*
		 * send to the MDB multicast data channel
		 * GETCHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
		 */

		/*
		 * TODO:	- Replace fileId
		 * 			- Replace chunkNo
		 * 			- Send message to MDR
		 * 			- Listen to MDR channel for requested chunk
		 */

		String message = "GETCHUNK " + protocol_version + " " + server_id + " FILEID? CHUNKNO? \\r\\n\\r\\n";

		/* -------------------------------- */

		/*
		 * receives via the MDR channel after a random delay uniformly distributed between 0 and 400 ms
		 * CHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
		 */

		System.out.println("RESTORE");
	};

	public void delete(String filename) {
		/*
		 * send to the MC multicast control channel
		 * DELETE <Version> <SenderId> <FileId> <CRLF><CRLF>
		 */

		String message = "DELETE " + protocol_version + " " + server_id + " FILEID? \\r\\n\\r\\n";

		System.out.println("DELETE");
	};

	public void reclaim(int available_space) {
		/*
		 * send to the MC multicast control channel
		 * REMOVED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
		 */

		String message = "REMOVED " + protocol_version + " " + server_id + " FILEID? CHUNKNO? \\r\\n\\r\\n";

		System.out.println("RECLAIM");
	};

	public void state() {
		/*
			This operation allows to observe the service state. In response to such a request, the peer shall send to the client the following information:

		    For each file whose backup it has initiated:
		        The file pathname
		        The backup service id of the file
		        The desired replication degree
		        For each chunk of the file:
		            Its id
		            Its perceived replication degree
		    For each chunk it stores:
		        Its id
		        Its size (in KBytes)
		        Its perceived replication degree
		    The peer's storage capacity, i.e. the maximum amount of disk space that can be used to store chunks, and the amount of storage (both in KBytes) used to backup the chunks.
		*/

		System.out.println("STATE");
	};


	public void sendChunk(String fileId, String chunkNo) {
		// It may also receive the chunk to store

		/*
		 * TODO: Store the chunk received on MDB and send confirmation message to MC
		 * CHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
		 */

		String messageHeader = "CHUNK " + protocol_version + " " + server_id + " " + fileId + " " + chunkNo + " \r\n\r\n";
		System.out.println("CHUNK message header : " + messageHeader );

		//TODO: Send message to MC
	}


	public void store(String fileId, String chunkNo) {
		// It may also receive the chunk to store

		/*
		 * TODO: Store the chunk received on MDB and send confirmation message to MC
		 * STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
		 */

		String message = "STORED " + protocol_version + " " + server_id + " " + "FILENAME_FROM_MDB CHUNKNO_FROM_MDB\r\n\r\n";

		//TODO: Send message to MC
	}

	public void listenToMDB() {
		/*
		 * TODO: This one must be a thread who's always listening to MDB and calls the store and sendChunk functions
		 */
	}

	public void listenToMC() {
		/*
		 * TODO: This one must be a thread who's always listening to MC
		 *
		 * receives confirmation messages after a random delay uniformly distributed between 0 and 400 ms
		 * STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
		 *
		 * WHAT MORE?
		 */
	}

	public void listenToMDR() {
		/*
		 * TODO: This one must be a thread who's always listening to MDR
		 *
		 * receives via the MDR channel after a random delay uniformly distributed between 0 and 400 ms
		 * CHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF><Body>
		 *
		 * WHAT MORE?
		 */
	}
}
