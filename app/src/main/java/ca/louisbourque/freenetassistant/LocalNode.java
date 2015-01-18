package ca.louisbourque.freenetassistant;

import java.io.Serializable;

public class LocalNode implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private String address;
	private int port;
	
	public LocalNode(){
		new LocalNode("","",0);
	}
	
	public LocalNode(String name, String address, int port){
		this.name = name;
		this.address = address;
		this.port = port;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	 public String toString()
	    {
	        return "[LocalNode: name=" + name + 
	            " address=" + address +
	            " port=" + port +
	            "]";
	    }    
	
}
