package com.saad.asaad.databaseassignment;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MainActivity extends Activity {
    SQLiteDatabase db;
    TextView OutputText;
    EditText InputText;

    private static final String QUERY_URL = "http://openlibrary.org/search.json?q=";
    private static final String IMAGE_URL_BASE = "http://covers.openlibrary.org/b/id/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = openOrCreateDatabase("booksDB.db", Context.MODE_PRIVATE, null);
        db.execSQL("DROP TABLE IF EXISTS book");
        db.execSQL("CREATE TABLE book (id INTEGER PRIMARY KEY AUTOINCREMENT, book_thumbnail VARCHAR, book_title VARCHAR, book_author VARCHAR)");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setFocusable(true);
        layout.setFocusableInTouchMode(true);

        InputText = new EditText(this);
        InputText.setHint("Book Title");
        InputText.setWidth(150);

        Button searchButton = new Button(this);
        searchButton.setText("Get Results");
        searchButton.setOnClickListener(searchButtonListener);

        OutputText = new TextView(this);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(OutputText);

        layout.addView(InputText);
        layout.addView(searchButton);
        layout.addView(scrollView);

        setContentView(layout);


    }

    View.OnClickListener searchButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            db.execSQL("DELETE FROM book");
            db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'book'");
            doQuery(InputText.getText().toString());
        }
    };


    private void doQuery(String searchString) {

        String urlString = "";



        try {
            urlString = URLEncoder.encode(searchString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        AsyncHttpClient client = new AsyncHttpClient();

        client.get(QUERY_URL + urlString,
                new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(JSONObject jsonObject) {
                        JSONArray dataArray = jsonObject.optJSONArray("docs");
                        for (int i = 0; i < dataArray.length(); i++) {

                            JSONObject dataObject = null;

                            String book_thumbnail = null, book_title = null, book_author = null;
                            try {
                                dataObject = dataArray.getJSONObject(i);
                                if (dataObject.has("cover_i")) {
                                    String imageID = dataObject.optString("cover_i");
                                    book_thumbnail = IMAGE_URL_BASE + imageID + "-S.jpg";
                                }

                                if (dataObject.has("title")) {
                                    book_title = dataObject.getString("title");
                                }
                                if (dataObject.has("author_name")) {
                                    book_author = dataObject.getString("author_name");
                                }
                                db.execSQL("INSERT INTO book VALUES ( NULL, ?, ?, ?)",
                                        new Object[]{
                                                book_thumbnail,
                                                book_title,
                                                book_author,
                                        }
                                );


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                        Cursor resultSet = db.rawQuery("Select * from book", null);
                        int rowCount = resultSet.getCount();
                        resultSet.moveToFirst();

                        String outputString = "";
                        for (int i = 0; i < rowCount; i++) {
                            outputString += resultSet.getInt(0) +" | "+ resultSet.getString(1) +" | "+ resultSet.getString(2) +" | "+ resultSet.getString(3)+ "\n";
                            resultSet.moveToNext();
                        }
                        OutputText.setText(outputString);

                    }

                    @Override
                    public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                        Log.e("Query Failed", statusCode + " " + throwable.getMessage());
                    }
                });
    }
}
