package lab3;
import java.io.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    private Client() {}

	public static void main(String args[]) throws IOException {

		if(args.length < 4) {
			System.out.println("USAGE: java Client <host_name> <remote_object_name> <oper> <opnd>*");
			System.exit(1);
		}

		String host_name = args[0];
		String remote_object_name = args[1];
		String oper = args[2];
		Object response = new Object();

        try {
            Registry registry = LocateRegistry.getRegistry(host_name);
            Register stub = (Register) registry.lookup(remote_object_name);

    		if(oper.equals("register")) {
    			//Verifica se ha 5 args
    			if(args.length != 5) {
    				System.out.println("WRONG NUMBER OF ARGUMENTS");
    				System.exit(1);
    			}
                response = stub.register(args[3], args[4]);
    		}
    		else {
    			if(oper.equals("lookup")) {
    				//Verifica se ha 4 args
    				if(args.length != 4) {
    					System.out.println("WRONG NUMBER OF ARGUMENTS");
    					System.exit(1);
    				}

                    response = stub.lookup(args[3]);
    			}
    			else {
    				System.out.println("OPERATION NOT SUPPORTED");
    				System.exit(2);
    			}
    		}

            System.out.println("response: " + response.toString());
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
	}

}
