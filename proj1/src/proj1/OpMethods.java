package proj1;
import java.rmi.Remote;
import java.rmi.RemoteException;

/* 
 * Class containing the operation methods to be called by RMI
 */
public interface OpMethods extends Remote {

	/*
	 * Handles the backup operation
	 * @param filepath Path of the file to backup
	 * @param replication_degree Desired replication degree
	 */
	void backup(String filepath, String repDeg) throws RemoteException;

	void restore(String filename) throws RemoteException;

	void delete(String filename) throws RemoteException;

	void reclaim(int available_space) throws RemoteException;

	void state() throws RemoteException;
}
