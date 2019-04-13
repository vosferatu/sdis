package protocols;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import communication.Mailman;
import communication.Sender;
import server.Peer;
import communication.SendPutchunk;

public class Removed implements Runnable {

    private String fileId;
    private int chunkNo;

    public Removed(String fileId, int chunkNo) {
        this.chunkNo = chunkNo;
        this.fileId = fileId;

    }

    @Override
    public void run() {

        boolean chunky = false;
        int repDegree = 0;

        for (int i = 0; i < Peer.getDatabase().getSavedChunks().size(); i++) {
            if (Peer.getDatabase().getSavedChunks().get(i).getChunkNo() == chunkNo && Peer.getDatabase().getSavedChunks().get(i).getFileId().equals(fileId)) {
                chunky = true;
                repDegree = Peer.getDatabase().getSavedChunks().get(i).getRepDegree();
                break;
            }
        }

        if (chunky) {
            String pair = fileId + '_' + chunkNo;

            if (Peer.getDatabase().getStoredEvents().get(pair) < repDegree) {

                int chunkSize = 64000;
                byte[] body = new byte[chunkSize];
                byte[] buf = new byte[chunkSize];

                File f = new File(Peer.getId() + "/" + fileId + "_" + chunkNo);
                try (FileInputStream fs = new FileInputStream(f);
                     BufferedInputStream bs = new BufferedInputStream(fs)) {

                    int bytes;
                    while ((bytes = bs.read(buf)) > 0) {
                        body = Arrays.copyOf(buf, bytes);
                        buf = new byte[chunkSize];
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

                String header = "PUTCHUNK " + "1.0" + " " + Peer.getId() + " " + fileId + " " + chunkNo + " " + repDegree;

                System.out.println("Sent " + header); header += Mailman.CRLF + Mailman.CRLF;


                if (!Peer.getDatabase().getStoredEvents().containsKey(pair)) {
                    Peer.getDatabase().getStoredEvents().put(pair, 0);
                }

                byte[] ascii;

                try {
                    ascii = header.getBytes("US-ASCII");
                    byte[] msg = new byte[ascii.length + body.length];
                    System.arraycopy(ascii, 0, msg, 0, ascii.length);
                    System.arraycopy(body, 0, msg, ascii.length, body.length);
                    Sender sender = new Sender(msg, "BACKUP");
                    Peer.getPool().execute(sender);
                    Peer.getPool().schedule(new SendPutchunk(msg, 1, fileId, chunkNo, repDegree), 1, TimeUnit.SECONDS);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

        }

    }
}
