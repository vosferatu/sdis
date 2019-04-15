package communication;

import server.Peer;

public class Sender implements Runnable {

    private String type;
    private byte[] message;


    public Sender(byte[] msg, String type) {
        this.message = msg;
        this.type = type;
    }

    public void run() {
        switch (type) {
            case "CONTROL":
                Peer.getControl().message(this.message);
                break;
            case "BACKUP":
                Peer.getBackup().message(this.message);
                break;
            case "RESTORE":
                Peer.getRestore().message(this.message);
                break;
        }
    }
}
