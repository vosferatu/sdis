package lab2;

import java.io.*;
import java.net.*;

public class Client {
	static int ucast_port = 0;
	
	
	public static void main(String args[]) throws IOException {
	
		if(args.length < 4) {
			System.out.println("USAGE: java Client <multicast_address> <multicast_port> <oper> <opnd>*");
			System.exit(1);
		}
		
		String message = new String();
		
		String mcast_address = args[0];
		int mcast_port = Integer.parseInt(args[1]);
		String oper = args[2];
		
		MulticastSocket mcast = new MulticastSocket(mcast_port);
		
		mcast.joinGroup(InetAddress.getByName(mcast_address));
		
		byte[] response_bytes = new byte[255];
		
		DatagramPacket response = new DatagramPacket(response_bytes, response_bytes.length);
		mcast.receive(response);
		
		String response_str = new String(response.getData(), 0, response.getLength());
		
		ucast_port = Integer.parseInt(response_str);
		System.out.println(response_str);
		
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
			
	    DatagramSocket client = new DatagramSocket();
	    InetAddress address = InetAddress.getByName("localhost");

		byte[] dataReceived = new byte[1024];
		byte[] dataSent = new byte[1024];
		
		dataSent = message.getBytes();
		
		
		DatagramPacket request = new DatagramPacket(dataSent, dataSent.length, address, ucast_port);
		client.send(request);
	
		DatagramPacket reply = new DatagramPacket(dataReceived, dataReceived.length);
		
		try {
			client.receive(reply);			
			String ack = new String(reply.getData(), 0, reply.getLength());
			System.out.println("FROM SERVER: " + ack);
		}
		catch (SocketTimeoutException ste) {
			System.out.println("NO_RESPONSE");
		}
		
		client.close();
		mcast.close();
	}
		
}