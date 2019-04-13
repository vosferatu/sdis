package communication;

import java.util.concurrent.TimeUnit;

import server.Peer;

public class SendPutchunk implements Runnable {
	
    private byte[] msg;
    private int time, repDegree, timeout;
    private String pair;

    public SendPutchunk(byte[] msg, int time, String fileId, int chunkNo, int repDegree) {
        this.msg = msg;
        this.time = time;
        this.timeout = 1;
        this.repDegree = repDegree;

        this.pair = fileId + '_' + chunkNo;
    }

    @Override
    public void run() {

        int events = Peer.getDatabase().getStoredEvents().get(this.pair);

        if (events < repDegree) {
            Peer.getBackup().message(msg);
            
            System.out.println("PUTCHUNK no of tries: " + timeout);
            
            this.timeout++;

            this.time += this.time;

            if (this.timeout < 5)
                Peer.getPool().schedule(this, this.time, TimeUnit.SECONDS);
        }
    }

}
