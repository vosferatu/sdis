package files;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;
import java.security.MessageDigest;


public class FileSystem {
	   
	/*
	* https://www.baeldung.com/sha-256-hashing-java
	*/
	private String id;
	private ArrayList<Chunk> chunks;
	private File descriptor;
	private int repDegree;
	
	public FileSystem(String path, int repDegree) {
		this.repDegree = repDegree;
		this.chunks = new ArrayList<>();
		this.descriptor = new File(path);
		fileSplit();
		serialize();
	}
	
	public File getDescriptor() {
		return this.descriptor;
	}
	
	public String getId() {
		return this.id;
	}
	
	public ArrayList<Chunk> getChunks() {
		 return this.chunks;
	}
	
	public int getRepDegree() {
		return this.repDegree;
	}
	
	private void serialize() {
		String name = this.descriptor.getName();
		String date = String.valueOf(this.descriptor.lastModified());
		String creator = this.descriptor.getParent();
	
		String fileId = name + '-' + date + '-' + creator;
	
		this.id = sha256(fileId);
	}
	
	private void fileSplit() {
		int chunkNo = 0, chunkSize = 64000;
		byte[] buf = new byte[chunkSize];
	
		try (FileInputStream fs = new FileInputStream(this.descriptor);
				BufferedInputStream bs = new BufferedInputStream(fs)) {
	
		        int bytes;
		        while ((bytes = bs.read(buf)) > 0) {
	                	chunkNo++;
		                byte[] body = Arrays.copyOf(buf, bytes);
		                Chunk tmp = new Chunk(chunkNo, body, bytes);
		                this.chunks.add(tmp);
		                buf = new byte[chunkSize];
		       }
	
		       if (this.descriptor.length() % 64000 == 0) {
		           Chunk tmp = new Chunk(chunkNo, null, 0);
		           this.chunks.add(tmp);
		       }
		       
		} catch (IOException exception) {
		        exception.printStackTrace();
		}
		
	}
	
	private static String sha256(String base) {
		try {
			MessageDigest cryp = MessageDigest.getInstance("SHA-256");
		    byte[] hash = cryp.digest(base.getBytes("UTF-8"));
		    StringBuffer sb = new StringBuffer();
	
		    for (byte onebyone : hash) {
		         String hexa = Integer.toHexString(0xff & onebyone);
		          if (hexa.length() == 1)
		        	  sb.append('0');
		          sb.append(hexa);
		    }
	
		    return sb.toString();
		 } catch (Exception e) {
		         throw new RuntimeException(e);
		 }
	}
	
}
