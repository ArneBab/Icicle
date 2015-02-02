package ca.louisbourque.freenetassistant;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class ReferenceActivity extends ActionBarActivity {
	
	private GlobalState gs;
    private String EncodedStr;
	private String randomStr;

    @SuppressLint("TrulyRandom")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reference);
		this.gs = (GlobalState) getApplication();
		LocalNode an = this.gs.getActiveLocalNode();
        String refStr = an.getNodeReference();
        EncodedStr = an.getEncodedNodeReference();
        // Set up the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        // setHasOptionsMenu(true);
        setSupportActionBar(toolbar);
		
		//final ActionBar actionBar = getActionBar();
		SecureRandom random = new SecureRandom();
		randomStr = new BigInteger(130, random).toString(32);
		
		TextView nodeText = (TextView) this.findViewById(R.id.text_reference);
		nodeText.setText(refStr);
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
        
	}

    @Override
    protected void onStart() {
        this.gs.registerActivity(this);
        super.onStart();
    }

    @Override
    protected void onStop() {
        this.gs.unregisterActivity(this);
        super.onStop();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.reference, menu);
		// Get the menu item.
	    MenuItem menuItem = menu.findItem(R.id.action_share);
	    // Get the provider and hold onto it to set/change the share intent.
        ShareActionProvider actionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

	    // Attach an intent to this ShareActionProvider.  You can update this at any time,
	    // like when the user selects a new piece of data they might like to share.
        actionProvider.setShareIntent(shareReference());
		
		
		return super.onCreateOptionsMenu(menu);
	}
	
	public Intent shareReference(){
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
	    shareIntent.setType("text/plain");
	    copyFileToInternal();
	    Uri uri = Uri.parse("content://ca.louisbourque.freenetassistant.fref/fref/"+randomStr+"/myref.fref");
	    
	    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
	    return shareIntent;
	}
	
	
	private void copyFileToInternal() {
	    try {
	        InputStream is = new ByteArrayInputStream(EncodedStr.getBytes());
	        File refDir = new File(getCacheDir(), "fref");

	        clearFolder(refDir);
	        //Save to a random location, to prevent guess location of ref
	        File randomDir = new File(refDir, randomStr);
	        File outFile = new File(randomDir, "myref.fref");
	        randomDir.mkdirs();
	        if(outFile.createNewFile()){
		        OutputStream os = new FileOutputStream(outFile.getAbsolutePath());
		
		        byte[] buff = new byte[1024];
		        int len;
		        while ((len = is.read(buff)) > 0) {
		            os.write(buff, 0, len);
		        }
		        os.flush();
		        os.close();
		        is.close();
	        }
	    } catch (IOException e) {
	        e.printStackTrace(); // TODO: should close streams properly here
	    }
	}
	
	private void clearFolder(File dir) {

        File[] files = dir.listFiles();
        if(files == null){
        	return;
        }
        for (File file : files) {
        	file.delete();
        }
    }
}
