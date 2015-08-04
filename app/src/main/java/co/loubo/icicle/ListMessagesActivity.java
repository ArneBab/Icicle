package co.loubo.icicle;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;


public class ListMessagesActivity extends ActionBarActivity implements ListMessagesFragment.OnItemSelectedListener {

    private GlobalState gs;
    private ListView list;
    private ListMessagesFragment mListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_messages);

        this.gs = (GlobalState) getApplication();
        this.list = (ListView)findViewById(android.R.id.list);
        mListFragment = (ListMessagesFragment) getSupportFragmentManager().findFragmentById(R.id.listFragment);
        // Set up the action bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        // setHasOptionsMenu(true);
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mListFragment.notifyDataSetChanged();
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


    }
}
