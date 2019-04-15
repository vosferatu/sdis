package communication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import files.Chunk;
import protocols.*;
import server.Peer;

public class Mailman implements Runnable {

    public static final byte CR = 0xD;
    public static final byte LF = 0xA;
    public static final String CRLF = "\r\n";


    public static final String PUTCHUNK = "PUTCHUNK";
    public static final String GETCHUNK = "GETCHUNK";
    public static final String CHUNK = "CHUNK";
    public static final String STORED = "STORED";
    public static final String DELETE = "DELETE";
    public static final String REMOVED = "REMOVED";

    private byte[] msg;
    private byte[] body;
    private int version;
    private int senderId;
    private String fileId;
    private String msgType;
    private int chunkNo;
    private int repDegree;

    public Mailman(byte[] msg) {
        this.msg = msg;
    }

    public void run() {
        msgType = new String(this.msg, 0, this.msg.length).trim().split(" ")[0];

        switch (msgType) {
            case STORED:
                stored();
                break;
            case GETCHUNK:
                getChunk();
                break;
            case DELETE:
                delete();
                break;
            case REMOVED:
                removed();
                break;
            case PUTCHUNK:
                putChunk();
                break;
            case CHUNK:
                chunk();
                break;
        }
    }
    
    private void saveChunks(String fileId, int chunkNo, byte[] body) {

        try {
            String name = Peer.getId() + "/" + fileId + "_" + chunkNo;

            File f = new File(name);
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }

            try (FileOutputStream fs = new FileOutputStream(name)) {
                fs.write(body);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void putChunk() {

        int i;
        for (i = 0; i < this.msg.length - 4; i++) {
            if (this.msg[i] == CR && this.msg[i + 1] == LF && this.msg[i + 2] == 0xD && this.msg[i + 3] == LF) {
                break;
            }
        }

        body = Arrays.copyOfRange(this.msg, i + 4, this.msg.length);
        byte[] header = Arrays.copyOfRange(this.msg, 0, i);

        String[] args = new String(header).trim().split(" ");

        version = Integer.parseInt(args[1].trim());
        senderId = Integer.parseInt(args[2].trim());
        fileId = args[3].trim();
        chunkNo = Integer.parseInt(args[4].trim());
        repDegree = Integer.parseInt(args[5].trim());

        String key = fileId + "_" + chunkNo;
        if (!Peer.getDatabase().getStoredEvents().containsKey(key)) {
            Peer.getDatabase().getStoredEvents().put(key, 0);
        }

        if (Peer.getId() != senderId) {
            Random random = new Random();
            Peer.getPool().schedule(new Putchunk(version, fileId, chunkNo, repDegree, body), random.nextInt(401), TimeUnit.MILLISECONDS);
            System.out.println("Received PUTCHUNK " + version + " " + senderId + " " + fileId + " " + chunkNo + " " + repDegree);
        }
    }
    
    private void getChunk() {

        int i;
        for (i = 0; i < this.msg.length - 4; i++) {
            if (this.msg[i] == CR && this.msg[i + 1] == LF && this.msg[i + 2] == 0xD && this.msg[i + 3] == LF) {
                break;
            }
        }

        body = Arrays.copyOfRange(this.msg, i + 4, this.msg.length);
        byte[] header = Arrays.copyOfRange(this.msg, 0, i);

        String[] args = new String(header).trim().split(" ");

        version = Integer.parseInt(args[1].trim());
        senderId = Integer.parseInt(args[2].trim());
        fileId = args[3].trim();
        chunkNo = Integer.parseInt(args[4].trim());

        if (Peer.getId() != senderId) {
            Random random = new Random();
            Peer.getPool().schedule(new Getchunk(fileId, chunkNo), random.nextInt(401), TimeUnit.MILLISECONDS);
            System.out.println("Received GETCHUNK " + version + " " + senderId + " " + fileId + " " + chunkNo);

        }
    }

    private synchronized void stored() {

        int i;
        for (i = 0; i < this.msg.length - 4; i++) {
            if (this.msg[i] == CR && this.msg[i + 1] == LF && this.msg[i + 2] == 0xD && this.msg[i + 3] == LF) {
                break;
            }
        }

        body = Arrays.copyOfRange(this.msg, i + 4, this.msg.length);
        byte[] header = Arrays.copyOfRange(this.msg, 0, i);

        String[] args = new String(header).trim().split(" ");

        version = Integer.parseInt(args[1].trim());
        senderId = Integer.parseInt(args[2].trim());
        fileId = args[3].trim();
        chunkNo = Integer.parseInt(args[4].trim());

        if (Peer.getId() != senderId) {
            Peer.getDatabase().addEvent(fileId, chunkNo);
            System.out.println("Received STORED " + version + " " + senderId + " " + fileId + " " + chunkNo);
        }
    }
    

    private void chunk() {

        int i;
        for (i = 0; i < this.msg.length - 4; i++) {
            if (this.msg[i] == CR && this.msg[i + 1] == LF && this.msg[i + 2] == 0xD && this.msg[i + 3] == LF) {
                break;
            }
        }

        body = Arrays.copyOfRange(this.msg, i + 4, this.msg.length);
        byte[] header = Arrays.copyOfRange(this.msg, 0, i);

        String[] args = new String(header).trim().split(" ");

        Double version = Double.parseDouble(args[1].trim());
        senderId = Integer.parseInt(args[2].trim());
        fileId = args[3].trim();
        chunkNo = Integer.parseInt(args[4].trim());

        if (Peer.getId() != senderId) {
            Chunk tmp = new Chunk(chunkNo, fileId, 0, 0);
            Peer.getDatabase().getSustainedChunks().add(tmp);

            if (!Peer.getDatabase().getSearchingChunks().isEmpty()) {
                Peer.getDatabase().setSearchingChunk(fileId, chunkNo);
                saveChunks(fileId, chunkNo, body);
            }
            System.out.println("Received CHUNK " + version + " " + senderId + " " + fileId + " " + chunkNo);
        }
    }

    private void delete() {

        int i;
        for (i = 0; i < this.msg.length - 4; i++) {
            if (this.msg[i] == CR && this.msg[i + 1] == LF && this.msg[i + 2] == 0xD && this.msg[i + 3] == LF) {
                break;
            }
        }

        body = Arrays.copyOfRange(this.msg, i + 4, this.msg.length);
        byte[] header = Arrays.copyOfRange(this.msg, 0, i);

        String[] args = new String(header).trim().split(" ");

        version = Integer.parseInt(args[1].trim());
        senderId = Integer.parseInt(args[2].trim());
        fileId = args[3].trim();

        if (Peer.getId() != senderId) {
            Peer.getDatabase().deleteSavedChunks(fileId);
            System.out.println("Received DELETE " + version + " " + senderId + " " + fileId);
        }
    }


    private void removed() {

        int i;
        for (i = 0; i < this.msg.length - 4; i++) {
            if (this.msg[i] == CR && this.msg[i + 1] == LF && this.msg[i + 2] == 0xD && this.msg[i + 3] == LF) {
                break;
            }
        }

        body = Arrays.copyOfRange(this.msg, i + 4, this.msg.length);
        byte[] header = Arrays.copyOfRange(this.msg, 0, i);

        String[] args = new String(header).trim().split(" ");

        version = Integer.parseInt(args[1].trim());
        senderId = Integer.parseInt(args[2].trim());
        fileId = args[3].trim();
        chunkNo = Integer.parseInt(args[4].trim());

        if (Peer.getId() != senderId) {
            Peer.getDatabase().removeStoredEvent(fileId, chunkNo);
            Random random = new Random();
            Peer.getPool().schedule(new Removed(fileId, chunkNo), random.nextInt(401), TimeUnit.MILLISECONDS);
            System.out.println("Received REMOVED " + version + " " + senderId + " " + fileId + " " + chunkNo);
        }
    }

}
