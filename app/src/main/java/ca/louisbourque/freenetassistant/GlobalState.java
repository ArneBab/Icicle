package ca.louisbourque.freenetassistant;
 

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import net.pterodactylus.fcp.DataFound;
import net.pterodactylus.fcp.FcpMessage;
import net.pterodactylus.fcp.FinishedCompression;
import net.pterodactylus.fcp.GetFailed;
import net.pterodactylus.fcp.IdentifierCollision;
import net.pterodactylus.fcp.NodeData;
import net.pterodactylus.fcp.NodeHello;
import net.pterodactylus.fcp.Peer;
import net.pterodactylus.fcp.PersistentGet;
import net.pterodactylus.fcp.PersistentPut;
import net.pterodactylus.fcp.PersistentPutDir;
import net.pterodactylus.fcp.PersistentRequestModified;
import net.pterodactylus.fcp.PersistentRequestRemoved;
import net.pterodactylus.fcp.PutFailed;
import net.pterodactylus.fcp.PutFetchable;
import net.pterodactylus.fcp.PutSuccessful;
import net.pterodactylus.fcp.SSKKeypair;
import net.pterodactylus.fcp.SimpleProgress;
import net.pterodactylus.fcp.StartedCompression;
import net.pterodactylus.fcp.URIGenerated;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.widget.Toast;
 

public class GlobalState extends Application{
	
	public static interface StateListener {
        public void onStateChanged(Bundle data);
	}

	private StateListener statusListener;
	private StateListener downloadListener;
	private StateListener uploadListener;
	private StateListener peersListener;
	
	private CopyOnWriteArrayList<LocalNode> localNodes;
	private int activeLocalNode;
	private String deviceID;
	private int refresh_rate;
	private boolean wifiOnly;
	
	private boolean isConnected = false;
	private boolean isMainActivityVisible = false;
	private BlockingQueue<Message> queue;
	private final Handler mFreenetHandler = new Handler();
	final Runnable updateStatus = new Runnable() {
		public void run() {
			Intent intent = new Intent(Constants.BROADCAST_UPDATE_STATUS);
			sendBroadcast(intent);
		}
	};

	final Runnable updateDownloads = new Runnable() {
		public void run() {
			Intent intent = new Intent(Constants.BROADCAST_UPDATE_DOWNLOADS);
			sendBroadcast(intent);
		}
	};
	
	final Runnable updateUploads = new Runnable() {
		public void run() {
			Intent intent = new Intent(Constants.BROADCAST_UPDATE_UPLOADS);
			sendBroadcast(intent);
		}
	};

	final Runnable updatePeers = new Runnable() {
		public void run() {
			Intent intent = new Intent(Constants.BROADCAST_UPDATE_PEERS);
			sendBroadcast(intent);
		}
	};
	private Debouncer debounceBroadcasts = new Debouncer(mFreenetHandler, Constants.debounceInterval);
	private NodeStatus nodeStatus;
	private NodeData nodeData;
	private CopyOnWriteArrayList<Peer> peers;
	private CopyOnWriteArrayList<Download> DownloadsList;
	private CopyOnWriteArrayList<Upload> UploadsList;
	private CopyOnWriteArrayList<UploadDir> UploadDirsList;
	SharedPreferences sharedPref;
	private Intent serviceIntent;
	private SSKKeypair anSSKeypair;
    private Activity activeActivity;
	@SuppressLint("HandlerLeak")
	private final Handler toastHandler = new Handler() {
		public void handleMessage(Message msg) {
			 Toast.makeText(getApplicationContext(),getResources().getString(msg.arg1), msg.arg2).show();
		}
	};
	
	public void onCreate() {
        super.onCreate();
        initializeState();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        loadPreferences();
        
        
	}
	
	private void initializeState(){
        serviceIntent = new Intent(this, FCPService.class);
		this.peers = new CopyOnWriteArrayList<Peer>();
		this.DownloadsList = new CopyOnWriteArrayList<Download>();
		this.UploadsList = new CopyOnWriteArrayList<Upload>();
		this.UploadDirsList = new CopyOnWriteArrayList<UploadDir>();
		this.nodeStatus = null;
		this.setConnected(false);
	}
	
