package proj1;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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

	static String server_id;
	static String protocol_version;
	static final int MAX_SIZE = 64000;

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
	 * Records information about the chunks it stores
	 */
	static ConcurrentHashMap<String, String> file_chunk_list = new ConcurrentHashMap<String, String>();

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
		executor.submit(new MulticastDataBackup());
		executor.submit(new MulticastControl());
		executor.submit(new MulticastDataRecovery());

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

		if(chunks.get(chunks.size()-1).length == MAX_SIZE) {
			chunks.add(new byte[0]);
		}

		/*
		 * TODO: append the actual chunk to the messageHeader (To be sent on threads?)
		 */


		for(int chunkNo = 0; chunkNo < chunks.size(); chunkNo++){
			String messageHeader = "PUTCHUNK " + protocol_version + " " + server_id + " " + fileId + " " + chunkNo + " " + repDeg + " " + chunks.get(chunkNo).length + " \r\n\r\n";
			byte[] message_bytes = messageHeader.getBytes();
			DatagramPacket packet = new DatagramPacket(message_bytes, message_bytes.length, mdb_inet, mdb_port);

			int attemp_no = 0;

			String key = fileId + "_" + chunkNo;

			file_chunk_list.put(key, "B_" + filepath + "_" + repDeg + "_" + 0);

			while(attemp_no != 5) {
				try {
					resetStoredMessages(key);
					mdb_mcast.send(packet);
					System.out.println("Going to wait " + (int) Math.pow(2, attemp_no) + " seconds...");
					Thread.sleep((int) Math.pow(2, attemp_no) * 1000);

					int stored_msgs = getStoredMessages(key);

					if(stored_msgs == repDeg) {
						file_chunk_list.put(fileId + "_" + chunkNo, "B_" + filepath + "_" + repDeg + "_" + repDeg);
						System.out.println("BACKUP -> Peer " + server_id + " has put " + "<" + fileId + "_" + chunkNo + "," + repDeg + "_" + repDeg + ">");
						attemp_no = 0;
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

			int stored_msgs = getStoredMessages(key);

			if(attemp_no == 5) {				
				file_chunk_list.put(fileId + "_" + chunkNo,  "B_" + filepath + "_" + repDeg + "_" + stored_msgs);
				System.out.println("BACKUP -> Peer " + server_id + " has put " + "<" + fileId + "_" + chunkNo + "," + 'B' + "_" + filepath + "_" +  repDeg + "_" + stored_msgs + ">");

				attemp_no = 0;

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

	@SuppressWarnings("serial")
	public void state() {
		ConcurrentHashMap<String, String> test_hashmap = new ConcurrentHashMap<String, String>();
		test_hashmap.put("fileId1_1", "B_fileId1path_2_2");
		test_hashmap.put("fileId1_2", "B_fileId1path_2_2");
		test_hashmap.put("fileId3_1", "B_fileId3path_2_2");
		test_hashmap.put("fileId3_2", "B_fileId3path_2_2");
		test_hashmap.put("fileId2_1", "S_1024_2_2");
		test_hashmap.put("fileId2_2", "S_768_2_2");

		HashMap<String, ArrayList<String>> backups = new HashMap<String, ArrayList<String>>();
		ArrayList<String> stores = new ArrayList<String>();
	
		for (Entry<String, String> entry : test_hashmap.entrySet()) {
			String key = entry.getKey().toString();
			String[] key_args = key.split("_");

			String value = entry.getValue();
			String[] value_args = value.split("_");

			String record_type = value_args[0];

			switch(record_type) {
			case "B":
				String backups_key = "File Pathname = " + value_args[1] + " | File ID = " + key_args[0] + " | Desired Replication Degree = " + value_args[2];
				if(!backups.containsKey(backups_key)) {
					backups.put(backups_key, new ArrayList<String>(){{
						   add("Chunk No = " + key_args[1] + " | Perceived Replication Degree = " + value_args[3]);
						}});
				}
				else {
					ArrayList<String> backups_values = backups.get(backups_key);
					backups_values.add("Chunk No = " + key_args[1] + " | Perceived Replication Degree = " + value_args[3]);
					backups.put(backups_key, backups_values);
				}
				break;
			case "S":
				stores.add("File ID = " + key_args[0] + " | Chunk No = " + key_args[1] + " | Size = " + value_args[1] + " | Perceived Replication Degree = " + value_args[3] + "\n");
				break;
			}
		}

		System.out.println("BACKUPS:\n");
		for (Entry<String, ArrayList<String>> entry : backups.entrySet()) {
		    System.out.println(entry.getKey());
		    ArrayList<String> values = entry.getValue();
		    
		    for (String backup : values) System.out.println(backup);
		    System.out.println();
		}

		System.out.println("STORES:\n");
		for (String store : stores) System.out.println(store);


		// TODO: Add the peer's storage capacity, i.e. the maximum amount of disk space that can be used to store chunks, and the amount of storage (both in KBytes) used to backup the chunks.
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

	private void resetStoredMessages(String key) {
		String value = file_chunk_list.get(key);

		String[] value_args = value.split("_");

		Integer real_replication_degree = Integer.parseInt(value_args[3]);
		real_replication_degree = 0;

		String new_value = "";

		for (int arg_i = 0; arg_i < value_args.length - 1; arg_i++) {
			new_value += value_args[arg_i] + "_";
		}

		new_value += real_replication_degree;

		file_chunk_list.put(key, new_value);
	}

	private int getStoredMessages(String key) {
		String value = file_chunk_list.get(key);

		String[] value_args = value.split("_");

		return Integer.parseInt(value_args[3]);
	}
}
