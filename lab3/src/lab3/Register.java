package lab3;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Register extends Remote {

    int register(String plate_number, String owner_name) throws RemoteException;

    String lookup(String plate_number) throws RemoteException;
}
