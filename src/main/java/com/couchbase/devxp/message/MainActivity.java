package com.couchbase.devxp.message;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Query;
import com.couchbase.lite.util.Log;

public class MainActivity extends ListActivity {

    private Database database;
    private Application application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(Application.TAG, "Starting MainActivity");
        application = (Application) getApplication();

        // Wire up the list view with the message in the Database via the Message Adapter for display
        this.database = application.getDatabase();

        Query all = Message.findAllByDate(database);
        setListAdapter(new MessageAdapter(all.toLiveQuery(), this));
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        final Menu m = menu;
        final MenuItem item = menu.findItem(R.id.switchSyncLayout);
        Switch sw = (Switch) item.getActionView().findViewById(R.id.switchSync);
        sw.setChecked(application.isSyncOn());
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    application.toggleOnSync();
                } else {
                    application.toggleOffSync();
                }
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // Wire up the button defined in the main_menu.xml
        if (id == R.id.action_add) {
            addMessage();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Create the button to add new message as needed
    private void addMessage() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Message");
        final EditText input = new EditText(this);
        input.setMaxLines(1);
        input.setSingleLine(true);
        alert.setView(input);

        alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = input.getText().toString();
                Message message = new Message(database);
                message.setMessage(title);
                try {
                    message.save();
                } catch (CouchbaseLiteException e) {
                    Log.e(Application.TAG, "Failed to save message");
                    return;
                }
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        alert.show();

    }
}
