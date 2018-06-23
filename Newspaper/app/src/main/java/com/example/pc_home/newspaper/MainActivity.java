package com.example.pc_home.newspaper;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> Content = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    ListView listView;

    SQLiteDatabase articleDB;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articleDB = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
        articleDB.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR )");




        DownloadTast task = new DownloadTast();
        try{
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }
        catch (Exception e){
            e.printStackTrace();
        }


        listView = (ListView)findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("content", Content.get(position));
                startActivity(intent);
            }
        });


        UpdateListView();
    }

    public void UpdateListView(){
        Cursor c = articleDB.rawQuery("SELECT * FROM articles",null);
        int contentIndex  = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if(c.moveToFirst()){
            titles.clear();
            Content.clear();

            do{
                titles.add(c.getString(titleIndex));
                Content.add(c.getString(contentIndex));
            }while(c.moveToNext());
            arrayAdapter.notifyDataSetChanged();
        }

    }

    public  class DownloadTast extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection=null;

            try{
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);

                int data = inputStream.read();

                while(data != -1){
                    char current = (char)data;
                    result += current;
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(result);
                int numberOfItems  =20;
                if(jsonArray.length() < numberOfItems){
                    numberOfItems = jsonArray.length();
                }

                articleDB.execSQL("DELETE FROM articles");

                for(int i=0; i<numberOfItems;i++){
                    String articlesIds = jsonArray.getString(i);
                    url= new URL("https://hacker-news.firebaseio.com/v0/item/"+articlesIds+".json?print=pretty");

                    urlConnection = (HttpURLConnection) url.openConnection();
                    inputStream = urlConnection.getInputStream();
                    reader = new InputStreamReader(inputStream);

                    data = inputStream.read();

                    String articleInfo ="";

                    while(data != -1){
                        char current = (char)data;
                        articleInfo += current;
                        data = reader.read();
                    }

                    JSONObject jsonObject = new JSONObject(articleInfo);

                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        String articletitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");

                        url = new URL(articleUrl);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        inputStream = urlConnection.getInputStream();
                        reader = new InputStreamReader(inputStream);

                        data = inputStream.read();
                        String articleContent = "";

                        while (data != -1){
                            char current = (char) data;
                            articleContent += current;
                            data = reader.read();
                        }

                        String sql = "INSERT INTO articles (articleId,title,content) VALUES (?,?,?)";
                        SQLiteStatement statement = articleDB.compileStatement(sql);
                        statement.bindString(1,articlesIds);
                        statement.bindString(2,articletitle);
                        statement.bindString(3,articleContent);

                        statement.execute();
                    }



                }

                return result;

            }
            catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            UpdateListView();
        }
    }
}
