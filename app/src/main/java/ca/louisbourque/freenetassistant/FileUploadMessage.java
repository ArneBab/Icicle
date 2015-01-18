package ca.louisbourque.freenetassistant;

public class FileUploadMessage {
	
	private String filemanagerstring;
	private String mimeType;
	private String key;
	public String getFilemanagerstring() {
		return filemanagerstring;
	}
	public void setFilemanagerstring(String filemanagerstring) {
		this.filemanagerstring = filemanagerstring;
	}
	public String getMimeType() {
		return mimeType;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	
}