	public void savePreferences() {
		Editor editor = sharedPref.edit();
		String encoded = null;
		  
		  try {
		   ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		   ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		   objectOutputStream.writeObject(this.localNodes);
		   objectOutputStream.close();
		   encoded = new String(Base64.encode(byteArrayOutputStream.toByteArray(), Base64.DEFAULT));
		  } catch (IOException e) {
		   e.printStackTrace();
		   return;
		  }
		
		  editor.putString(Constants.PREF_LOCAL_NODES, encoded.toString());
		  editor.putInt(Constants.PREF_ACTIVE_LOCAL_NODE, this.activeLocalNode);
		  editor.putInt(Constants.PREF_REFRESH_RATE, this.refresh_rate);
		  editor.putBoolean(Constants.PREF_WIFI_ONLY, this.wifiOnly);
		  editor.commit();
	}
	
	@SuppressWarnings("unchecked")
	public void loadPreferences() {
		String strLocalNodes = sharedPref.getString(Constants.PREF_LOCAL_NODES, "");
		if(strLocalNodes.equals("")){
			this.localNodes = new CopyOnWriteArrayList<LocalNode>();
		}else{
			byte[] bytes = Base64.decode(strLocalNodes.getBytes(),Base64.DEFAULT);
			  try {
			   ObjectInputStream objectInputStream = new ObjectInputStream( new ByteArrayInputStream(bytes) );
			   this.localNodes = (CopyOnWriteArrayList<LocalNode>)objectInputStream.readObject();
			  } catch (IOException e) {
				  e.printStackTrace();
				  this.localNodes = new CopyOnWriteArrayList<LocalNode>();
			  } catch (ClassNotFoundException e) {
				  e.printStackTrace();
				  this.localNodes = new CopyOnWriteArrayList<LocalNode>();
			  } catch (ClassCastException e) {
				  e.printStackTrace();
				  this.localNodes = new CopyOnWriteArrayList<LocalNode>();
			  }
		}
		this.activeLocalNode = sharedPref.getInt(Constants.PREF_ACTIVE_LOCAL_NODE, 0);
		this.refresh_rate = sharedPref.getInt(Constants.PREF_REFRESH_RATE, 0);
		this.deviceID = sharedPref.getString(Constants.PREF_DEVICE_ID,"");
		this.wifiOnly = sharedPref.getBoolean(Constants.PREF_WIFI_ONLY, false);
		if(this.deviceID.equals("")){
			Editor editor = sharedPref.edit();
			Random random = new Random();
			this.deviceID = new BigInteger(130, random).toString(32);
			editor.putString(Constants.PREF_DEVICE_ID,this.deviceID);
			editor.commit();
		}
	}
	
	public void setStatusStateListener(StateListener listener) {
		this.statusListener = listener;
	}
	public void setDownloadStateListener(StateListener listener) {
		this.downloadListener = listener;
	}
	public void setUploadStateListener(StateListener listener) {
		this.uploadListener = listener;
	}
	public void setPeersStateListener(StateListener listener) {
		this.peersListener = listener;
	}

	public String getDeviceID() {
		return deviceID;
	}

	public NodeStatus getNodeStatus() {
		return nodeStatus;
	}

	public void setNodeHello(NodeHello nodeHello) {
		this.nodeStatus = new NodeStatus(false,nodeHello.getVersion());
	}
	
	public void setNodeData(NodeData newNodeData){
		this.nodeStatus.setAdvanced(true);
		this.nodeStatus.setRecentInputRate(Double.parseDouble(newNodeData.getVolatile("recentInputRate"))/1000);
		this.nodeStatus.setRecentOutputRate(Double.parseDouble(newNodeData.getVolatile("recentOutputRate"))/1000);
		this.nodeStatus.setUptimeSeconds(Double.parseDouble(newNodeData.getVolatile("uptimeSeconds")));
		this.nodeData = newNodeData;
        extractNodeReference(newNodeData, this.getActiveLocalNode());
		sendRedrawStatus();
        savePreferences();
	}


	public void addToPeerList(Peer peer) {
		int existingPeer = getPeerIndex(peer.getIdentity());
		if(existingPeer < 0){
			this.peers.add(peer);
		}else{
			this.peers.set(existingPeer, peer);
		}
		sendRedrawPeersList();
	}

	
	public void addToDownloadsList(PersistentGet get){
		Download existingDownload = getDownload(get.getIdentifier());
		if(existingDownload == null){
			DownloadsList.add(new Download(get));
		}else{
			existingDownload.setPersistentGet(get);
		}
		sendRedrawDownloads();
	}

