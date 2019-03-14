package proj1;

import java.io.*;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Peer implements OpMethods {
	/* TODO: DEBUG: Delete */
	private static enum Debugger {JOAO, BRUNO};
	static Debugger dev = Debugger.BRUNO;
	/*				*/
	
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
			codebase = "file:///C:/Users/bmsp2/Documents/GitHub/sdis/proj1/bin";
		}
		else {
			codebase = "file:///home/vosferatu/eclipse-workspace/sdis3/bin/";
		}
		/*				*/
		System.setProperty("java.rmi.server.codebase",codebase);

		//Check for errors
		String protocol_version = args[0];
		int server_id = Integer.parseInt(args[1]);
		String access_point = args[2];
		
		try {
			String mc_ip = args[3];
			int mc_port = Integer.parseInt(args[4]);
			InetAddress mc_inet = InetAddress.getByName(mc_ip);
			MulticastSocket mc_mcast = new MulticastSocket(mc_port);

			String mdb_ip = args[5];
			int mdb_port = Integer.parseInt(args[6]);
			InetAddress mdb_inet = InetAddress.getByName(mdb_ip);
			MulticastSocket mdb_mcast = new MulticastSocket(mdb_port);

			String mdr_ip = args[7];
			int mdr_port = Integer.parseInt(args[8]);
			InetAddress mdr_inet = InetAddress.getByName(mdr_ip);
			MulticastSocket mdr_mcast = new MulticastSocket(mdr_port);
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		try {
            Peer obj = new Peer();
            OpMethods stub = (OpMethods) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(access_point, stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

	}

	public void backup(String filename, int replication_degree) {
		/*
		 * send to the MDB multicast data channel
		 * PUTCHUNK <Version> <SenderId> <FileId> <ChunkNo> <ReplicationDeg> <CRLF><CRLF> <Body>
		 * TODO: Decide on FileId generator bit string 
		 */
		
		/*
		 * receives on multicast control channel (MC) a confirmation message after a random delay uniformly distributed between 0 and 400 ms
		 * STORED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
		 */
		
		System.out.println("BACKUP");
	};

	public void restore(String filename) {
		/*
		 * send to the MDB multicast data channel
		 * GETCHUNK <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
		 */
		
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
		
		System.out.println("DELETE");
	};

	public void reclaim(int available_space) {
		/*
		 * send to the MC multicast control channel
		 * REMOVED <Version> <SenderId> <FileId> <ChunkNo> <CRLF><CRLF>
		 */
		
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
}
