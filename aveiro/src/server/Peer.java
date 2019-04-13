package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import communication.*;
import files.*;
import protocols.*;

public class Peer implements Protocol {
	
    private static int id, version;

    private static ControlChannel Control;
    private static BackupChannel Backup;
    private static RestoreChannel Restore;
    private static ScheduledThreadPoolExecutor pool;
    private static Database db;

    private Peer(InetAddress MCAddress, int MCPort, InetAddress MDBAddress, int MDBPort, InetAddress MDRAddress, int MDRPort) {
        pool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(250);
        Control = new ControlChannel(MCAddress, MCPort);
        Backup = new BackupChannel(MDBAddress, MDBPort);
        Restore = new RestoreChannel(MDRAddress, MDRPort);
    }

    public static int getId() {
        return id;
    }

    public static ScheduledThreadPoolExecutor getPool() {
        return pool;
    }

    public static ControlChannel getControl() {
        return Control;
    }
    
    public static RestoreChannel getRestore() {
        return Restore;
    }


    public static BackupChannel getBackup() {
        return Backup;
    }

    public static Database getDatabase() {
        return db;
    }

    public static void main(String args[]) {

        try {

            if(args.length != 9){
                System.out.println("Usage: Peer <version> <peer_id> <access_point> <MC_IP> <MC_port> <MDB_IP> <MDB_port> <MDR_IP> <MDR_port>");
                return;
            }

            version = Integer.parseInt(args[0]);
            id = Integer.parseInt(args[1]);
            String accessP = args[2];
            int MCPort = Integer.parseInt(args[4]);
            int MDBPort = Integer.parseInt(args[6]);
            int MDRPort = Integer.parseInt(args[8]);

            InetAddress MCAddress = InetAddress.getByName((String) args[3]);
            InetAddress MDBAddress = InetAddress.getByName((String) args[5]);;
            InetAddress MDRAddress = InetAddress.getByName((String) args[7]);;

            Peer obj = new Peer(MCAddress, MCPort, MDBAddress, MDBPort, MDRAddress, MDRPort);
            Protocol stub = (Protocol) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();

            if (loadedDatabase())
                registry.rebind(accessP, stub);
            else registry.bind(accessP, stub);

            System.err.println("Peer " + id +  " ready");
        } catch (Exception e) {
            System.err.println("Peer " + id + " ERROR" + e.toString());
            e.printStackTrace();
        }

        pool.execute(Control);
        pool.execute(Backup);
        pool.execute(Restore);

        Runtime.getRuntime().addShutdownHook(new Thread(Peer::serializeDatabase));
    }


