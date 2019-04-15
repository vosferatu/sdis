package client;

import java.rmi.registry.Registry;
import server.Protocol;
import java.rmi.registry.LocateRegistry;

class TestApp {

    private static String peer_ap, sub_protocol, path = "";
    private static int repDegree, maxSize;
    private static Protocol stub;

    public static void main(String[] args) {

    	if (args.length < 2) {
    		System.out.println("Usage: TestApp <access_point> <sub_protocol> [<opnd_1> <opnd_2>]");
            System.exit(1);
        }

        peer_ap = args[0];
        sub_protocol = args[1].toUpperCase();
            
        try {
            Registry registry = LocateRegistry.getRegistry("localhost");
            stub = (Protocol) registry.lookup(peer_ap);

            switch (sub_protocol) {
            
                case "BACKUP":

                    if (args.length != 4) {
                        System.out.println("Usage: TestApp <peer_ap> BACKUP <path> <rep_degree>");
                        return;
                    }

                    path = args[2];
                    repDegree = Integer.parseInt(args[3]);
                    stub.backup(path, repDegree);
                    break;
                    
                case "RESTORE":

                    if (args.length != 3) {
                        System.out.println("Usage: TestApp <peer_ap> RESTORE <path>");
                        return;
                    }
                    
                    path = args[2];
                    stub.restore(path);
                    break;
                    
                case "DELETE":

                    if (args.length != 3) {
                        System.out.println("Usage: TestApp <peer_ap> DELETE <path>");
                        return;
                    }

                    path = args[2];
                    stub.delete(path);
                    break;
                    
                case "RECLAIM":

                    if (args.length != 3) {
                        System.out.println("Usage: TestApp <peer_ap> RECLAIM <space>");
                        return;
                    }

                    maxSize = Integer.parseInt(args[2]);
                    
                    if(maxSize < 0)
                    	maxSize = 0;
                    stub.reclaim(maxSize);

                    break;
                    
                case "STATE":

                    if (args.length != 2) {
                        System.out.println("Usage: TestApp <peer_ap> STATE");
                        return;
                    }

                    stub.state();
                    break;
            }

        } catch (Exception e) {
            System.out.println("TestApp error: " + e.toString());
            e.printStackTrace();
        }
    }
}
