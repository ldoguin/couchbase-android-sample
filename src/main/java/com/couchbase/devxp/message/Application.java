package com.couchbase.devxp.message;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.listener.LiteListener;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class Application extends android.app.Application {

    public static String TAG = "Message";
    /*
      If you are running this app from the android emulator, the host IP is 10.0.2.2.
      Sync Gateway's port is 4984.
     */
    private static final String SYNC_URL_HTTP = "http://10.0.2.2:4984/messages";

    private static String DBNAME = "messages";
    private Database database = null;
    private Manager manager;
    private Replication pull;
    private Replication push;

    /*
     * Opens a new Couchbase Lite database instance.
     */
    public Database getDatabase() {
        if (database == null) {
            try {
                manager = new Manager(new AndroidContext(this), Manager.DEFAULT_OPTIONS);
                Log.d(TAG, "Created database manager");

                if (!Manager.isValidDatabaseName(DBNAME)) {
                    Log.e(TAG, "Bad database name");
                    return null;
                }

                try {
                    database = manager.getDatabase(DBNAME);
                    Log.d(TAG, "Database created");
                } catch (CouchbaseLiteException e) {
                    Log.e(TAG, "Database creation failed");
                    return null;
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to create database manager");
                return null;
            }

        }
        return database;
    }

    /*
     * Setup two replication links against the SYNC_URL_HTTP. The URL could point to a Sync Gateway instance or another
     * Couchbase Lite instance using Couchbase Lite Listener.
     */
    private int setupSync() {
        URL url;
        try {
            url = new URL(SYNC_URL_HTTP);
        } catch (MalformedURLException e) {
            Log.e(Application.TAG, "Sync URL is invalid, setting up sync failed");
            return 0;
        }

        pull = database.createPullReplication(url);
        push = database.createPushReplication(url);

        pull.setContinuous(true);
        push.setContinuous(true);

        pull.addChangeListener(getReplicationChangeListener());
        push.addChangeListener(getReplicationChangeListener());

        toggleOnSync();

        /* Instanciate a new Couchbase Lite listener based on TJWS. This launches a new server that other Couchbase Lite
        * instances can replicate against. */
        LiteListener listener = new LiteListener(manager, 5432, null);
        int port = listener.getListenPort();
        Thread thread = new Thread(listener);
        thread.start();

        return port;
    }


    // print out errors and see what is going on
    @SuppressWarnings("ThrowableInstanceNeverThrown")
    private Replication.ChangeListener getReplicationChangeListener() {
        return new Replication.ChangeListener() {
            @Override
            public void changed(Replication.ChangeEvent event) {
                Replication replication = event.getSource();
                if (replication.getLastError() != null) {
                    Throwable lastError = replication.getLastError();
                    if (lastError.getMessage().contains("existing change tracker")) {
                        Log.e("Replication Event", String.format("Sync error: %s:", lastError.getMessage()));
                    }
                }
                Log.d(TAG, event.toString());
                Log.d(TAG, "Completed: " + replication.getCompletedChangesCount() + " of " + replication.getChangesCount());
            }
        };
    }


    @Override
    public void onCreate() {
        // Load up the database on start, if this fails the app is of no use anyway
        if (getDatabase() == null) {
            Log.e(TAG, "Failed to initialize");
            return;
        }

        // Setup the Sync for Couchbase Lite to a Sync Gateway
        int listenerServePort = setupSync();
        super.onCreate();
    }

    public void toggleOffSync() {
        pull.stop();
        push.stop();
    }

    public void toggleOnSync() {
        pull.start();
        push.start();
    }

    public boolean isSyncOn() {
        return pull.isRunning();
    }

}
