package lab1;

import java.io.*;
import java.net.*;
import java.util.HashMap;

public class Server {
	
	static HashMap<String, String> database = new HashMap<String, String>();
	
	public static void main(String args[]) throws IOException {
	
		if(args.length != 1) {
			System.out.println("USAGE: java Server <port_number>");
			System.exit(1);
		}
		
		//Check for errors
		int port_number = Integer.parseInt(args[0]);
		
		DatagramSocket server = new DatagramSocket(port_number);
	
		byte[] dataReceived = new byte[1024];
		byte[] dataSent = new byte[1024];
	
		while(true) {
			DatagramPacket request = new DatagramPacket(dataReceived, dataReceived.length);
			server.receive(request);
			InetAddress address = request.getAddress();
			int port = request.getPort();
		
			String message = new String(request.getData(), 0, request.getLength());
			System.out.println("RECEIVED: " + message);
			
			String[] splitted = message.split(" ");
			String oper = splitted[0];
			
			String serverResponse = new String();
			
			if(oper.equals("register")) {
				String plate_number = splitted[1];
				String owner_name = splitted[2];
				
				if(database.containsKey(plate_number))
					serverResponse = "-1";
				else {
					database.put(plate_number, owner_name);
					serverResponse = "" + database.size();
				}					
			}
			else {
				String plate_number = splitted[1];
				String owner = database.get(plate_number);

				if(owner == null)
					serverResponse = "NOT FOUND";
				else serverResponse = owner;

			}
			
			dataSent = serverResponse.getBytes();
			
			DatagramPacket reply = new DatagramPacket(dataSent, dataSent.length, address, port);
		
			server.send(reply);
		}
		
	}
}
