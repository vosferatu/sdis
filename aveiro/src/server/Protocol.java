package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Protocol extends Remote {

    void backup(String path, int replicationDegree) throws RemoteException;

    void restore(String path) throws RemoteException;

    void delete(String path) throws RemoteException;

    void reclaim(int maxSpace) throws RemoteException;

    void state() throws RemoteException;

}
