package protocols;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import communication.Mailman;
import files.Chunk;
import server.Peer;

public class Putchunk implements Runnable {

	private String fileId;
    private int chunkNo;
    private int version;
    private int repDegree;
    private byte[] data;

    public Putchunk(int version, String fileId, int chunkNo, int repDegree, byte[] data) {
        this.version = version;
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.repDegree = repDegree;
        this.data = data;
    }

    @Override
    public void run() {
        String pair = fileId + "_" + chunkNo;

        for(int i = 0; i < Peer.getDatabase().getFiles().size(); i++){
            if(Peer.getDatabase().getFiles().get(i).getId().equals(fileId))
                return;
        }

        if (version == 2) {
            if (Peer.getDatabase().getStoredEvents().get(pair) >= repDegree)
                return;
        }

        if (Peer.getDatabase().getSpace() >= data.length ) {
            Chunk chunk = new Chunk(chunkNo, fileId, repDegree, data.length);

            if (!Peer.getDatabase().addSavedChunk(chunk)) {
                return;
            }

            Peer.getDatabase().subSpace(data.length);

            try {
                String name = Peer.getId() + "/" + fileId + "_" + chunkNo;

                File f = new File(name);
                if (!f.exists()) {
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                }

                try (FileOutputStream fs = new FileOutputStream(name)) {
                    fs.write(data);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            Peer.getDatabase().addEvent(fileId, chunkNo);
            String header = "STORED " + version + " " + Peer.getId() + " " + fileId + " " + chunkNo;;
            System.out.println("Sent " + header); header += Mailman.CRLF + Mailman.CRLF;
            Peer.getControl().message(header.getBytes());
        } else {
            System.out.println("NOT ENOUGH SPACE FOR: " + fileId + "_" + chunkNo);
        }

    }
}
