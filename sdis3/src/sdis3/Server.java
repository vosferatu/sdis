package sdis3;
import java.io.*;
import java.util.HashMap;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Server implements Register {
	
	public Server() {}

	static HashMap<String, String> database = new HashMap<String, String>();

	public static void main(String args[]) throws RemoteException {

		if(args.length != 1) {
			System.out.println("USAGE: java Server <remote_object_name>");
			System.exit(1);
		}
		
		String codebase = "file:///home/vosferatu/eclipse-workspace/sdis3/bin/";
		System.setProperty("java.rmi.server.codebase",codebase);

		//Check for errors
		String remote_object_name = args[0];
		
		try {
            Server obj = new Server();
            Register stub = (Register) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(remote_object_name, stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

	}

	public int register(String plate_number, String owner_name){
		if(database.containsKey(plate_number))
			return -1;
		else {
			database.put(plate_number, owner_name);
			return database.size();
		}
	}
	
	public String lookup(String plate_number) {
		String owner = database.get(plate_number);

		if(owner == null)
			return "NOT FOUND";
		else return owner;
	}
	
	
}
