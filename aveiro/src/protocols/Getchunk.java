package protocols;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import communication.Mailman;
import communication.Sender;
import server.Peer;

public class Getchunk implements Runnable {


    private int chunkNo;
    private String fileId;

    public Getchunk(String fileId, int chunkNo) {
        this.chunkNo = chunkNo;
        this.fileId = fileId;
    }

    @Override
    public void run() {
        for (int i = 0; i < Peer.getDatabase().getSavedChunks().size(); i++) {
            if (compareChunks(Peer.getDatabase().getSavedChunks().get(i).getFileId(), Peer.getDatabase().getSavedChunks().get(i).getChunkNo()) && !aborted()) {
                String header = "CHUNK " + "1.0" + " " + Peer.getId() + " " + this.fileId + " " + this.chunkNo + Mailman.CRLF + Mailman.CRLF;

                try {
                    Random random = new Random();

                    byte[] ascii = header.getBytes("US-ASCII");

                    String name = Peer.getId() + "/" + fileId + "_" + chunkNo;

                    File f = new File(name);
                    byte[] body = new byte[(int) f.length()];
                    FileInputStream fs = new FileInputStream(f);
                    fs.read(body);

                    byte[] msg = new byte[ascii.length + body.length];
                    System.arraycopy(ascii, 0, msg, 0, ascii.length);
                    System.arraycopy(body, 0, msg, ascii.length, body.length);

                    Sender sender = new Sender(msg, "RESTORE");
                    System.out.println("Sent" + "CHUNK " + "1.0" + " " + Peer.getId() + " " + this.fileId + " " + this.chunkNo);

                    Peer.getPool().schedule(sender, random.nextInt(401), TimeUnit.MILLISECONDS);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private boolean aborted() {
        for (int i = 0; i < Peer.getDatabase().getSustainedChunks().size(); i++) {
            if (compareChunks(Peer.getDatabase().getSustainedChunks().get(i).getFileId(), Peer.getDatabase().getSustainedChunks().get(i).getChunkNo()))
                return true;
        }
        return false;
    }

    private boolean compareChunks(String fileId, int chunkNo) {
        return chunkNo == this.chunkNo && fileId.equals(this.fileId);
    }
    
}
