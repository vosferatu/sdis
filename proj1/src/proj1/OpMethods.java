package proj1;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface OpMethods extends Remote {

	void backup(String filename, int replication_degree) throws RemoteException;

	void restore(String filename) throws RemoteException;

	void delete(String filename) throws RemoteException;

	void reclaim(int available_space) throws RemoteException;

	void state() throws RemoteException;
}
