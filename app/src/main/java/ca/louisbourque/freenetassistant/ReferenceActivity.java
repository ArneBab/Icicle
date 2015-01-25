package ca.louisbourque.freenetassistant;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;

import net.pterodactylus.fcp.NodeData;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ActionProvider;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class ReferenceActivity extends ActionBarActivity {
	
	private GlobalState gs;
	private NodeData myNode;
	private String refStr;
	private String EncodedStr;
	private String randomStr;
    private ShareActionProvider actionProvider;

	@SuppressLint("TrulyRandom")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reference);
		this.gs = (GlobalState) getApplication();
		this.myNode = this.gs.getNodeData();

        // Set up the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        // setHasOptionsMenu(true);
        setSupportActionBar(toolbar);
		
		//final ActionBar actionBar = getActionBar();
		SecureRandom random = new SecureRandom();
		randomStr = new BigInteger(130, random).toString(32);
		refStr = "";
		String temp = "";
		EncodedStr = "";
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
        actionProvider = (ShareActionProvider)MenuItemCompat.getActionProvider(menuItem);

	    // Attach an intent to this ShareActionProvider.  You can update this at any time,
	    // like when the user selects a new piece of data they might like to share.
        actionProvider.setShareIntent(shareReference());
		
		
		return super.onCreateOptionsMenu(menu);
	}
	
	public Intent shareReference(){
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
	    shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
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
