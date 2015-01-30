package ca.louisbourque.freenetassistant;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;

import net.pterodactylus.fcp.ARK;
import net.pterodactylus.fcp.AddPeer;
import net.pterodactylus.fcp.DSAGroup;
import net.pterodactylus.fcp.FcpUtils;
import net.pterodactylus.fcp.NodeRef;
import net.pterodactylus.fcp.Version;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class OpenReferenceActivity extends ActionBarActivity {

	public GlobalState gs;
	private AddPeer aPeer;
    private String nodeRef;
	
    private String encodedNodeRef;
    NfcAdapter mNfcAdapter;
    // Flag to indicate that Android Beam is available
    boolean mAndroidBeamAvailable  = false;
    private Uri[] mFileUris = new Uri[1];

    protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();
	    setContentView(R.layout.activity_open_reference);
		this.gs = (GlobalState) getApplication();

        // Set up the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        // setHasOptionsMenu(true);
        setSupportActionBar(toolbar);

        // NFC isn't available on the device
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC)) {
            mAndroidBeamAvailable = true;
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        }
		
	    if (Intent.ACTION_VIEW.equals(action) && type != null) {
	        //if ("text/plain".equals(type)) {
            this.nodeRef = handleSendText(intent); // Handle text being sent

            findViewById(R.id.addNodeRef).setVisibility(View.VISIBLE);
	        //}
	    } else {
	        int selected = intent.getIntExtra(Constants.LOCAL_NODE_SELECTED,-1);
            if(selected >= 0){
                this.nodeRef = this.gs.getLocalNodeList().get(selected).getNodeReference();
                this.encodedNodeRef = this.gs.getLocalNodeList().get(selected).getEncodedNodeReference();

                if(mAndroidBeamAvailable){
                    findViewById(R.id.shareNodeRef).setVisibility(View.VISIBLE);
                    File outFile = copyFileToInternal();
                    mFileUris[0] = Uri.fromFile(outFile);
                    mNfcAdapter.setBeamPushUris(mFileUris,this);
                }
            }
	    }
        if (this.nodeRef != null) {
            TextView textView = (TextView) findViewById(R.id.NodeRef_value);
            textView.setText(this.nodeRef);
        }
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
	
	private String handleSendText(Intent intent) {
		Uri uri = intent.getData();
		BufferedReader in;
		StringBuilder sb = new StringBuilder(10000);
		String arkPubURI = null;
        String arkPrivURI = null;
        String arkNumber = null;
        String dsaGroupG = null;
        String dsaGroupP = null;
        String dsaGroupQ = null;
        String ecdsaP256pub = null;
        String sigP256 = null;
        String str;
        NodeRef aNode = new NodeRef();
        //hack for case when Location is not set in NodeRef
        aNode.setLocation(-1);
		try {
			in = new BufferedReader(new InputStreamReader( getContentResolver().openInputStream(uri)));
	        
	        
	        while ((str = in.readLine()) != null) {
	            sb.append(str + "\n");
	            String str2;
	            if(str.startsWith("identity=")){
	            	if(str.charAt(9) == '=')
	            		str2 = new String(Base64.decode(str.substring(10), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(9);
	            	aNode.setIdentity(str2);
	            }else if(str.startsWith("opennet=")){
	            	if(str.charAt(8) == '=')
	            		str2 = new String(Base64.decode(str.substring(9), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(8);
	            	aNode.setOpennet(Boolean.valueOf(str2));
	            }else if(str.startsWith("myName=")){
	            	if(str.charAt(7) == '=')
	            		str2 = new String(Base64.decode(str.substring(8), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(7);
	            	aNode.setName(str2);
	            }else if(str.startsWith("location=")){
	            	if(str.charAt(9) == '=')
	            		str2 = new String(Base64.decode(str.substring(10), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(9);
	            	aNode.setLocation(Double.valueOf(str2));
	            }else if(str.startsWith("physical.udp=")){
	            	if(str.charAt(13) == '=')
	            		str2 = new String(Base64.decode(str.substring(14), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(13);
	            	aNode.setPhysicalUDP(str2);
	            }else if(str.startsWith("ark.pubURI=")){
	            	
	            	arkPubURI = str.substring(11);
	            }else if(str.startsWith("ark.privURI=")){
	            	if(str.charAt(12) == '=')
	            		str2 = new String(Base64.decode(str.substring(13), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(12);
	            	arkPrivURI = str2;
	            }else if(str.startsWith("ark.number=")){
	            	if(str.charAt(11) == '=')
	            		str2 = new String(Base64.decode(str.substring(12), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(11);
	            	arkNumber = str2;
	            }else if(str.startsWith("dsaPubKey.y=")){
	            	if(str.charAt(12) == '=')
	            		str2 = new String(Base64.decode(str.substring(13), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(12);
	            	aNode.setDSAPublicKey(str2);
	            }else if(str.startsWith("dsaGroup.g=")){
	            	if(str.charAt(11) == '=')
	            		str2 = new String(Base64.decode(str.substring(12), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(11);
	            	dsaGroupG = str2;
	            }else if(str.startsWith("dsaGroup.p=")){
	            	if(str.charAt(11) == '=')
	            		str2 = new String(Base64.decode(str.substring(12), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(11);
	            	dsaGroupP = str2;
	            }else if(str.startsWith("dsaGroup.q=")){
	            	if(str.charAt(11) == '=')
	            		str2 = new String(Base64.decode(str.substring(12), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(11);
	            	dsaGroupQ = str2;
	            }else if(str.startsWith("auth.negTypes=")){
	            	if(str.charAt(14) == '=')
	            		str2 = new String(Base64.decode(str.substring(15), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(14);
	            	aNode.setNegotiationTypes(FcpUtils.decodeMultiIntegerField(str2));
	            }else if(str.startsWith("version=")){
	            	if(str.charAt(8) == '=')
	            		str2 = new String(Base64.decode(str.substring(9), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(8);
	            	aNode.setVersion(new Version(str2));
	            }else if(str.startsWith("lastGoodVersion=")){
	            	if(str.charAt(16) == '=')
	            		str2 = new String(Base64.decode(str.substring(17), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(16);
	            	aNode.setLastGoodVersion(new Version(str2));
	            }else if(str.startsWith("testnet=")){
	            	if(str.charAt(8) == '=')
	            		str2 = new String(Base64.decode(str.substring(9), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(8);
	            	aNode.setTestnet(Boolean.valueOf(str2));
	            }else if(str.startsWith("sig=")){
	            	if(str.charAt(4) == '=')
	            		str2 = new String(Base64.decode(str.substring(5), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(4);
	            	aNode.setSignature(str2);
	            }else if(str.startsWith("ecdsa.P256.pub=")){
	            	if(str.charAt(15) == '=')
	            		str2 = new String(Base64.decode(str.substring(16), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(15);
	            	ecdsaP256pub = str2;
	            }else if(str.startsWith("sigP256=")){
	            	if(str.charAt(8) == '=')
	            		str2 = new String(Base64.decode(str.substring(9), Base64.DEFAULT));
	            	else
	            		str2 = str.substring(8);
	            	sigP256 = str2;
	            }
	            
	        }
	        aNode.setARK(new ARK(arkPubURI,arkPrivURI,arkNumber));
	        aNode.setDSAGroup(new DSAGroup(dsaGroupG,dsaGroupP,dsaGroupQ));
	        this.aPeer = new AddPeer(aNode);
	        if(ecdsaP256pub != null){
	        	aPeer.setField("ecdsa.P256.pub", ecdsaP256pub);
	        }
	        if(sigP256 != null){
	        	aPeer.setField("sigP256", sigP256);
	        }
	        //TODO: Make these user-selected
			aPeer.setField("Trust", "NORMAL");
			aPeer.setField("Visibility", "NO");
			
			/*System.out.println("dsaPubKey.y: "+aPeer.getField("dsaPubKey.y"));
			System.out.println("dsaGroup.g: "+aPeer.getField("dsaGroup.g"));
			System.out.println("dsaGroup.p: "+aPeer.getField("dsaGroup.p"));
			System.out.println("dsaGroup.q: "+aPeer.getField("dsaGroup.q"));*/
	        in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return sb.toString();
	}

	public void cancelReference(View view) {
		finish();
	}
	
	public void addReference(View view) {
		try {
			this.gs.getQueue().put(Message.obtain(null, 0, Constants.MsgAddNoderef,0,(Object)this.aPeer));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setResult(Activity.RESULT_OK);
		Toast.makeText(this, R.string.addingNodeRef, Toast.LENGTH_SHORT).show();
		finish();
		
	}

    public void shareReference(View view) {
        startActivity(shareReference());
    }

    public Intent shareReference(){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        File outFile = copyFileToInternal();
        if(outFile == null) return null;
        Uri uri = Uri.fromFile(outFile);

        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        return shareIntent;
    }


    private File copyFileToInternal() {
        try {
            InputStream is = new ByteArrayInputStream(encodedNodeRef.getBytes());
            File refDir = new File(getExternalFilesDir(null), "fref");

            clearFolder(refDir);
            //Save to a random location, to prevent guess location of ref
            File outFile = new File(refDir, "myref.fref");
            if(refDir.mkdirs() && outFile.createNewFile()){
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
            outFile.setReadable(true, false);
            return outFile;
        } catch (IOException e) {
            e.printStackTrace(); // TODO: should close streams properly here
        }
        return null;
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
