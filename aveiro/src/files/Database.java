package files;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import server.Peer;

public class Database implements java.io.Serializable {
	
    private ArrayList<FileSystem> files;
    private ArrayList<Chunk> savedChunks;
    private ArrayList<Chunk> sustainedChunks;
    private ConcurrentHashMap<String, Integer> storedEvents; // fileid->chunkno
    private ConcurrentHashMap<String, String> searchingChunks;
    private int space;

    public Database() {
        this.files = new ArrayList<>();
        this.savedChunks = new ArrayList<>();
        this.sustainedChunks = new ArrayList<>();
        this.storedEvents = new ConcurrentHashMap<>();
        this.searchingChunks = new ConcurrentHashMap<>();
        this.space = 1000000000;
    }

    public ArrayList<FileSystem> getFiles() {
        return this.files;
    }

    public ArrayList<Chunk> getSustainedChunks() {
        return this.sustainedChunks;
    }
    
    public synchronized ArrayList<Chunk> getSavedChunks() {
        return this.savedChunks;
    }

    public ConcurrentHashMap<String, String> getSearchingChunks() {
        return this.searchingChunks;
    }

    public synchronized ConcurrentHashMap<String, Integer> getStoredEvents() {
        return this.storedEvents;
    }
    
    public void addFile(FileSystem file) {
        this.files.add(file);
    }

    public void deleteSavedChunks(String fileId) {
        for (Iterator<Chunk> iter = this.savedChunks.iterator(); iter.hasNext(); ) {
            Chunk chunk = iter.next();
            if (chunk.getFileId().equals(fileId)) {
                String name = Peer.getId() + "/" + fileId + "_" + chunk.getChunkNo();
                File f = new File(name);
                f.delete();
                removeStoredEvent(fileId, chunk.getChunkNo());
                addSpace(fileId, chunk.getChunkNo());
                iter.remove();
            }
        }
    }

    public synchronized void addEvent(String fileId, int chunkNo) {

        String pair = fileId + '_' + chunkNo;

        if (!Peer.getDatabase().getStoredEvents().containsKey(pair)) {
            Peer.getDatabase().getStoredEvents().put(pair, 1);
        } else {
            int n = this.storedEvents.get(pair) + 1;
            this.storedEvents.replace(pair, n);
        }

    }
    
    public synchronized boolean addSavedChunk(Chunk chunk) {

        for (Chunk savedChunk : this.savedChunks) {
            if (savedChunk.getChunkNo() == chunk.getChunkNo() && savedChunk.getFileId().equals(chunk.getFileId()))
                return false;
        }
        this.savedChunks.add(chunk);
        return true;
    }

    public synchronized void subStoredEvents(String fileId, int chunkNo) {
        String pair = fileId + '_' + chunkNo;
        int n = this.storedEvents.get(pair) - 1;
        this.storedEvents.replace(pair, n);
    }

    public void checkRepDegreeChunks() {
        for (Chunk savedChunk : this.savedChunks) {
            String pair = savedChunk.getFileId() + "_" + savedChunk.getChunkNo();
            savedChunk.setRealRepDegree(this.storedEvents.get(pair));
        }
    }
    
    public synchronized void removeStoredEvent(String fileId, int chunkNo){
        String pair = fileId + '_' + chunkNo;
        this.storedEvents.remove(pair);
    }
    

    public void addSearchingChunk(String fileId, int chunkNo) {
        String pair = fileId + '_' + chunkNo;
        this.searchingChunks.put(pair, "false");
    }


    public synchronized int getSpace() {
        return this.space;
    }
    
    public void setSearchingChunk(String fileId, int chunkNo) {
        String pair = fileId + '_' + chunkNo;
        this.searchingChunks.replace(pair, "true");
    }

    public synchronized void subSpace(int value){
        space = space - value;
    }
    
    public synchronized int getFilledSpace(){
        int leftSpace = 0;
        for (Chunk savedChunk : this.savedChunks) {
            leftSpace += savedChunk.getSize();
        }
        return leftSpace;
    }
    
    public synchronized void setSpace(int space) {
        this.space = space;
    }

    public synchronized void addSpace(String fileId, int chunkNo){
        for (Chunk savedChunk : this.savedChunks) {
            if (savedChunk.getFileId().equals(fileId) && savedChunk.getChunkNo() == chunkNo)
                this.space = this.space + savedChunk.getSize();
        }
    }

}
