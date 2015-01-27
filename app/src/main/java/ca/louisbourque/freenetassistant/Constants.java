package ca.louisbourque.freenetassistant;

import android.annotation.SuppressLint;

public class Constants {
	
	public static final String ACTION 	= "ca.louisbourque.timagebrowser.action";
	public static final int	MsgEXIT		= 1000;
	public static final int MsgGetNode = 1001;
	public static final int MsgGetSSKeypair = 1002;
	public static final int MsgGetPersistentRequests = 1003;
	public static final int MsgGetPeers = 1004;
	public static final int MsgFileUpload = 1005;
	public static final int MsgAddNoderef = 1006;
    public static final int MsgUpdatePriority = 1007;
	protected static final Object FNconnected = "CONNECTED";
	protected static final Object FNbackedoff = "BACKED OFF";
	public static final int Activity_File_Upload = 20;
	public static final int Activity_Settings = 21;
	public static final int Activity_Reference = 22;
	public static final int PagerPositionUploads = 2;
	public static final int PagerPositionPeers = 3;
	protected static final String BROADCAST_UPDATE_STATUS = "ca.louisbourque.freenetassistant.update_status";
	protected static final String BROADCAST_UPDATE_DOWNLOADS = "ca.louisbourque.freenetassistant.update_downloads";
	protected static final String BROADCAST_UPDATE_UPLOADS = "ca.louisbourque.freenetassistant.update_uploads";
	protected static final String BROADCAST_UPDATE_PEERS = "ca.louisbourque.freenetassistant.update_peers";
    public static final String LOCAL_NODE_SELECTED = "ca.louisbourque.freenetassistant.local_node_selected";
	public static final String IS_CONNECTED = "isConnected";
	public static final String STATUS = "status";
	public static final String DOWNLOADS = "downloads";
	public static final String UPLOADS = "uploads";
	public static final String PEERS = "peers";
	public static final String UPLOAD_DIRS = "uploadDirs";
	public static final String PREF_LOCAL_NODES = "localNodes";
	public static final String PREF_ACTIVE_LOCAL_NODE = "activeLocalNode";
	public static final String PREF_UPLOAD_KEY = "uploadKey";
	public static final String PREF_REFRESH_RATE = "refresh_rate";
	public static final String PREF_WIFI_ONLY = "wifiOnly";
	protected static final int DEFAULT_FCP_PORT = 9481;
	public static final String PREF_DEVICE_ID = "deviceID";
	public static final String KEY_TYPE_CHK = "CHK@";
	public static final String KEY_TYPE_SSK = "SSK@";
	public static final String KEY_TYPE_DEFAULT = KEY_TYPE_SSK;
	
	public static int numberOfTabs = 4;
	public static int debounceInterval = 500;
	
	//http://stackoverflow.com/a/3758880
	@SuppressLint("DefaultLocale")
	public static String humanReadableByteCount(long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	
}