	public void addToUploadsList(PersistentPut put){
		Upload existingUpload = getUpload(put.getIdentifier());
		if(existingUpload == null){
			UploadsList.add(new Upload(put));
		}else{
			existingUpload.setPersistentPut(put);
		}
		sendRedrawUploads();
	}

	public void addToUploadsList(PersistentPutDir persistentPutDir) {
		UploadDir existingUpload = getUploadDir(persistentPutDir.getIdentifier());
		if(existingUpload == null){
			UploadDirsList.add(new UploadDir(persistentPutDir));
		}else{
			existingUpload.setPersistentPutDir(persistentPutDir);
		}
		sendRedrawUploads();
	}
	
	public void sendRedrawStatus(){
		debounceBroadcasts.call(this.updateStatus);
	}
	
	public void sendRedrawDownloads(){
		debounceBroadcasts.call(this.updateDownloads);
	}
	
	public void sendRedrawUploads(){
		debounceBroadcasts.call(this.updateUploads);
	}
	
	public void sendRedrawPeersList(){
		debounceBroadcasts.call(this.updatePeers);
	}
	
	public void sendRedrawAll(){
		sendRedrawStatus();
		sendRedrawDownloads();
		sendRedrawUploads();
		sendRedrawPeersList();
	}
	
	public void redrawStatus(){
		Bundle data = new Bundle();
		data.putSerializable(Constants.STATUS, 	this.nodeStatus);
		data.putBoolean(Constants.IS_CONNECTED, this.isConnected);
		if (statusListener != null) {
			statusListener.onStateChanged(data);
        }
	}
	
	public void redrawDownloads() {
		Bundle data = new Bundle();
		data.putSerializable(Constants.DOWNLOADS, this.DownloadsList);
		data.putBoolean(Constants.IS_CONNECTED, this.isConnected);
		if (downloadListener != null) {
			downloadListener.onStateChanged(data);
        }
	}
	
	public void redrawUploads() {
		Bundle data = new Bundle();
		data.putSerializable(Constants.UPLOADS, this.UploadsList);
		data.putSerializable(Constants.UPLOAD_DIRS, this.UploadDirsList);
		data.putBoolean(Constants.IS_CONNECTED, this.isConnected);
		if (uploadListener != null) {
			uploadListener.onStateChanged(data);
        }
	}
	
	public void redrawPeerList(){
		Bundle data = new Bundle();
		data.putSerializable(Constants.PEERS, this.peers);
		data.putBoolean(Constants.IS_CONNECTED, this.isConnected);
		if (peersListener != null) {
			peersListener.onStateChanged(data);
        }
	}
	
	
	
	public CopyOnWriteArrayList<Peer> getPeers() {
		return peers;
	}
	
	public CopyOnWriteArrayList<Download> getDownloadList() {
		return this.DownloadsList;
	}
	
	public CopyOnWriteArrayList<Upload> getUploadList() {
		return this.UploadsList;
	}
	
	public CopyOnWriteArrayList<UploadDir> getUploadDirList() {
		return this.UploadDirsList;
	}
	
	public Peer getPeer(String identifier){
		for(int i=0;i<this.peers.size();i++){
	        if(this.peers.get(i).getIdentity().equals(identifier)){
	            return this.peers.get(i);
		        }
		}
		return null;
	}
	
	public Download getDownload(String identifier){
		for(int i=0;i<this.DownloadsList.size();i++){
	        if(this.DownloadsList.get(i).getPersistentGet().getIdentifier().equals(identifier)){
	            return this.DownloadsList.get(i);
		        }
		}
		return null;
	}
	
	public Upload getUpload(String identifier){
		for(int i=0;i<this.UploadsList.size();i++){
	        if(this.UploadsList.get(i).getPersistentPut().getIdentifier().equals(identifier)){
	            return this.UploadsList.get(i);
		        }
		}
		return null;
	}
	
