package proj1;

import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {

    private TestApp() {}

	public static void main(String args[]) throws IOException {
		if(args.length > 4) {
			System.out.println("USAGE: java TestApp <peer_ap> <operation> <opnd_1> <opnd_2>");
			System.exit(1);
		}
		
		String access_point = args[0];
		String operation_arg = args[1];
		
        try {
            Registry registry = LocateRegistry.getRegistry();
            OpMethods stub = (OpMethods) registry.lookup(access_point);
    		
    		switch(operation_arg) {
    			case "BACKUP":
    				/* filename = args[3] | replication_degree = args[4] */
    				stub.backup(args[3], Integer.parseInt(args[4]));
    				break;
    			case "RESTORE":
    				/* filename = args[3] */
    				stub.restore(args[3]);
    				break;
    			case "DELETE":
    				/* filename = args[3] */
    				stub.delete(args[3]);
    				break;
    			case "RECLAIM":
    				/* available_space = args[3] */
    				stub.reclaim(Integer.parseInt(args[3]));
    				break;
    			case "STATE":
    				stub.state();
    				break;
    			default:
    				System.err.println("Operation not supported!");
    		}
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
	}
}
