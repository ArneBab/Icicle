package ca.louisbourque.freenetassistant;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.concurrent.CopyOnWriteArrayList;

import ca.louisbourque.freenetassistant.GlobalState.StateListener;
import net.pterodactylus.fcp.DataFound;
import net.pterodactylus.fcp.GetFailed;
import net.pterodactylus.fcp.Peer;
import net.pterodactylus.fcp.PersistentGet;
import net.pterodactylus.fcp.PersistentPut;
import net.pterodactylus.fcp.PersistentPutDir;
import net.pterodactylus.fcp.PutFailed;
import net.pterodactylus.fcp.SimpleProgress;

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener{
	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
	 * three primary sections of the app. We use a {@link android.support.v4.app.FragmentPagerAdapter}
	 * derivative, which will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	AppSectionsPagerAdapter mAppSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will display the three primary sections of the app, one at a
	 * time.
	 */
	ViewPager mViewPager;
	public GlobalState gs;
	private MainViewBroadcastReceiver mReceiver;
    private SlidingTabLayout mSlidingTabLayout;

    public synchronized void updateStatusView(){
		this.gs.redrawStatus();
	}

	public synchronized void updateDownloadsView(){
		this.gs.redrawDownloads();
	}


	public synchronized void updateUploadsView(){
		this.gs.redrawUploads();
	}


	public synchronized void updatePeersView(){
		this.gs.redrawPeerList();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.gs = (GlobalState) getApplication();

		// Create the adapter that will return a fragment for each of the three primary sections
		// of the app.
		mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager(),this);

		// Set up the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
		// setHasOptionsMenu(true);
        setSupportActionBar(toolbar);




		// Set up the ViewPager, attaching the adapter and setting up a listener for when the
		// user swipes between sections.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mAppSectionsPagerAdapter);

        mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setCustomTabView(R.layout.tab_indicator, android.R.id.text1);

        setSlidingTabLayoutContentDescriptions();

        Resources res = getResources();
        mSlidingTabLayout.setSelectedIndicatorColors(res.getColor(R.color.primary));
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setViewPager(mViewPager);

		mViewPager.setOffscreenPageLimit(Constants.numberOfTabs);
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
		if(isFinishing()){
			this.gs.stopFCPService(false);
		}
	}


	@Override
	protected void onPause() {
		this.gs.setMainActivityVisible(false);
		unregisterReceiver(mReceiver);
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.gs.setMainActivityVisible(true);
		IntentFilter iFilter = new IntentFilter(Constants.BROADCAST_UPDATE_STATUS);
		iFilter.addAction(Constants.BROADCAST_UPDATE_UPLOADS);
		iFilter.addAction(Constants.BROADCAST_UPDATE_DOWNLOADS);
		iFilter.addAction(Constants.BROADCAST_UPDATE_PEERS);
		this.mReceiver = new MainViewBroadcastReceiver(this);
		registerReceiver(this.mReceiver, iFilter);
		this.gs.startFCPService();
		//updateStatusView();
		//updateDownloadsView();
		//updateUploadsView();
		//updatePeersView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
		case R.id.action_settings:
			handleSettings();
			return true;
		case R.id.action_upload:
			handleFileUpload();
			return true;
		case R.id.action_social:
			handleSocial();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

    private void setSlidingTabLayoutContentDescriptions() {
        for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
            mSlidingTabLayout.setContentDescription(i,
                    mAppSectionsPagerAdapter.getPageTitle(i).toString());
        }
    }

	private void handleSettings() {
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivityForResult(intent,Constants.Activity_Settings);
	}

	private void handleFileUpload() {
		Intent intent = new Intent(this, UploadActivity.class);
		startActivityForResult(intent,Constants.Activity_File_Upload);
	}

	private void handleSocial() {
		Intent intent = new Intent(this, ReferenceActivity.class);
		startActivityForResult(intent,Constants.Activity_Reference);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		System.out.println("Handling onActivityResult...");
		if(requestCode == Constants.Activity_File_Upload && resultCode == Activity.RESULT_OK){
            mViewPager.setCurrentItem(Constants.PagerPositionUploads);
		}
		if(requestCode == Constants.Activity_Settings && resultCode == Activity.RESULT_OK){
			this.gs.sendRedrawAll();
		}
		if(requestCode == Constants.Activity_Reference && resultCode == Activity.RESULT_OK){
            mViewPager.setCurrentItem(Constants.PagerPositionPeers);
		}
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
	}

	
	public void changeTransferPriority(View view){
		TextView transferName = (TextView)((View) view.getParent()).findViewById(R.id.transfer_name);
		//TODO: Create a dialog to choose the new priority.
		
	}
	
	/**
	 * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
	 * sections of the app.
	 */
	public static class AppSectionsPagerAdapter extends FragmentStatePagerAdapter {

		private Context context;

		public AppSectionsPagerAdapter(FragmentManager fm, Context context) {
			super(fm);
			this.context = context;
		}

		@Override
		public Fragment getItem(int i) {
			System.out.println("getItem - "+i);
			switch (i) {
			case 0:
				return new StatusSectionFragment();
			case 1:
				return new DownloadsSectionFragment();
			case 2:
				return new UploadsSectionFragment();
			case 3:
				return new PeersSectionFragment();
			default:
				return new StatusSectionFragment();
			}
		}

		@Override
		public Fragment instantiateItem(ViewGroup v, int i){
			System.out.println("instantiateItem - "+i);
			Fragment aFragment = (Fragment) super.instantiateItem(v,i);
			GlobalState gs = ((MainActivity)context).gs;
			switch (i) {
			case 0:
				gs.setStatusStateListener((StateListener) aFragment);
				gs.sendRedrawStatus();
				break;
			case 1:
				gs.setDownloadStateListener((StateListener) aFragment);
				gs.sendRedrawDownloads();
				break;
			case 2:
				gs.setUploadStateListener((StateListener) aFragment);
				gs.sendRedrawUploads();
				break;
			case 3:
				gs.setPeersStateListener((StateListener) aFragment);
				gs.sendRedrawPeersList();
				break;
			default:

			}
			return aFragment;
		}

		public void destroyItem(ViewGroup container, int position, Object object){
			GlobalState gs = ((MainActivity)context).gs;
			switch (position) {
			case 0:
				gs.setStatusStateListener(null);
				break;
			case 1:
				gs.setDownloadStateListener(null);
				break;
			case 2:
				gs.setUploadStateListener(null);
				break;
			case 3:
				gs.setPeersStateListener(null);
				break;
			default:

			}
			super.destroyItem(container, position, object);
		}

		@Override
		public int getCount() {
			return Constants.numberOfTabs;
		}


		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return context.getString(R.string.status);
			case 1:
				return context.getString(R.string.downloads);
			case 2:
				return context.getString(R.string.uploads);
			case 3:
				return context.getString(R.string.peers);
			default:
				return "";
			}
		}
		
		public int getPageIcon(int position) {
			switch (position) {
			case 0:
				return R.drawable.ic_action_about;
			case 1:
				return R.drawable.ic_action_download;
			case 2:
				return R.drawable.ic_file_upload_black_36dp;
			case 3:
				return R.drawable.ic_people_black_36dp;
			default:
				return R.drawable.ic_action_about;
			}
		}
	}







	public static class PeersSectionFragment extends Fragment implements GlobalState.StateListener {

		private View mView;

		public static PeersSectionFragment newInstance() {
			// Seems pointless, I know, but this static method
			// Might later be used to pass in arguments via a Bundle
			return new PeersSectionFragment();
		}

		public PeersSectionFragment() {
			// Default, no argument constructor
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			mView = inflater.inflate(R.layout.fragment_peers, container, false);

			return mView;
		}

		@SuppressWarnings("unchecked")
		@Override
		public synchronized void onStateChanged(Bundle data) {
			System.out.println("onStateChanged: Peers");
			//TODO
			//if(gs.getNodeData() == null)
			//  we don't have full access, display an notice
			LinearLayout peerListView = (LinearLayout)mView.findViewById(R.id.peer_list_view);
			peerListView.removeAllViews();
			if(!data.getBoolean(Constants.IS_CONNECTED)){
                FrameLayout fl = (FrameLayout)getLayoutInflater(null).inflate(R.layout.fragment_no_connectivity, peerListView, false);
				peerListView.addView(fl);
				return;
			}
			for (Peer p : (CopyOnWriteArrayList<Peer>)data.getSerializable(Constants.PEERS)) {
				LinearLayout peerView = (LinearLayout) getLayoutInflater(new Bundle()).inflate(R.layout.peer, peerListView, false);
				TextView peerName = (TextView) peerView.findViewById(R.id.peer_name);
				ImageView peerIcon = (ImageView)peerView.findViewById(R.id.peer_icon);
				if(p.getMyName() != null){
					peerName.setText(p.getMyName());
				}else if(p.isOpennet()){
					peerName.setText(mView.getContext().getString(R.string.unnamed_opennet_peer));
				}else{
					peerName.setText(mView.getContext().getString(R.string.unnamed_peer));
				}
				if(p.getVolatile("status").equals(Constants.FNconnected)){
					peerIcon.setColorFilter(getResources().getColor(R.color.green_500));
				}else if(p.getVolatile("status").equals(Constants.FNbackedoff)){
					peerIcon.setColorFilter(getResources().getColor(R.color.orange_500));
				}else{
					peerIcon.setColorFilter(getResources().getColor(R.color.grey_500));
				}
				((TextView) peerView.findViewById(R.id.peer_address)).setText(p.getPhysicalUDP());
				peerListView.addView(peerView);
			}
		}

	}

	public static class UploadsSectionFragment extends Fragment implements GlobalState.StateListener {

		private View mView;

		public static UploadsSectionFragment newInstance() {
			// Seems pointless, I know, but this static method
			// Might later be used to pass in arguments via a Bundle
			return new UploadsSectionFragment();
		}

		public UploadsSectionFragment() {
			// Default, no argument constructor
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			mView = inflater.inflate(R.layout.fragment_uploads, container, false);

			return mView;
		}

		@SuppressWarnings("unchecked")
		@Override
		public synchronized void onStateChanged(Bundle data) {
			System.out.println("onStateChanged: Uploads");
			LinearLayout uploadListView = (LinearLayout)mView.findViewById(R.id.uploads_list_view);
			uploadListView.removeAllViews();
			if(!data.getBoolean(Constants.IS_CONNECTED)){
                FrameLayout fl = (FrameLayout)getLayoutInflater(null).inflate(R.layout.fragment_no_connectivity, uploadListView, false);
				uploadListView.addView(fl);
				return;
			}
			//Directories
			for (UploadDir u : (CopyOnWriteArrayList<UploadDir>)data.getSerializable(Constants.UPLOAD_DIRS)) {
				PersistentPutDir p = u.getPersistentPutDir();
				LinearLayout transferView = (LinearLayout) getLayoutInflater(new Bundle()).inflate(R.layout.transfer, uploadListView, false);
				TextView transferName = (TextView) transferView.findViewById(R.id.transfer_name);
				ImageButton playButton = (ImageButton) transferView.findViewById(R.id.play_button);
				ImageButton pauseButton = (ImageButton) transferView.findViewById(R.id.pause_button);
				TextView statusLabel = (TextView) transferView.findViewById(R.id.transfer_status_label);
				TextView statusText = (TextView) transferView.findViewById(R.id.transfer_percentage);
				TextView priorityLabel = (TextView) transferView.findViewById(R.id.transfer_priority_label);
				TextView priorityText = (TextView) transferView.findViewById(R.id.transfer_priority);
				ImageView transferDone = (ImageView) transferView.findViewById(R.id.transfer_done);
				transferName.setText(p.getIdentifier());
				transferName.setHint(p.getIdentifier());
				if(u.getPriority() == 6){
					pauseButton.setVisibility(View.GONE);
					playButton.setVisibility(View.VISIBLE);
					priorityLabel.setVisibility(View.GONE);
					priorityText.setVisibility(View.GONE);
					statusLabel.setText(getResources().getText(R.string.status_paused));
				}else{
					pauseButton.setVisibility(View.VISIBLE);
					playButton.setVisibility(View.GONE);
					priorityLabel.setVisibility(View.VISIBLE);
					priorityText.setVisibility(View.VISIBLE);
					switch(u.getPriority()){
						case 0:
							priorityText.setText(getResources().getText(R.string.priority_0));
							break;
						case 1:
							priorityText.setText(getResources().getText(R.string.priority_1));
							break;
						case 2:
							priorityText.setText(getResources().getText(R.string.priority_2));
							break;
						case 3:
							priorityText.setText(getResources().getText(R.string.priority_3));
							break;
						case 4:
							priorityText.setText(getResources().getText(R.string.priority_4));
							break;
						case 5:
							priorityText.setText(getResources().getText(R.string.priority_5));
							break;
						default:
					}
					statusLabel.setText(getResources().getText(R.string.status_uploading));
				}
				long dataLength = u.getDataLength();
				
				if(dataLength != 0){
					((TextView) transferView.findViewById(R.id.transfer_size)).setText(Constants.humanReadableByteCount(dataLength,false));
				}
				ProgressBar bar = (ProgressBar) transferView.findViewById(R.id.transfer_progress);
				SimpleProgress sp = u.getProgress();
				PutFailed pf = u.getPutFailed();
				
				if(u.getPutSuccessful() != null){
        			bar.setMax(1);
        			bar.setProgress(1);
        			bar.setVisibility(View.GONE);
        			pauseButton.setVisibility(View.GONE);
					playButton.setVisibility(View.GONE);
					transferDone.setVisibility(View.VISIBLE);
					statusLabel.setText(getResources().getText(R.string.status_done));
					statusText.setText(" - 100%"); 
        		}else if(pf != null){
        			bar.setVisibility(View.GONE);
        			pauseButton.setVisibility(View.GONE);
					playButton.setVisibility(View.GONE);
        			((ImageView) transferView.findViewById(R.id.transfer_failed)).setVisibility(View.VISIBLE);
        			statusLabel.setText(getResources().getText(R.string.status_failed));
        			statusText.setText(" - "+pf.getCodeDescription());
        		}else if(sp != null){
					bar.setMax(sp.getRequired());
					bar.setProgress(sp.getSucceeded());
					NumberFormat numberFormat = NumberFormat.getNumberInstance();
					numberFormat.setMaximumFractionDigits(1);
					if(sp.isFinalizedTotal()){
						statusText.setTypeface(null, Typeface.NORMAL);
						statusText.setText(
								" - "+numberFormat.format(((float)sp.getSucceeded()/(float)sp.getRequired())*100)+"%");
					}else{
						statusText.setTypeface(null, Typeface.ITALIC);
						statusText.setText(" - (" + getResources().getText(R.string.estimated) + "): "+numberFormat.format(((float)sp.getSucceeded()/(float)sp.getRequired())*100)+"%");
					}
				}
	    		uploadListView.addView(transferView);
			}

			//Files
			for (Upload u : (CopyOnWriteArrayList<Upload>)data.getSerializable(Constants.UPLOADS)) {
				PersistentPut p = u.getPersistentPut();
				LinearLayout transferView = (LinearLayout) getLayoutInflater(new Bundle()).inflate(R.layout.transfer, uploadListView, false);
				TextView transferName = (TextView) transferView.findViewById(R.id.transfer_name);
				ImageButton playButton = (ImageButton) transferView.findViewById(R.id.play_button);
				ImageButton pauseButton = (ImageButton) transferView.findViewById(R.id.pause_button);
				TextView statusLabel = (TextView) transferView.findViewById(R.id.transfer_status_label);
				TextView statusText = (TextView) transferView.findViewById(R.id.transfer_percentage);
				TextView priorityLabel = (TextView) transferView.findViewById(R.id.transfer_priority_label);
				TextView priorityText = (TextView) transferView.findViewById(R.id.transfer_priority);
				ImageView transferDone = (ImageView) transferView.findViewById(R.id.transfer_done);
				if(p.getTargetFilename() == null){
					transferName.setText(p.getIdentifier());
				}else{
					transferName.setText(p.getTargetFilename());
				}
				transferName.setHint(p.getIdentifier());
				if(u.getPriority() == 6){
					pauseButton.setVisibility(View.GONE);
					playButton.setVisibility(View.VISIBLE);
					priorityLabel.setVisibility(View.GONE);
					priorityText.setVisibility(View.GONE);
					statusLabel.setText(getResources().getText(R.string.status_paused));
				}else{
					pauseButton.setVisibility(View.VISIBLE);
					playButton.setVisibility(View.GONE);
					priorityLabel.setVisibility(View.VISIBLE);
					priorityText.setVisibility(View.VISIBLE);
					switch(u.getPriority()){
						case 0:
							priorityText.setText(getResources().getText(R.string.priority_0));
							break;
						case 1:
							priorityText.setText(getResources().getText(R.string.priority_1));
							break;
						case 2:
							priorityText.setText(getResources().getText(R.string.priority_2));
							break;
						case 3:
							priorityText.setText(getResources().getText(R.string.priority_3));
							break;
						case 4:
							priorityText.setText(getResources().getText(R.string.priority_4));
							break;
						case 5:
							priorityText.setText(getResources().getText(R.string.priority_5));
							break;
						default:
					}
					statusLabel.setText(getResources().getText(R.string.status_uploading));
				}
				long dataLength = p.getDataLength();
				if(dataLength != 0){
					((TextView) transferView.findViewById(R.id.transfer_size)).setText(Constants.humanReadableByteCount(dataLength,false));
				}
				ProgressBar bar = (ProgressBar) transferView.findViewById(R.id.transfer_progress);
				SimpleProgress sp = u.getProgress();
				PutFailed pf = u.getPutFailed();
				
				if(u.getPutSuccessful() != null){
        			bar.setMax(1);
        			bar.setProgress(1);
        			bar.setVisibility(View.GONE);
        			pauseButton.setVisibility(View.GONE);
					playButton.setVisibility(View.GONE);
					transferDone.setVisibility(View.VISIBLE);
					statusLabel.setText(getResources().getText(R.string.status_done));
					statusText.setText(" - 100%"); 
        		}else if(pf != null){
        			bar.setVisibility(View.GONE);
        			pauseButton.setVisibility(View.GONE);
					playButton.setVisibility(View.GONE);
        			((ImageView) transferView.findViewById(R.id.transfer_failed)).setVisibility(View.VISIBLE);
        			statusLabel.setText(getResources().getText(R.string.status_failed));
        			statusText.setText(" - "+pf.getCodeDescription());
        		}else if(sp != null){
					bar.setMax(sp.getRequired());
					bar.setProgress(sp.getSucceeded());
					NumberFormat numberFormat = NumberFormat.getNumberInstance();
					numberFormat.setMaximumFractionDigits(1);
					if(sp.isFinalizedTotal()){
						statusText.setTypeface(null, Typeface.NORMAL);
						statusText.setText(
								" - "+numberFormat.format(((float)sp.getSucceeded()/(float)sp.getRequired())*100)+"%");
					}else{
						statusText.setTypeface(null, Typeface.ITALIC);
						statusText.setText(" - (" + getResources().getText(R.string.estimated) + "): "+numberFormat.format(((float)sp.getSucceeded()/(float)sp.getRequired())*100)+"%");
					}
				}
        		
        		uploadListView.addView(transferView);
			}
		}

	}

	public static class DownloadsSectionFragment extends Fragment implements GlobalState.StateListener {

		private View mView;

		public static DownloadsSectionFragment newInstance() {
			// Seems pointless, I know, but this static method
			// Might later be used to pass in arguments via a Bundle
			return new DownloadsSectionFragment();
		}

		public DownloadsSectionFragment() {
			// Default, no argument constructor
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			mView = inflater.inflate(R.layout.fragment_downloads, container, false);
			return mView;
		}

		@SuppressWarnings("unchecked")
		@Override
		public synchronized void onStateChanged(Bundle data) {
			System.out.println("onStateChanged: Downloads");
			LinearLayout downloadListView = (LinearLayout)mView.findViewById(R.id.downloads_list_view);
			downloadListView.removeAllViews();
			if(!data.getBoolean(Constants.IS_CONNECTED)){
                FrameLayout fl = (FrameLayout)getLayoutInflater(null).inflate(R.layout.fragment_no_connectivity, downloadListView, false);
				downloadListView.addView(fl);
				return;
			}
			for (Download d : (CopyOnWriteArrayList<Download>)data.getSerializable(Constants.DOWNLOADS)) {
				PersistentGet p = d.getPersistentGet();
				LinearLayout transferView = (LinearLayout) getLayoutInflater(new Bundle()).inflate(R.layout.transfer, downloadListView, false);
				
				TextView transferName = (TextView) transferView.findViewById(R.id.transfer_name);
				ImageButton playButton = (ImageButton) transferView.findViewById(R.id.play_button);
				ImageButton pauseButton = (ImageButton) transferView.findViewById(R.id.pause_button);
				TextView statusLabel = (TextView) transferView.findViewById(R.id.transfer_status_label);
				TextView statusText = (TextView) transferView.findViewById(R.id.transfer_percentage);
				TextView priorityLabel = (TextView) transferView.findViewById(R.id.transfer_priority_label);
				TextView priorityText = (TextView) transferView.findViewById(R.id.transfer_priority);
				ImageView transferDone = (ImageView) transferView.findViewById(R.id.transfer_done);
				
				transferName.setText(p.getIdentifier());
				transferName.setHint(p.getIdentifier());
				if(d.getPriority() == 6){
					pauseButton.setVisibility(View.GONE);
					playButton.setVisibility(View.VISIBLE);
					priorityLabel.setVisibility(View.GONE);
					priorityText.setVisibility(View.GONE);
					statusLabel.setText(getResources().getText(R.string.status_paused));
				}else{
					pauseButton.setVisibility(View.VISIBLE);
					playButton.setVisibility(View.GONE);
					priorityLabel.setVisibility(View.VISIBLE);
					priorityText.setVisibility(View.VISIBLE);
					switch(d.getPriority()){
						case 0:
							priorityText.setText(getResources().getText(R.string.priority_0));
							break;
						case 1:
							priorityText.setText(getResources().getText(R.string.priority_1));
							break;
						case 2:
							priorityText.setText(getResources().getText(R.string.priority_2));
							break;
						case 3:
							priorityText.setText(getResources().getText(R.string.priority_3));
							break;
						case 4:
							priorityText.setText(getResources().getText(R.string.priority_4));
							break;
						case 5:
							priorityText.setText(getResources().getText(R.string.priority_5));
							break;
						default:
					}
					statusLabel.setText(getResources().getText(R.string.status_downloading));
				}

				long dataLength = d.getDataLength();
				if(dataLength != 0){
					((TextView) transferView.findViewById(R.id.transfer_size)).setText(Constants.humanReadableByteCount(dataLength,false));
				}
				ProgressBar bar = (ProgressBar) transferView.findViewById(R.id.transfer_progress);
				SimpleProgress sp = d.getProgress();
				DataFound df = d.getDataFound();
				GetFailed gf = d.getGetFailed();
				
				if(df != null){
					bar.setMax(1);
					bar.setProgress(1);
					bar.setVisibility(View.GONE);
					pauseButton.setVisibility(View.GONE);
					playButton.setVisibility(View.GONE);
					transferDone.setVisibility(View.VISIBLE);
					statusLabel.setText(getResources().getText(R.string.status_done));
					statusText.setText(" - 100%"); 
				}else if(gf != null){
					bar.setVisibility(View.GONE);
					pauseButton.setVisibility(View.GONE);
					playButton.setVisibility(View.GONE);
					((ImageView) transferView.findViewById(R.id.transfer_failed)).setVisibility(View.VISIBLE);
					statusLabel.setText(getResources().getText(R.string.status_failed));
					statusText.setText(" - "+gf.getCodeDescription());
				}else if(sp != null){
					bar.setMax(sp.getRequired());
					bar.setProgress(sp.getSucceeded());
					NumberFormat numberFormat = NumberFormat.getNumberInstance();
					numberFormat.setMaximumFractionDigits(1);
					if(sp.isFinalizedTotal()){
						statusText.setTypeface(null, Typeface.NORMAL);
						statusText.setText(
								" - "+numberFormat.format(((float)sp.getSucceeded()/(float)sp.getRequired())*100)+"%");
					}else{
						statusText.setTypeface(null, Typeface.ITALIC);
						statusText.setText(" - (" + getResources().getText(R.string.estimated) + "): "+numberFormat.format(((float)sp.getSucceeded()/(float)sp.getRequired())*100)+"%");
					}
					
				}
				
				
				downloadListView.addView(transferView);
			}
		}

	}

	public static class StatusSectionFragment extends Fragment implements GlobalState.StateListener {

		private View mView;

		public static StatusSectionFragment newInstance() {
			// Seems pointless, I know, but this static method
			// Might later be used to pass in arguments via a Bundle
			return new StatusSectionFragment();
		}

		public StatusSectionFragment() {
			// Default, no argument constructor
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			mView = inflater.inflate(R.layout.fragment_status, container, false);

			return mView;
		}

		@Override
		public synchronized void onStateChanged(Bundle data) {
			System.out.println("onStateChanged: Status");


			if(!data.getBoolean(Constants.IS_CONNECTED)){
                mView.findViewById(R.id.no_connection).setVisibility(View.VISIBLE);
				mView.findViewById(R.id.basic_node_status).setVisibility(View.GONE);
				mView.findViewById(R.id.advanced_node_status).setVisibility(View.GONE);
				return;
			}

			NodeStatus aNodeStatus = (NodeStatus)data.getSerializable(Constants.STATUS);
			if(aNodeStatus == null){
				return;
			}

			if(aNodeStatus.isAdvanced()){
                mView.findViewById(R.id.no_connection).setVisibility(View.GONE);
				mView.findViewById(R.id.basic_node_status).setVisibility(View.GONE);
				mView.findViewById(R.id.advanced_node_status).setVisibility(View.VISIBLE);
				((TextView)  mView.findViewById(R.id.status_version_value)).setText(aNodeStatus.getVersion());
				((TextView)  mView.findViewById(R.id.status_input_value)).setText(String.format("%.2f",aNodeStatus.getRecentInputRate())+" KB/s");
				((TextView)  mView.findViewById(R.id.status_output_value)).setText(String.format("%.2f",aNodeStatus.getRecentOutputRate())+" KB/s");
				double uptimeSeconds = aNodeStatus.getUptimeSeconds();
				double uptimeMinutes = uptimeSeconds/60;
				double uptimeHours = uptimeMinutes/60;
				double uptimeDays = uptimeHours/24;
				if(uptimeDays>1){
					((TextView)  mView.findViewById(R.id.status_uptime_value)).setText(String.format("%.0f",uptimeDays)+" Days");
				}else if(uptimeHours > 1){
					((TextView)  mView.findViewById(R.id.status_uptime_value)).setText(String.format("%.0f",uptimeHours)+" Hours");
				}else if(uptimeMinutes > 1){
					((TextView)  mView.findViewById(R.id.status_uptime_value)).setText(String.format("%.0f",uptimeMinutes)+" Minutes");
				}else{
					((TextView)  mView.findViewById(R.id.status_uptime_value)).setText(String.format("%.0f",uptimeSeconds)+" Seconds");
				}
			}else{
                mView.findViewById(R.id.no_connection).setVisibility(View.GONE);
				mView.findViewById(R.id.advanced_node_status).setVisibility(View.GONE);
				mView.findViewById(R.id.basic_node_status).setVisibility(View.VISIBLE);
				((TextView)  mView.findViewById(R.id.basic_status_version_value)).setText(aNodeStatus.getVersion());
			}
		}

	}

}
