package lab1;

import java.io.*;
import java.net.*;

public class Client {
	
	public static void main(String args[]) throws IOException {
	
		if(args.length < 4) {
			System.out.println("USAGE: java Client <host_name> <port_number> <oper> <opnd>*");
			System.exit(1);
		}
		
		String message = new String();
		
		String host_name = args[0];
		int port_number = Integer.parseInt(args[1]);
		String oper = args[2];
		
		message = args[2];
		
		if(oper.equals("register")) {
			//Verifica se há 5 args
			if(args.length != 5) {
				System.out.println("WRONG NUMBER OF ARGUMENTS");
				System.exit(1);
			}
			
			message += " " + args[3] + " " + args[4];
		}
		else {
			if(oper.equals("lookup")) {
				//Verifica se há 4 args
				if(args.length != 4) {
					System.out.println("WRONG NUMBER OF ARGUMENTS");
					System.exit(1);
				}
				
				message += " " + args[3];
			}
			else {
				System.out.println("OPERATION NOT SUPPORTED");
				System.exit(2);
			}
		}
		
	    BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	
	    DatagramSocket client = new DatagramSocket();
	    InetAddress address = InetAddress.getByName(host_name);

		byte[] dataReceived = new byte[1024];
		byte[] dataSent = new byte[1024];
		
		dataSent = message.getBytes();
		
		
		DatagramPacket request = new DatagramPacket(dataSent, dataSent.length, address, port_number);
		client.send(request);
	
		DatagramPacket reply = new DatagramPacket(dataReceived, dataReceived.length);
		
		client.setSoTimeout(5000);
		
		try {
			client.receive(reply);			
			String ack = new String(reply.getData(), 0, reply.getLength());
			System.out.println("FROM SERVER: " + ack);
		}
		catch (SocketTimeoutException ste) {
			System.out.println("NO_RESPONSE");
		}
		
		client.close();
	}
		
}