	public UploadDir getUploadDir(String identifier){
		for(int i=0;i<this.UploadDirsList.size();i++){
	        if(this.UploadDirsList.get(i).getPersistentPutDir().getIdentifier().equals(identifier)){
	            return this.UploadDirsList.get(i);
		        }
		}
		return null;
	}

	public int getPeerIndex(String identifier){
		for(int i=0;i<this.peers.size();i++){
	        if(this.peers.get(i).getIdentity().equals(identifier)){
	            return i;
		        }
		}
		return -1;
	}
	
	public int getDownloadIndex(String identifier){
		for(int i=0;i<this.DownloadsList.size();i++){
	        if(this.DownloadsList.get(i).getPersistentGet().getIdentifier().equals(identifier)){
	            return i;
		        }
		}
		return -1;
	}
	
	public int getUploadIndex(String identifier){
		for(int i=0;i<this.UploadsList.size();i++){
	        if(this.UploadsList.get(i).getPersistentPut().getIdentifier().equals(identifier)){
	            return i;
		        }
		}
		return -1;
	}
	
	public int getUploadDirIndex(String identifier){
		for(int i=0;i<this.UploadDirsList.size();i++){
	        if(this.UploadDirsList.get(i).getPersistentPutDir().getIdentifier().equals(identifier)){
	            return i;
		        }
		}
		return -1;
	}
	
	public boolean isConnected() {
		return isConnected;
	}

	public void setConnected(boolean isConnected) {
		this.isConnected = isConnected;
		sendRedrawStatus();
	}

	public boolean isMainActivityVisible() {
		return isMainActivityVisible;
	}

	public void setMainActivityVisible(boolean isMainActivityVisible) {
		this.isMainActivityVisible = isMainActivityVisible;
	}

	public void updateTransferProgress(SimpleProgress simpleProgress) {
		Download existingDownload = getDownload(simpleProgress.getIdentifier());
		if(existingDownload == null){
			Upload existingUpload = getUpload(simpleProgress.getIdentifier());
			
			
			if(existingUpload == null){
				UploadDir existingUploadDir = getUploadDir(simpleProgress.getIdentifier());
				
				if(existingUploadDir == null){
					return;
				}
				existingUploadDir.updateProgress(simpleProgress);
				sendRedrawUploads();
			}else{
				existingUpload.updateProgress(simpleProgress);
				sendRedrawUploads();
			}
		}else{
			existingDownload.updateProgress(simpleProgress);
			sendRedrawDownloads();
		}
	}

	public void addDataLength(FcpMessage fcpMessage) {
		Download existingDownload = getDownload(fcpMessage.getField("Identifier"));
		if(existingDownload == null){
			Upload existingUpload = getUpload(fcpMessage.getField("Identifier"));
			
			if(existingUpload == null){
				UploadDir existingUploadDir = getUploadDir(fcpMessage.getField("Identifier"));
				if(existingUploadDir == null){
					return;
				}
				existingUploadDir.updateDataLength(fcpMessage.getField("DataLength"));
				sendRedrawUploads();
			}else{
				existingUpload.updateDataLength(fcpMessage.getField("DataLength"));
				sendRedrawUploads();
			}
		}else{
			existingDownload.updateDataLength(fcpMessage.getField("DataLength"));
			sendRedrawDownloads();
		}
		
	}

	public void updateDataFound(DataFound dataFound) {
		Download existingDownload = getDownload(dataFound.getIdentifier());
		if(existingDownload != null){
			existingDownload.setDataFound(dataFound);
		}
	}

	public void removePersistentRequest(PersistentRequestRemoved persistentRequestRemoved) {
		int existingDownloadIndex = getDownloadIndex(persistentRequestRemoved.getIdentifier());
		if(existingDownloadIndex < 0){
			int existingUploadIndex = getUploadIndex(persistentRequestRemoved.getIdentifier());
			
			
			if(existingUploadIndex< 0){
				int existingUploadDirIndex = getUploadDirIndex(persistentRequestRemoved.getIdentifier());
				
				if(existingUploadDirIndex < 0){
					return;
				}
				UploadDirsList.remove(existingUploadDirIndex);
				sendRedrawUploads();
			}else{
				UploadsList.remove(existingUploadIndex);
				sendRedrawUploads();
			}
		}else{
			DownloadsList.remove(existingDownloadIndex);
			sendRedrawDownloads();
		}
	}

