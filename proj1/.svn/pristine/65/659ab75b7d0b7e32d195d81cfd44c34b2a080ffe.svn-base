package protocols;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import server.Peer;

import java.util.ArrayList;


public class Restore implements Runnable {

    private String name;

    public Restore(String file) {
        this.name = file;
    }

    @Override
    public void run() {
    	//System.out.println("\nrow:\n" + Peer.getDatabase().getSearchingChunks());

        if (!Peer.getDatabase().getSearchingChunks().containsValue("false")) {
            if (restored())
                System.out.println("> File <" + this.name + "> restored!\n");
            else System.out.println("WARNING: File <" + this.name + "> wasn't restored.\n");
        } else System.out.println("WARNING: File <"  + this.name + "> wasn't restored because chunks are missing.\n");

        Peer.getDatabase().getSearchingChunks().clear();
    }

    private boolean restored() {
        String path = Peer.getId() + "/" + this.name;
        File f = new File(path);
        byte[] body;

        try {
            FileOutputStream fs = new FileOutputStream(f, true);

            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }

            List<String> chunkPairs = new ArrayList<>(Peer.getDatabase().getSearchingChunks().keySet());

            chunkPairs.sort((o1, o2) -> {
                int one = Integer.valueOf(o1.split("_")[1]), other = Integer.valueOf(o2.split("_")[1]);
                return Integer.compare(one, other);
            });

            for (String pair : chunkPairs) {
                String cp = Peer.getId() + "/" + pair;

                File cf = new File(cp);
                if (!cf.exists()) {
                    return false;
                }

                body = new byte[(int) cf.length()];
                FileInputStream fsi = new FileInputStream(cf);

                fsi.read(body);
                fs.write(body);

                cf.delete();
            }

            fs.close();
            return true;
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
