package ca.louisbourque.freenetassistant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import net.pterodactylus.fcp.SSKKeypair;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
 
public class UploadActivity extends ActionBarActivity {
 
	private static final int SELECT_FILE = 0;
    private static final int SELECT_FILE_KITKAT = 1;
    private ImageButton thumbnail;
	private FileUploadMessage fileUploadMessage;
	private GlobalState gs;
	private SSKKeypair anSSKey;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_upload);
        // Set up the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        // setHasOptionsMenu(true);
        setSupportActionBar(toolbar);
        thumbnail = (ImageButton) findViewById(R.id.thumbnail);
        this.gs = (GlobalState) getApplication();
        this.fileUploadMessage = new FileUploadMessage();
        new GetSSKeypairTask().execute("");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String strUploadKeyType = sharedPref.getString(Constants.PREF_UPLOAD_KEY, Constants.KEY_TYPE_DEFAULT);
        RadioButton radioButton;
        ImageButton uploadButton = (ImageButton) this.findViewById(R.id.file_upload_button);
		if(strUploadKeyType.equals(Constants.KEY_TYPE_SSK)){
			radioButton = (RadioButton) this.findViewById(R.id.radio_button_SSK);
			radioButton.setChecked(true);
			uploadButton.setEnabled(anSSKey != null);
		}else{
			radioButton = (RadioButton) this.findViewById(R.id.radio_button_CHK);
			radioButton.setChecked(true);
			uploadButton.setEnabled(true);
		}
    }
    
    public void pickFile(View view) {
        if (Build.VERSION.SDK_INT <19){
            Intent intent = new Intent();
            intent.setType("image/jpeg");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.select_file)),SELECT_FILE);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, SELECT_FILE_KITKAT);
        }
    }
    
    public void updateKeyType(View view){
    	RadioButton chk_rb = (RadioButton) this.findViewById(R.id.radio_button_CHK);
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	Editor editor = sharedPref.edit();
    	ImageButton uploadButton = (ImageButton) this.findViewById(R.id.file_upload_button);
    	if(chk_rb.isChecked()){
    		//User has selected CHK
    		uploadButton.setEnabled(true);
    		editor.putString(Constants.PREF_UPLOAD_KEY, Constants.KEY_TYPE_CHK);
    	}else{
    		//User has selected SSK
    		uploadButton.setEnabled(anSSKey != null);
    		editor.putString(Constants.PREF_UPLOAD_KEY, Constants.KEY_TYPE_SSK);
    	}
    	 editor.commit();
    	 if(anSSKey != null){
    		 RadioButton ssk_rb = (RadioButton) this.findViewById(R.id.radio_button_SSK);
    		 ssk_rb.setText(R.string.SSK);
    	 }
    }
    
    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	if(resultCode != RESULT_OK || data == null) return;
        if (requestCode != SELECT_FILE && requestCode != SELECT_FILE_KITKAT) return;
        Uri selectedFileUri = null;
        if (requestCode == SELECT_FILE) {
            selectedFileUri = data.getData();
        } else if (requestCode == SELECT_FILE_KITKAT) {
            selectedFileUri = data.getData();
            final int takeFlags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // Check for the freshest data.

            //noinspection ResourceType
            getContentResolver().takePersistableUriPermission(selectedFileUri, takeFlags);
        }
        if(selectedFileUri == null) return;

        ImageButton uploadButton = (ImageButton) this.findViewById(R.id.file_upload_button);
        ImageButton pickButton = (ImageButton) this.findViewById(R.id.file_picker_button);
        TextView exifRemoved = (TextView) this.findViewById(R.id.remove_exif);
        exifRemoved.setVisibility(View.GONE);
        pickButton.setVisibility(View.GONE);
        uploadButton.setVisibility(View.VISIBLE);
        thumbnail.setVisibility(View.VISIBLE);
        TextView instructions = (TextView) this.findViewById(R.id.file_upload_instructions);
        instructions.setText(R.string.file_upload_instructions_another);


        fileUploadMessage.setUri(selectedFileUri);
        //just to display the imagepath
        //Toast.makeText(this.getApplicationContext(), filemanagerstring, Toast.LENGTH_SHORT).show();
        ContentResolver cR = getApplicationContext().getContentResolver();
        fileUploadMessage.setMimeType(cR.getType(selectedFileUri));
        if(fileUploadMessage.getMimeType().startsWith("image/")){
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                InputStream is = null;
                is = cR.openInputStream(selectedFileUri);
                BitmapFactory.decodeStream(is,null,options);
                is.close();
                is = cR.openInputStream(selectedFileUri);
                // here w and h are the desired width and height
                options.inSampleSize = Math.max(options.outWidth/512, options.outHeight/512);
                // bitmap is the resized bitmap
                Bitmap bitmap = BitmapFactory.decodeStream(is,null,options);
                thumbnail.setImageBitmap(bitmap);

                Cursor returnCursor =
                        getContentResolver().query(selectedFileUri, null, null, null, null);
                int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                returnCursor.moveToFirst();
                fileUploadMessage.setName(returnCursor.getString(nameIndex));
                fileUploadMessage.setSize(returnCursor.getLong(sizeIndex));

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if(fileUploadMessage.getMimeType().equals("image/jpeg")){
                exifRemoved.setVisibility(View.VISIBLE);
            }
        }else{
            //TODO: check for other common file types
            thumbnail.setImageResource(R.drawable.ic_action_photo);
        }
        RadioButton chk_rb = (RadioButton) this.findViewById(R.id.radio_button_CHK);
        if(chk_rb.isChecked()){
            fileUploadMessage.setKey(Constants.KEY_TYPE_CHK);
        }else{
            fileUploadMessage.setKey(anSSKey.getInsertURI()+fileUploadMessage.getName());
        }

    }
    
    public void uploadFile(View view) {
    	try {
			executeMultipartPost();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void executeMultipartPost() throws Exception {
		try {
			this.gs.getQueue().put(Message.obtain(null, 0, Constants.MsgFileUpload,0,(Object)fileUploadMessage));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setResult(Activity.RESULT_OK);
		finish();
	}
    

    private class GetSSKeypairTask extends AsyncTask<String, Void, SSKKeypair> {
        /** The system calls this to perform work in a worker thread and
          * delivers it the parameters given to AsyncTask.execute() */
        protected SSKKeypair doInBackground(String... urls) {
            return gs.getSSKKeypair();
        }
        
        /** The system calls this to perform work in the UI thread and delivers
          * the result from doInBackground() */
        protected void onPostExecute(SSKKeypair result) {
        	anSSKey = result;
        	updateKeyType(null);
        }
    }
}