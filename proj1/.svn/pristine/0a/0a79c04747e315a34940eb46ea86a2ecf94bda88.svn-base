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
    private static Database db;

    private static Channel Control, Backup, Restore;
    private static ScheduledThreadPoolExecutor pool;

    private Peer(InetAddress mcAddress, int mcPort, InetAddress mdbAddress, int mdbPort, InetAddress mdrAddress, int mdrPort) {
        Backup = new Channel(mdbAddress, mdbPort);
        Control = new Channel(mcAddress, mcPort);
        Restore = new Channel(mdrAddress, mdrPort);
        pool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(300);
    }

    public static ScheduledThreadPoolExecutor getPool() {
        return pool;
    }
    
    public static int getId() {
        return id;
    }
    
    public static Channel getBackup() {
        return Backup;
    }

    public static Channel getControl() {
        return Control;
    }
    
    public static Channel getRestore() {
        return Restore;
    }
    
    public static Database getDatabase() {
        return db;
    }

    public static void main(String args[]) {

        try {

            if(args.length != 6){
                System.err.println("Usage: java Peer <version> <peer_id> <access_point> <MC>:<MC_port> <MDB>:<MDB_port> <MDR>:<MDR_port>");
                return;
            }
            
            InetAddress mcAddress = InetAddress.getByName((String)args[3].split(":")[0]);
            int mcPort = Integer.parseInt(args[3].split(":")[1]);

            InetAddress mdbAddress = InetAddress.getByName((String) args[4].split(":")[0]);
            int mdbPort = Integer.parseInt(args[4].split(":")[1]);

            InetAddress mdrAddress = InetAddress.getByName((String) args[5].split(":")[0]);
            int mdrPort = Integer.parseInt(args[5].split(":")[1]);

            version = Integer.parseInt(args[0]);
            id = Integer.parseInt(args[1]);
            String access_point = args[2];

            Peer obj = new Peer(mcAddress, mcPort, mdbAddress, mdbPort, mdrAddress, mdrPort);
            Protocol stub = (Protocol) UnicastRemoteObject.exportObject(obj, 0);

            Registry registry = LocateRegistry.getRegistry();

            if (loadedDatabase())
                registry.rebind(access_point, stub);
            else registry.bind(access_point, stub);

            System.err.println("Peer " + id + ":" + access_point +  " up");
        } catch (Exception e) {
            System.err.println("Peer " + id + " ERROR" + e.toString());
            e.printStackTrace();
        }
       /*
        System.out.println(Backup);
        System.out.println(Control);
        System.out.println(Restore);
        */
        pool.execute(Backup);
        pool.execute(Control);
        pool.execute(Restore);

        Runtime.getRuntime().addShutdownHook(new Thread(Peer::serializeDatabase));
    }


    public void restore(String path) {

        String name = null;

        for (int i = 0; i < db.getFiles().size(); i++) {
            if (db.getFiles().get(i).getDescriptor().getPath().equals(path)) {
                for (int k = 0; k < db.getFiles().get(i).getChunks().size(); k++) {

                    String header = "GETCHUNK " + version + " " + id + " " + 
                    db.getFiles().get(i).getId() + " " + 
                    		db.getFiles().get(i).getChunks().get(k).getChunkNo();
                    System.out.println("Sending "+ header );
                    
                    header += Mailman.CRLF + Mailman.CRLF;

                    db.addSearchingChunk(db.getFiles().get(i).getId(), db.getFiles().get(i).getChunks().get(k).getChunkNo());
                    name = db.getFiles().get(i).getDescriptor().getName();
                    
                    try {
                        byte[] msg = header.getBytes("US-ASCII");
                        Sender sender = new Sender(msg, "RESTORE");

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
            Chunk tmp = f.getChunks().get(i);
            tmp.setRepDegree(repDegree);

            String header = "PUTCHUNK " + version + " " + id + " " + f.getId() + " " + tmp.getChunkNo() + " " + tmp.getRepDegree();
            System.out.println("Sending " + header);
            header += Mailman.CRLF + Mailman.CRLF;

            String pair = f.getId() + "_" + tmp.getChunkNo();
            if (!db.getStoredEvents().containsKey(pair)) {
                Peer.getDatabase().getStoredEvents().put(pair, 0);
            }
            byte[] body = tmp.getData();

            try {
                byte[] ascii = header.getBytes("US-ASCII");
                byte[] msg = new byte[ascii.length + body.length];
                System.arraycopy(ascii, 0, msg, 0, ascii.length);
                System.arraycopy(body, 0, msg, ascii.length, body.length);

                Sender sender = new Sender(msg, "BACKUP");
                pool.execute(sender);
                Thread.sleep(400);
                Peer.getPool().schedule(new SendPutchunk(msg, 1, f.getId(), tmp.getChunkNo(), repDegree), 1, TimeUnit.SECONDS);

            } catch (InterruptedException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public void reclaim(int spaceOn) {

        int spaceUp = db.getFilledSpace() - spaceOn;

        System.out.println("RECLAIM: previous space - " + db.getSpace());

        if (spaceUp > 0) {
            db.checkRepDegreeChunks();
            db.getSavedChunks().sort(Collections.reverseOrder());

            int freedSpace = 0;

            for (Iterator<Chunk> iter = db.getSavedChunks().iterator(); iter.hasNext(); ) {
                Chunk tmp = iter.next();
                if (freedSpace < spaceUp) {
                    freedSpace = freedSpace + tmp.getSize();

                    String header = "REMOVED " + version + " " + id + " " + tmp.getFileId() + " " + tmp.getChunkNo();
                    System.out.println("Sending " + header); header += Mailman.CRLF + Mailman.CRLF;
                    try {
                        byte[] msg = header.getBytes("US-ASCII");
                        Sender sender = new Sender(msg, "CONTROL");
                        pool.execute(sender);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    String name = Peer.getId() + "/" + tmp.getFileId() + "_" + tmp.getChunkNo();
                    File f = new File(name);
                    f.delete();
                    Peer.getDatabase().subStoredEvents(tmp.getFileId(), tmp.getChunkNo());
                    iter.remove();
                } else {
                    break;
                }
            }
            
            System.out.println("RECLAIM: updated space - " + db.getSpace());

            db.setSpace(spaceOn - db.getFilledSpace());
        }

        //db.setSpace(spaceOn + db.getFilledSpace());

    }

    public void delete(String path) {

        for (int i = 0; i < db.getFiles().size(); i++) {
            if (db.getFiles().get(i).getDescriptor().getPath().equals(path)) {

                for (int k = 0; k < 5; k++) {
                    String header = "DELETE " + version + " " + id + " " + db.getFiles().get(i).getId();
                    System.out.println("Sending DELETE " + header);
                    header += Mailman.CRLF + Mailman.CRLF;
                    
                    try {
                    	byte[] msg = header.getBytes("US-ASCII");
                        Sender sender = new Sender(msg, "CONTROL");
                        pool.execute(sender);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

                for (int k = 0; k < db.getFiles().get(i).getChunks().size(); k++) {
                    db.removeStoredEvent(db.getFiles().get(i).getId(), db.getFiles().get(i).getChunks().get(k).getChunkNo());
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

            System.out.println("Filepath: " + db.getFiles().get(i).getDescriptor().getPath());
            System.out.println("FileId: " + fileId);
            System.out.println("File RepDegree: " + db.getFiles().get(i).getRepDegree() + "\n");

            for (int k = 0; k < db.getFiles().get(i).getChunks().size(); k++) {
                int chunkNo = db.getFiles().get(i).getChunks().get(k).getChunkNo();
                String pair = fileId + '_' + chunkNo;

                System.out.println("ChunkNo: " + chunkNo);
                System.out.println("Chunk RealRepDegree: " + db.getStoredEvents().get(pair) + "\n");
            }
        }

        System.out.println("\n->For each chunk");
        for (int i = 0; i < db.getSavedChunks().size(); i++) {
            int chunkNo = db.getSavedChunks().get(i).getChunkNo();
            String pair = db.getSavedChunks().get(i).getFileId() + '_' + chunkNo;
            System.out.println("ChunkNo: " + chunkNo);
            System.out.println("Chunk RealRepDegree: " + db.getStoredEvents().get(pair) + "\n");
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
            
            return true;

        } catch (IOException i) {
            i.printStackTrace();
            return false;
        } catch (ClassNotFoundException c) {
            System.out.println("Database unavailable");
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
