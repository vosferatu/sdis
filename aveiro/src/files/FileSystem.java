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
	private File f;
	private int repDegree;
	
	public FileSystem(String path, int repDegree) {
		this.repDegree = repDegree;
		this.chunks = new ArrayList<>();
		this.f = new File(path);
		fileSplit();
		setId();
	}
	
	public File getFile() {
		return this.f;
	}
	
	public String getId() {
		return this.id;
	}
	
	
	public int getRepDegree() {
		return this.repDegree;
	}
	
	public ArrayList<Chunk> getChunks() {
		 return this.chunks;
	}
	
	private void fileSplit() {
		int chunkNo = 0, chunkSize = 64000;
		byte[] buf = new byte[chunkSize];
	
		try (FileInputStream fs = new FileInputStream(this.f);
				BufferedInputStream bs = new BufferedInputStream(fs)) {
	
		        int bytes;
		        while ((bytes = bs.read(buf)) > 0) {
		                byte[] body = Arrays.copyOf(buf, bytes);
	
		                chunkNo++;
		                Chunk chunk = new Chunk(chunkNo, body, bytes);
		                this.chunks.add(chunk);
		                buf = new byte[chunkSize];
		       }
	
		       if (this.f.length() % 64000 == 0) {
		           Chunk chunk = new Chunk(chunkNo, null, 0);
		           this.chunks.add(chunk);
		       }
		       
		} catch (IOException exception) {
		        exception.printStackTrace();
		}
		
	}
	
	private void setId() {
		String name = this.f.getName();
		String date = String.valueOf(this.f.lastModified());
		String creator = this.f.getParent();
	
		String fileId = name + '-' + date + '-' + creator;
	
		this.id = sha256(fileId);
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
