package ca.louisbourque.freenetassistant;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import net.pterodactylus.fcp.ARK;
import net.pterodactylus.fcp.AddPeer;
import net.pterodactylus.fcp.DSAGroup;
import net.pterodactylus.fcp.FcpUtils;
import net.pterodactylus.fcp.NodeRef;
import net.pterodactylus.fcp.Version;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.util.Base64;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class OpenReferenceActivity extends Activity {

	public GlobalState gs;
	private AddPeer aPeer;
	
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();
	    setContentView(R.layout.activity_open_reference);
		this.gs = (GlobalState) getApplication();
		this.gs.startFCPService();
		
	    if (Intent.ACTION_VIEW.equals(action) && type != null) {
	        //if ("text/plain".equals(type)) {
	            handleSendText(intent); // Handle text being sent
	        //}
	    } else {
	        // Handle other intents, such as being started from the home screen
	    }
	
	}
	@Override
	public void onDestroy(){
		super.onDestroy();
		if(isFinishing()){
			this.gs.stopFCPService(false);
		}
	}
	
	void handleSendText(Intent intent) {
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
	    if (sb != null) {
	    	TextView textView = (TextView) findViewById(R.id.NodeRef_value);
	    	textView.setText(sb.toString());
	    }
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
}