	public void addPutSuccessful(PutSuccessful putSuccessful) {
		Upload existingUpload = getUpload(putSuccessful.getIdentifier());
		
		if(existingUpload == null){
			UploadDir existingUploadDir = getUploadDir(putSuccessful.getIdentifier());
			if(existingUploadDir == null){
				return;
			}
			existingUploadDir.setPutSuccessful(putSuccessful);
		}else{
			existingUpload.setPutSuccessful(putSuccessful);
		}
		sendRedrawUploads();
	}
	

	public void addPutFetchable(PutFetchable putFetchable) {
		Upload existingUpload = getUpload(putFetchable.getIdentifier());
		
		if(existingUpload == null){
			UploadDir existingUploadDir = getUploadDir(putFetchable.getIdentifier());
			if(existingUploadDir == null){
				return;
			}
			existingUploadDir.setPutFetchable(putFetchable);

		}else{
			existingUpload.setPutFetchable(putFetchable);
		}
		sendRedrawUploads();
	}

	public void addURIGenerated(URIGenerated uriGenerated) {
		Upload existingUpload = getUpload(uriGenerated.getIdentifier());
		
		if(existingUpload == null){
			UploadDir existingUploadDir = getUploadDir(uriGenerated.getIdentifier());
			if(existingUploadDir == null){
				return;
			}
			existingUploadDir.setUriGenerated(uriGenerated);

		}else{
			existingUpload.setUriGenerated(uriGenerated);
		}
	}

	public void addStartedCompression(StartedCompression startedCompression) {
		Upload existingUpload = getUpload(startedCompression.getIdentifier());
		
		if(existingUpload == null){
			UploadDir existingUploadDir = getUploadDir(startedCompression.getIdentifier());
			if(existingUploadDir == null){
				return;
			}
			existingUploadDir.setStartedCompression(startedCompression);

		}else{
			existingUpload.setStartedCompression(startedCompression);
		}
	}

	public void addFinishedCompression(FinishedCompression finishedCompression) {
		Upload existingUpload = getUpload(finishedCompression.getIdentifier());
		
		if(existingUpload == null){
			UploadDir existingUploadDir = getUploadDir(finishedCompression.getIdentifier());
			if(existingUploadDir == null){
				return;
			}
			existingUploadDir.setFinishedCompression(finishedCompression);

		}else{
			existingUpload.setFinishedCompression(finishedCompression);
		}
	}

	public void addGetFailed(GetFailed getFailed) {
		Download existingDownload = getDownload(getFailed.getIdentifier());
		if(existingDownload != null){
			existingDownload.setGetFailed(getFailed);
		}
		sendRedrawDownloads();
	}

	public void addPutFailed(PutFailed putFailed) {
		Upload existingUpload = getUpload(putFailed.getIdentifier());
		
		if(existingUpload == null){
			UploadDir existingUploadDir = getUploadDir(putFailed.getIdentifier());
			if(existingUploadDir == null){
				return;
			}
			existingUploadDir.setPutFailed(putFailed);

		}else{
			existingUpload.setPutFailed(putFailed);
		}
		sendRedrawUploads();
	}

	public void updatePeristentRequest(PersistentRequestModified persistentRequestModified) {
	
		Download existingDownload = getDownload(persistentRequestModified.getIdentifier());
		if(existingDownload == null){
			Upload existingUpload = getUpload(persistentRequestModified.getIdentifier());
			
			
			if(existingUpload == null){
				UploadDir existingUploadDir = getUploadDir(persistentRequestModified.getIdentifier());
				
				if(existingUploadDir == null){
					return;
				}
				if(existingUploadDir.getPriority() == persistentRequestModified.getPriority().ordinal()){
					return;
				}
				existingUploadDir.setPriority(persistentRequestModified.getPriority().ordinal());
				sendRedrawUploads();
				
			}else{
				if(existingUpload.getPriority() == persistentRequestModified.getPriority().ordinal()){
					return;
				}
				existingUpload.setPriority(persistentRequestModified.getPriority().ordinal());
				sendRedrawUploads();
			}
		}else{
			if(existingDownload.getPriority() == persistentRequestModified.getPriority().ordinal()){
				return;
			}
			existingDownload.setPriority(persistentRequestModified.getPriority().ordinal());
			sendRedrawDownloads();
		}
	}

