package ca.louisbourque.freenetassistant;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.Message;

public class FCPService extends Service {
	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	private FreenetUtil freenet;
	private RefreshThread refreshThread;
	public BlockingQueue<Message> queue;
	public GlobalState gs;
	protected ConnectivityManager cm;
	
	private class RefreshThread extends Thread{
		
		public void run(){
			while (true) {
				synchronized (this) {
					try {
						if(gs.getRefresh_rate() == 0){
							wait(10000);
							continue;
						}
						//check status of connection, reconnect if able
						if(freenet.isAlive()){
							//if we are connected, but the network type changed and we are wifi only, disconnect
							if(gs.isWifiOnly() && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().getType() != ConnectivityManager.TYPE_WIFI){
								freenet.tearDown();
							}
						}else{
							if(!gs.isWifiOnly() || (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI)){
								switch(freenet.getState()) {
									case TERMINATED:
										freenet = new FreenetUtil(getApplicationContext(),queue, gs);
									default: //NEW
										freenet.start();
										updateStatus();
										updatePeers();
								}
								
							}else{
								//wait for the network to become available
								wait(5000);
								continue;
							}
						}
						wait(gs.getRefresh_rate()*1000);
						if(gs.isMainActivityVisible()){
							updateStatus();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
	}
	
	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		@Override
		public void handleMessage(Message msg) {
			if(!refreshThread.isAlive()){
				refreshThread.start();
				try {
					refreshThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}
	}

	@Override
	public void onCreate() {
		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread("ServiceStartArguments",
				Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

		queue = new ArrayBlockingQueue<Message>(1024);
		this.gs = (GlobalState) getApplication();
		this.gs.setQueue(queue);
		
		cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		freenet = new FreenetUtil(this,queue, this.gs);
		refreshThread = new RefreshThread();
		//only connect if we allow connection on non-wifi or we are on wifi
		if(!this.gs.isWifiOnly() || (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI)){
			freenet.start();
			updateStatus();
			updatePeers();
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public void onDestroy(){
		freenet.tearDown();
		refreshThread.interrupt();
		int timeout = 5;
		while(timeout > 0 && this.gs.isConnected()){
			synchronized (this) {
				try {
					Thread.sleep(1000);
					timeout--;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		super.onDestroy();
	}

	public void updateStatus(){
		try {
			queue.put(Message.obtain(null, 0, Constants.MsgGetNode, 0));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updatePersistentRequests(){
		try {
			queue.put(Message.obtain(null, 0, Constants.MsgGetPersistentRequests, 0));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updatePeers(){
		try {
			queue.put(Message.obtain(null, 0, Constants.MsgGetPeers, 0));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}