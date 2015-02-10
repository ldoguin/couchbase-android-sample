package com.couchbase.devxp.message;

import com.couchbase.lite.*;
import com.couchbase.lite.util.Log;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/*
 * Simple POJO representing a message object and associated CRUD methods.
 */
public class Message {
    public static String VIEW_NAME = "messages-view";
    public static String TYPE = "message";
    private Document sourceDocument;
    private Database database;
    private Date createdAt;
    private String message;

    public static Query findAllByDate(Database database) {
        View view = database.getView(VIEW_NAME);
        if (view.getMap() == null) {
            view.setMap(new Mapper() {
                @Override
                public void map(Map<String, Object> document, Emitter emitter) {
                    if (TYPE.equals(document.get("type"))) {
                        emitter.emit(document.get("created_at"), null);
                    }
                }
            }, "1");
        }
        return view.createQuery();
    }

    public static void createMessage(Database database, String message) throws CouchbaseLiteException {
        Document doc = database.createDocument();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("message", message);
        properties.put("channels", "all");
        properties.put("type", TYPE);
        properties.put("created_at", new Date());
        doc.putProperties(properties);
    }

    public static Message from(Document document) {
        Message message = new Message(document.getDatabase());
        if (document.getProperty("message") != null) {
            message.setMessage((String) document.getProperty("message"));
        }
        if (document.getProperty("created_at") != null) {
            long createdAtL = 0;
            Object createdAt = document.getProperty("created_at");
            if (createdAt instanceof Double) {
                createdAtL = ((Double) createdAt).longValue();
            }
            if (createdAt instanceof Long) {
                createdAtL = (Long) createdAt;
            }


            message.setCreatedAt(new Date(createdAtL));
        }
        message.setSourceDocument(document);
        return message;
    }

    public Message(Database database) {
        this.createdAt = new Date();
        this.message = "";
        this.database = database;
    }

    public void save() throws CouchbaseLiteException {
        Map<String, Object> properties = new HashMap<String, Object>();
        Document document;
        if (sourceDocument == null) {
            document = database.createDocument();
        } else {
            document = sourceDocument;
            properties.putAll(sourceDocument.getProperties());
        }
        properties.put("type", TYPE);
        properties.put("created_at", createdAt.getTime());
        properties.put("message", message);
        try {
            document.putProperties(properties);
        } catch (CouchbaseLiteException e) {
            Log.e("MESSAGE", "Failed to save");
            throw e;
        }
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Document getSourceDocument() {
        return sourceDocument;
    }

    public void setSourceDocument(Document sourceDocument) {
        this.sourceDocument = sourceDocument;
    }
}