	public void setQueue(BlockingQueue<Message> queue) {
		this.queue = queue;
	}

	public BlockingQueue<Message> getQueue() {
		return this.queue;
	}

	public LocalNode getActiveLocalNode() {
		return localNodes.get(this.activeLocalNode);
	}
	
	public int getActiveLocalNodeIndex(){
		return this.activeLocalNode;
	}
	
	public void setActiveLocalNodeIndex(int newIndex){
		this.activeLocalNode = newIndex;
		onActiveNodeChanged();
	}
	
	public CopyOnWriteArrayList<LocalNode> getLocalNodeList(){
		return this.localNodes;
	}

	public void onActiveNodeChanged() {
		Editor editor = sharedPref.edit();
		editor.putInt(Constants.PREF_ACTIVE_LOCAL_NODE, this.activeLocalNode);
		editor.commit();
		//Toast.makeText(this, R.string.node_change_active, Toast.LENGTH_SHORT).show();
		showToast(R.string.node_change_active);
		restartFCPService(true);
	}
	
	public void showToast(int stringMessage){
		Message msg = toastHandler.obtainMessage();
		msg.arg1 = stringMessage;
		msg.arg2 = Toast.LENGTH_SHORT;
		toastHandler.sendMessage(msg);
	}
	
	public void restartFCPService(boolean force){
		stopFCPService();
		initializeState();
		startFCPService();
	}
	
public void onRefreshRateChange(int integer, boolean need_to_reset_loop) {
		Editor editor = sharedPref.edit();
		editor.putInt(Constants.PREF_REFRESH_RATE, integer);
		editor.commit();
		if(need_to_reset_loop){
			restartFCPService(false);
		}
	}

	public int getRefresh_rate() {
		return refresh_rate;
	}



	public void setRefresh_rate(int refresh_rate) {
		//if the new refresh rate is shorter than the old one, restart the service. Otherwise may need to wait a while for new refresh rate to take effect.
		boolean need_to_reset_loop = refresh_rate < this.refresh_rate || this.refresh_rate == 0;
		this.refresh_rate = refresh_rate;
		onRefreshRateChange(refresh_rate,need_to_reset_loop);
	}



	public boolean isWifiOnly() {
		return wifiOnly;
	}

	public void setWifiOnly(boolean wifiOnly) {
		this.wifiOnly = wifiOnly;
		Editor editor = sharedPref.edit();
		editor.putBoolean(Constants.PREF_WIFI_ONLY, wifiOnly);
		editor.commit();
	}

	public void startFCPService() {
		System.out.println(">>>GlobalState.startFCPService()");
		startService(serviceIntent);
	}
	
	public void stopFCPService() {
		System.out.println(">>>GlobalState.stopFCPService()");
		if(serviceIntent == null){
			return;
		}
	    stopService(serviceIntent);
	}

    public boolean serviceShouldStop() {
        return this.activeActivity == null;
    }

    public void registerActivity(Activity act){
        System.out.println(">>>GlobalState.registerActivity("+act.toString()+")");
        this.activeActivity = act;
        startFCPService();
    }

    public void unregisterActivity(Activity act){
        System.out.println(">>>GlobalState.unregisterActivity("+act.toString()+")");
        if(this.activeActivity == act)
        this.activeActivity = null;
    }