    public void restore(String path) {

        String name = null;

        for (int i = 0; i < db.getFiles().size(); i++) {
            if (db.getFiles().get(i).getFile().getPath().equals(path)) {
                for (int j = 0; j < db.getFiles().get(i).getChunks().size(); j++) {

                    String header = "GETCHUNK " + version + " " + id + " " + db.getFiles().get(i).getId() + " " + db.getFiles().get(i).getChunks().get(j).getChunkNo();
                    System.out.println("Sent "+ header );
                    
                    header += Mailman.CRLF + Mailman.CRLF;

                    db.addSearchingChunk(db.getFiles().get(i).getId(), db.getFiles().get(i).getChunks().get(j).getChunkNo());
                    name = db.getFiles().get(i).getFile().getName();

                    try {
                        Sender sender = new Sender(header.getBytes("US-ASCII"), "CONTROL");

                        pool.execute(sender);

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                Peer.getPool().schedule(new Restore(name), 10, TimeUnit.SECONDS);
            } else System.out.println("WARNING: File <" + name +  "> wasn't backed up.");
        }
    }


    public synchronized void backup(String path, int repDegree) {

        FileSystem f = new FileSystem(path, repDegree);
        db.addFile(f);

        for (int i = 0; i < f.getChunks().size(); i++) {
            Chunk chunk = f.getChunks().get(i);
            chunk.setRepDegree(repDegree);

            String header = "PUTCHUNK " + version + " " + id + " " + f.getId() + " " + chunk.getChunkNo() + " " + chunk.getRepDegree();
            System.out.println("Sent " + header);
            header += Mailman.CRLF + Mailman.CRLF;

            String pair = f.getId() + "_" + chunk.getChunkNo();
            if (!db.getStoredEvents().containsKey(pair)) {
                Peer.getDatabase().getStoredEvents().put(pair, 0);
            }

            try {
                byte[] body = chunk.getData();
                byte[] ascii = header.getBytes("US-ASCII");
                byte[] msg = new byte[ascii.length + body.length];
                System.arraycopy(ascii, 0, msg, 0, ascii.length);
                System.arraycopy(body, 0, msg, ascii.length, body.length);

                Sender sender = new Sender(msg, "BACKUP");
                pool.execute(sender);
                Thread.sleep(500);
                Peer.getPool().schedule(new SendPutchunk(msg, 1, f.getId(), chunk.getChunkNo(), repDegree), 1, TimeUnit.SECONDS);

            } catch (UnsupportedEncodingException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void reclaim(int spaceOn) {

        System.out.println("RECLAIM: previous space - " + db.getSpace());

        int spaceUp = db.getFilledSpace() - spaceOn;

        if (spaceUp > 0) {
            db.checkRepDegreeChunks();
            db.getSavedChunks().sort(Collections.reverseOrder());

            int freedSpace = 0;

            for (Iterator<Chunk> iter = db.getSavedChunks().iterator(); iter.hasNext(); ) {
                Chunk chunk = iter.next();
                if (freedSpace < spaceUp) {
                    freedSpace = freedSpace + chunk.getSize();

                    String header = "REMOVED " + version + " " + id + " " + chunk.getFileId() + " " + chunk.getChunkNo();
                    System.out.println("Sent " + header); header += Mailman.CRLF + Mailman.CRLF;
                    try {
                        byte[] ascii = header.getBytes("US-ASCII");
                        Sender sender = new Sender(ascii, "CONTROL");
                        pool.execute(sender);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    String name = Peer.getId() + "/" + chunk.getFileId() + "_" + chunk.getChunkNo();
                    File f = new File(name);
                    f.delete();
                    Peer.getDatabase().subStoredEvents(chunk.getFileId(), chunk.getChunkNo());
                    iter.remove();
                } else {
                    break;
                }
            }

            db.setSpace(spaceOn - db.getFilledSpace());
            System.out.println("RECLAIM: updated space - " + db.getSpace());
        }

        db.setSpace(spaceOn + db.getFilledSpace());

    }

    public void delete(String path) {

        for (int i = 0; i < db.getFiles().size(); i++) {
            if (db.getFiles().get(i).getFile().getPath().equals(path)) {

                for (int j = 0; j < 5; j++) {
                    String header = "DELETE " + version + " " + id + " " + db.getFiles().get(i).getId();
                    System.out.println("Send DELETE " + header);
                    header += Mailman.CRLF + Mailman.CRLF;
                    try {
                        Sender sender = new Sender(header.getBytes("US-ASCII"), "CONTROL");
                        pool.execute(sender);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                for (int j = 0; j < db.getFiles().get(i).getChunks().size(); j++) {
                    db.removeStoredEvent(db.getFiles().get(i).getId(), db.getFiles().get(i).getChunks().get(j).getChunkNo());
                }

                db.getFiles().remove(i);

                break;
            }
        }

    }


    public void state() {

        System.out.println("\n-> Each file that backup has initiated");
        for (int i = 0; i < db.getFiles().size(); i++) {
            String fileId = db.getFiles().get(i).getId();

            System.out.println("FILE PATHNAME: " + db.getFiles().get(i).getFile().getPath());
            System.out.println("FILE ID: " + fileId);
            System.out.println("FILE REPLICATION DEGREE: " + db.getFiles().get(i).getRepDegree() + "\n");

            for (int j = 0; j < db.getFiles().get(i).getChunks().size(); j++) {
                int chunkNo = db.getFiles().get(i).getChunks().get(j).getChunkNo();
                String pair = fileId + '_' + chunkNo;

                System.out.println("CHUNK ID: " + chunkNo);
                System.out.println("CHUNK PERCEIVED REPLICATION DEGREE: " + db.getStoredEvents().get(pair) + "\n");
            }
        }

        System.out.println("\n->For each chunk");
        for (int i = 0; i < db.getSavedChunks().size(); i++) {
            int chunkNo = db.getSavedChunks().get(i).getChunkNo();
            String pair = db.getSavedChunks().get(i).getFileId() + '_' + chunkNo;
            System.out.println("CHUNK ID: " + chunkNo);
            System.out.println("CHUNK PERCEIVED REPLICATION DEGREE: " + db.getStoredEvents().get(pair) + "\n");
        }
    }


    private static boolean loadedDatabase() {
        try {
            String name = Peer.getId() + "/storage.ser";

            File f = new File(name);
            if (!f.exists()) {
                db = new Database();
                return false;
            }

            FileInputStream fs = new FileInputStream(name);
            ObjectInputStream os = new ObjectInputStream(fs);
            db = (Database) os.readObject();
            os.close();
            fs.close();
            
            if(db.getFilledSpace() > 0)
            	return true;
            else return false;
            

        } catch (IOException i) {
            i.printStackTrace();
            return false;
        } catch (ClassNotFoundException c) {
            System.out.println("DB not found");
            c.printStackTrace();
            return false;
        }
    }

    private static void serializeDatabase() {
        try {
            String name = Peer.getId() + "/storage.ser";

            File f = new File(name);
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }

            FileOutputStream fs = new FileOutputStream(name);
            ObjectOutputStream os = new ObjectOutputStream(fs);
            os.writeObject(db);
            os.close();
            fs.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

}
