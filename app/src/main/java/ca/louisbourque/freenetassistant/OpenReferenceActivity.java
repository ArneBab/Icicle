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
import java.nio.charset.Charset;
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
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class OpenReferenceActivity extends ActionBarActivity implements NfcAdapter.CreateNdefMessageCallback {

	public GlobalState gs;
	private AddPeer aPeer;
    private String nodeRef;
    private Spinner lnTrust;
    private Spinner lnVisibility;
	
    private String encodedNodeRef;
    NfcAdapter mNfcAdapter;
    // Flag to indicate that Android Beam is available
    boolean mAndroidBeamAvailable  = false;

    protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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


	}

    @Override
    public void onResume() {
        super.onResume();
        // Check to see that the Activity started due to an Android Beam

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            //opening friend node from NFC
            this.nodeRef = processNFCIntent(intent);
            if(this.gs.isConnected()) {
                findViewById(R.id.addNodeRef).setVisibility(View.VISIBLE);
            }else {
                findViewById(R.id.saveNodeRef).setVisibility(View.VISIBLE);
            }
            setupSpinners();

        }else if (Intent.ACTION_VIEW.equals(action) && type != null) {
            //opening friend node from intent
            this.nodeRef = handleSendText(intent); // Handle text being sent
            if(this.gs.isConnected()) {
                findViewById(R.id.addNodeRef).setVisibility(View.VISIBLE);
            }else {
                findViewById(R.id.saveNodeRef).setVisibility(View.VISIBLE);
            }
            setupSpinners();
        } else {
            //opening own node
            findViewById(R.id.trust_visibility_row).setVisibility(View.GONE);
            int selected = intent.getIntExtra(Constants.LOCAL_NODE_SELECTED,-1);
            if(selected >= 0){
                this.nodeRef = this.gs.getLocalNodeList().get(selected).getNodeReference();
                this.encodedNodeRef = this.gs.getLocalNodeList().get(selected).getEncodedNodeReference();

                if(mAndroidBeamAvailable){
                    findViewById(R.id.shareNodeRef).setVisibility(View.VISIBLE);
                    mNfcAdapter.setNdefPushMessageCallback(this, this);
                }
            }
        }
        if (this.nodeRef != null) {
            TextView textView = (TextView) findViewById(R.id.NodeRef_value);
            textView.setText(this.nodeRef);
        }
    }

    private void setupSpinners() {
        this.lnTrust =(Spinner) findViewById(R.id.trust_spinner);
        this.lnVisibility =(Spinner) findViewById(R.id.visibility_spinner);
        ArrayAdapter<String> adapterT = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, Constants.TrustValues);
        this.lnTrust.setAdapter(adapterT);
        ArrayAdapter<String> adapterV = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, Constants.VisibilityValues);
        this.lnVisibility.setAdapter(adapterV);
        this.lnTrust.setSelection(Constants.TrustValues.indexOf(this.aPeer.getField("Trust")));
        this.lnVisibility.setSelection(Constants.VisibilityValues.indexOf(this.aPeer.getField("Visibility")));

        this.lnTrust.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                aPeer.setField("Trust", lnTrust.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // do nothing
            }

        });

        this.lnVisibility.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                aPeer.setField("Visibility", lnVisibility.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // do nothing
            }

        });
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
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

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    public String processNFCIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        //textView.setText(new String(msg.getRecords()[0].getPayload()));
        return processStringIntoNode(new String(msg.getRecords()[0].getPayload()));
    }

	private String handleSendText(Intent intent) {
		Uri uri = intent.getData();
		BufferedReader in;
		StringBuilder sb = new StringBuilder(10000);
        String str;
		try {
			in = new BufferedReader(new InputStreamReader( getContentResolver().openInputStream(uri)));
	        
	        
	        while ((str = in.readLine()) != null) {
                sb.append(str).append("\n");

            }
			/*System.out.println("dsaPubKey.y: "+aPeer.getField("dsaPubKey.y"));
			System.out.println("dsaGroup.g: "+aPeer.getField("dsaGroup.g"));
			System.out.println("dsaGroup.p: "+aPeer.getField("dsaGroup.p"));
			System.out.println("dsaGroup.q: "+aPeer.getField("dsaGroup.q"));*/
            in.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
        return processStringIntoNode(sb.toString());
	}

    public String processStringIntoNode(String in){
        String arkPubURI = null;
        String arkPrivURI = null;
        String arkNumber = null;
        String dsaGroupG = null;
        String dsaGroupP = null;
        String dsaGroupQ = null;
        String ecdsaP256pub = null;
        String sigP256 = null;

        NodeRef aNode = new NodeRef();
        //hack for case when Location is not set in NodeRef
        aNode.setLocation(-1);
        String str2;
        String[] array = in.split("\\r?\\n");

        for (String anArray : array) {
            if (anArray.startsWith("identity=")) {
                if (anArray.charAt(9) == '=')
                    str2 = new String(Base64.decode(anArray.substring(10), Base64.DEFAULT));
                else
                    str2 = anArray.substring(9);
                aNode.setIdentity(str2);
            } else if (anArray.startsWith("opennet=")) {
                if (anArray.charAt(8) == '=')
                    str2 = new String(Base64.decode(anArray.substring(9), Base64.DEFAULT));
                else
                    str2 = anArray.substring(8);
                aNode.setOpennet(Boolean.valueOf(str2));
            } else if (anArray.startsWith("myName=")) {
                if (anArray.charAt(7) == '=')
                    str2 = new String(Base64.decode(anArray.substring(8), Base64.DEFAULT));
                else
                    str2 = anArray.substring(7);
                aNode.setName(str2);
            } else if (anArray.startsWith("location=")) {
                if (anArray.charAt(9) == '=')
                    str2 = new String(Base64.decode(anArray.substring(10), Base64.DEFAULT));
                else
                    str2 = anArray.substring(9);
                aNode.setLocation(Double.valueOf(str2));
            } else if (anArray.startsWith("physical.udp=")) {
                if (anArray.charAt(13) == '=')
                    str2 = new String(Base64.decode(anArray.substring(14), Base64.DEFAULT));
                else
                    str2 = anArray.substring(13);
                aNode.setPhysicalUDP(str2);
            } else if (anArray.startsWith("ark.pubURI=")) {

                arkPubURI = anArray.substring(11);
            } else if (anArray.startsWith("ark.privURI=")) {
                if (anArray.charAt(12) == '=')
                    str2 = new String(Base64.decode(anArray.substring(13), Base64.DEFAULT));
                else
                    str2 = anArray.substring(12);
                arkPrivURI = str2;
            } else if (anArray.startsWith("ark.number=")) {
                if (anArray.charAt(11) == '=')
                    str2 = new String(Base64.decode(anArray.substring(12), Base64.DEFAULT));
                else
                    str2 = anArray.substring(11);
                arkNumber = str2;
            } else if (anArray.startsWith("dsaPubKey.y=")) {
                if (anArray.charAt(12) == '=')
                    str2 = new String(Base64.decode(anArray.substring(13), Base64.DEFAULT));
                else
                    str2 = anArray.substring(12);
                aNode.setDSAPublicKey(str2);
            } else if (anArray.startsWith("dsaGroup.g=")) {
                if (anArray.charAt(11) == '=')
                    str2 = new String(Base64.decode(anArray.substring(12), Base64.DEFAULT));
                else
                    str2 = anArray.substring(11);
                dsaGroupG = str2;
            } else if (anArray.startsWith("dsaGroup.p=")) {
                if (anArray.charAt(11) == '=')
                    str2 = new String(Base64.decode(anArray.substring(12), Base64.DEFAULT));
                else
                    str2 = anArray.substring(11);
                dsaGroupP = str2;
            } else if (anArray.startsWith("dsaGroup.q=")) {
                if (anArray.charAt(11) == '=')
                    str2 = new String(Base64.decode(anArray.substring(12), Base64.DEFAULT));
                else
                    str2 = anArray.substring(11);
                dsaGroupQ = str2;
            } else if (anArray.startsWith("auth.negTypes=")) {
                if (anArray.charAt(14) == '=')
                    str2 = new String(Base64.decode(anArray.substring(15), Base64.DEFAULT));
                else
                    str2 = anArray.substring(14);
                aNode.setNegotiationTypes(FcpUtils.decodeMultiIntegerField(str2));
            } else if (anArray.startsWith("version=")) {
                if (anArray.charAt(8) == '=')
                    str2 = new String(Base64.decode(anArray.substring(9), Base64.DEFAULT));
                else
                    str2 = anArray.substring(8);
                aNode.setVersion(new Version(str2));
            } else if (anArray.startsWith("lastGoodVersion=")) {
                if (anArray.charAt(16) == '=')
                    str2 = new String(Base64.decode(anArray.substring(17), Base64.DEFAULT));
                else
                    str2 = anArray.substring(16);
                aNode.setLastGoodVersion(new Version(str2));
            } else if (anArray.startsWith("testnet=")) {
                if (anArray.charAt(8) == '=')
                    str2 = new String(Base64.decode(anArray.substring(9), Base64.DEFAULT));
                else
                    str2 = anArray.substring(8);
                aNode.setTestnet(Boolean.valueOf(str2));
            } else if (anArray.startsWith("sig=")) {
                if (anArray.charAt(4) == '=')
                    str2 = new String(Base64.decode(anArray.substring(5), Base64.DEFAULT));
                else
                    str2 = anArray.substring(4);
                aNode.setSignature(str2);
            } else if (anArray.startsWith("ecdsa.P256.pub=")) {
                if (anArray.charAt(15) == '=')
                    str2 = new String(Base64.decode(anArray.substring(16), Base64.DEFAULT));
                else
                    str2 = anArray.substring(15);
                ecdsaP256pub = str2;
            } else if (anArray.startsWith("sigP256=")) {
                if (anArray.charAt(8) == '=')
                    str2 = new String(Base64.decode(anArray.substring(9), Base64.DEFAULT));
                else
                    str2 = anArray.substring(8);
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
        aPeer.setField("Trust", Constants.DEFAULT_TRUST);
        aPeer.setField("Visibility", Constants.DEFAULT_VISIBILITY);

        return in;
    }

	public void cancelReference(View view) {
		finish();
	}

    public void saveReference(View view) {
        saveNodeRef();
        Toast.makeText(this, R.string.savingNodeRef, Toast.LENGTH_SHORT).show();
        finish();
    }

	public void addReference(View view) {
		try {
            saveNodeRef();
			this.gs.getQueue().put(Message.obtain(null, 0, Constants.MsgAddNoderef,0,(Object)this.aPeer));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		setResult(Activity.RESULT_OK);
		Toast.makeText(this, R.string.addingNodeRef, Toast.LENGTH_SHORT).show();
		finish();
	}

    public void saveNodeRef(){
        FriendNode ref = new FriendNode(this.aPeer.getField("myName"),this.aPeer.getField("identity"),this.aPeer.getField("Trust"),this.aPeer.getField("Visibility"));
        this.gs.addFriendNode(ref);
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
        InputStream is;
        OutputStream os;
        try {
            is = new ByteArrayInputStream(encodedNodeRef.getBytes());
            File refDir = new File(getExternalFilesDir(null), "fref");

            clearFolder(refDir);
            //Save to a random location, to prevent guess location of ref
            File outFile = new File(refDir, "myref.fref");
            if(refDir.mkdirs() && outFile.createNewFile()){
                os = new FileOutputStream(outFile.getAbsolutePath());

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
            e.printStackTrace();
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

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        NdefMessage msg = new NdefMessage(
                new NdefRecord[] { NdefRecord.createMime(
                        "application/vnd.ca.louisbourque.freenetassistant", this.encodedNodeRef.getBytes(Charset.forName("US-ASCII")))
                        /**
                         * The Android Application Record (AAR) is commented out. When a device
                         * receives a push with an AAR in it, the application specified in the AAR
                         * is guaranteed to run. The AAR overrides the tag dispatch system.
                         * You can add it back in to guarantee that this
                         * activity starts when receiving a beamed message. For now, this code
                         * uses the tag dispatch system.
                         */
                        //,NdefRecord.createApplicationRecord("com.example.android.beam")
                });
        return msg;
    }
}
