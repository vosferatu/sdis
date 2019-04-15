package files;

@SuppressWarnings("rawtypes")
public class Chunk implements java.io.Serializable,  Comparable  {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int chunkNo;
    private int size;
    private byte[] data;
    private String fileId;
    private int realRepDegree = 0;
    private int repDegree;

    public Chunk(int chunkNo, byte[] data, int size) {
        this.chunkNo = chunkNo;
        this.size = size;
        this.data = data;
    }

    public Chunk(int chunkNo, String fileId, int repDegree, int size) {
        this.chunkNo = chunkNo;
        this.size = size;
        this.repDegree = repDegree;
        this.fileId = fileId;
    }

    public int getChunkNo() {
        return chunkNo;
    }

    public String getFileId() {
        return this.fileId;
    }

    public int getSize() {
        return size;
    }

    public byte[] getData() {
        return data;
    }

    public int getRealRepDegree() {
        return realRepDegree;
    }

    public void setRealRepDegree(int realRepDegree) {
        this.realRepDegree = realRepDegree;
    }
    
    public int getRepDegree() {
        return repDegree;
    }

    public void setRepDegree(int repDegree) {
        this.repDegree = repDegree;
    }

    @Override
    public int compareTo(Object c2) {
        return this.getRealRepDegree() - ((Chunk) c2).getRealRepDegree();
    }

}
