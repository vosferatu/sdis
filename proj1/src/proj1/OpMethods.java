package proj1;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface OpMethods extends Remote {

	void backup(String filename, int replication_degree);

	void restore(String filename);

	void delete(String filename);

	void reclaim(int available_space);

	void state();
}