	public SSKKeypair getSSKKeypair() {
		try {
			this.setSSKeypair(null);
			this.queue.put(Message.obtain(null, 0, Constants.MsgGetSSKeypair, 0));
			int limit = 15;
			while(this.anSSKeypair == null && limit > 0){
				limit--;
				Thread.sleep(1000);
			}
			return anSSKeypair;
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void setSSKeypair(SSKKeypair sskKeypair) {
		this.anSSKeypair = sskKeypair;
		
	}

	public void handleIdentifierCollision(IdentifierCollision identifierCollision) {

		//check if the Identifier Collision was caused by the user trying to upload the same file
		for(Upload u:UploadsList){
			if(u.getPersistentPut().getIdentifier().equals(identifierCollision.getIdentifier())){
				Message msg = toastHandler.obtainMessage();
				msg.arg1 = R.string.identifierCollision_upload;
				msg.arg2 = Toast.LENGTH_LONG;
				toastHandler.sendMessage(msg);
			}		
		}

		//Identifier Collision was not caused by an upload
	}

	public NodeData getNodeData() {
		return this.nodeData;
	}

    private void extractNodeReference(NodeData myNode, LocalNode activeLocalNode) {
        String refStr = "";
        String temp = "";
        String EncodedStr = "";
        refStr+="identity="+myNode.getIdentity()+"\n";
        EncodedStr+="identity="+myNode.getIdentity()+"\n";
        refStr+="lastGoodVersion="+myNode.getLastGoodVersion()+"\n";
        temp = new String(Base64.encode(myNode.getLastGoodVersion().toString().getBytes(), Base64.NO_PADDING|Base64.NO_WRAP));
        EncodedStr+="lastGoodVersion=="+temp+"\n";
        refStr+="location="+myNode.getNodeRef().getLocation()+"\n";
        temp = new String(Base64.encode(String.valueOf(myNode.getNodeRef().getLocation()).getBytes(), Base64.NO_PADDING|Base64.NO_WRAP));
        EncodedStr+="location=="+temp+"\n";
        refStr+="myName="+myNode.getMyName()+"\n";
        temp = new String(Base64.encode(myNode.getMyName().getBytes(), Base64.NO_PADDING|Base64.NO_WRAP));
        EncodedStr+="myName=="+temp+"\n";
        refStr+="opennet="+myNode.isOpennet()+"\n";
        EncodedStr+="opennet="+myNode.isOpennet()+"\n";
        refStr+="sig="+myNode.getSignature()+"\n";
        EncodedStr+="sig="+myNode.getSignature()+"\n";
        refStr+="sigP256="+myNode.getField("sigP256")+"\n";
        EncodedStr+="sigP256="+myNode.getField("sigP256")+"\n";
        refStr+="version="+myNode.getVersion()+"\n";
        temp = new String(Base64.encode(myNode.getVersion().toString().getBytes(), Base64.NO_PADDING|Base64.NO_WRAP));
        EncodedStr+="version=="+temp+"\n";
        refStr+="ark.number="+myNode.getARK().getNumber()+"\n";
        EncodedStr+="ark.number="+myNode.getARK().getNumber()+"\n";
        refStr+="ark.pubURI="+myNode.getARK().getPublicURI()+"\n";
        EncodedStr+="ark.pubURI="+myNode.getARK().getPublicURI()+"\n";
        refStr+="auth.negTypes="+myNode.getField("auth.negTypes")+"\n";
        temp = new String(Base64.encode(myNode.getField("auth.negTypes").toString().getBytes(), Base64.NO_PADDING|Base64.NO_WRAP));
        EncodedStr+="auth.negTypes=="+temp+"\n";
        refStr+="dsaGroup.g="+myNode.getDSAGroup().getBase()+"\n";
        EncodedStr+="dsaGroup.g="+myNode.getDSAGroup().getBase()+"\n";
        refStr+="dsaGroup.p="+myNode.getDSAGroup().getPrime()+"\n";
        EncodedStr+="dsaGroup.p="+myNode.getDSAGroup().getPrime()+"\n";
        refStr+="dsaGroup.q="+myNode.getDSAGroup().getSubprime()+"\n";
        EncodedStr+="dsaGroup.q="+myNode.getDSAGroup().getSubprime()+"\n";
        refStr+="dsaPubKey.y="+myNode.getDSAPublicKey()+"\n";
        EncodedStr+="dsaPubKey.y="+myNode.getDSAPublicKey()+"\n";
        refStr+="ecdsa.P256.pub="+myNode.getField("ecdsa.P256.pub")+"\n";
        EncodedStr+="ecdsa.P256.pub="+myNode.getField("ecdsa.P256.pub")+"\n";
        refStr+="physical.udp="+myNode.getPhysicalUDP()+"\n";
        temp = new String(Base64.encode(myNode.getPhysicalUDP().toString().getBytes(), Base64.NO_PADDING|Base64.NO_WRAP));
        EncodedStr+="physical.udp=="+temp+"\n";
        refStr+="End\n";
        EncodedStr+="End\n";
        activeLocalNode.setNodeReference(refStr);
        activeLocalNode.setEncodedNodeReference(EncodedStr);
    }
}