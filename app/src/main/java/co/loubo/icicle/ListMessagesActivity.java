package co.loubo.icicle;

import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.concurrent.CopyOnWriteArrayList;


public class ListMessagesActivity extends ActionBarActivity {

    private GlobalState gs;
    private SwipeRefreshLayout swipeLayoutMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_messages);

        this.gs = (GlobalState) getApplication();

        // Set up the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        // setHasOptionsMenu(true);
        setSupportActionBar(toolbar);

        swipeLayoutMessages = (SwipeRefreshLayout) findViewById(R.id.swipe_container_messages);
        swipeLayoutMessages.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                redrawMessageList();
            }
        });
        swipeLayoutMessages.setColorSchemeResources(R.color.primary,
                R.color.accent);

        redrawMessageList();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list_messages, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            redrawMessageList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void redrawMessageList(){
        CopyOnWriteArrayList<FreenetMessage> messages = this.gs.getMessageList();
        LinearLayout messageList = (LinearLayout)findViewById(R.id.message_list_view);
        messageList.removeAllViews();
        for(FreenetMessage msg: messages){
            LinearLayout ms = (LinearLayout)getLayoutInflater().inflate(R.layout.message_summary, messageList, false);
            TextView sender = (TextView) ms.findViewById(R.id.message_name);
            TextView messageText = (TextView) ms.findViewById(R.id.message_summary);
            sender.setText(msg.getSender());
            messageText.setText(msg.getMessage());
            messageList.addView(ms);
        }
        swipeLayoutMessages.setRefreshing(false);
    }
}
