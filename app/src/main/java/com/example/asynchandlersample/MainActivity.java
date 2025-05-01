package com.example.asynchandlersample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity {
    private static final  String DEBUG_TAG="AsyncSample";
    private static final String WEATHERINFO_URL="https://api.openweathermap.org/data/2.5/weather?";
    private static final String APP_ID="097314cf5a4eae31100d411f1deb67b2";
    private List<Map<String,String>> _list;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _list = createList();

        ListView lvCityList=findViewById(R.id.lvCityList);
        String[] from={"name"};
        int[] to= {android.R.id.text1};
        SimpleAdapter adapter=new SimpleAdapter(MainActivity.this,_list, android.R.layout.simple_list_item_1,from,to);
        lvCityList.setAdapter(adapter);
        lvCityList.setOnItemClickListener(new ListItemClickListener());
        Log.d(DEBUG_TAG, "ListViewのクリックリスナーを設定しました"); // デバッグ用ログ
    }

    @UiThread
    private void addMsg(String msg){
        TextView tvWeatherDesc=findViewById(R.id.tvWeatherDesc);
        String msgNow=tvWeatherDesc.getText().toString();
        if (!msgNow.equals("")){
            msgNow+="\n";
        }
        msgNow+=msg;
        tvWeatherDesc.setText(msgNow);

    }

    private List<Map<String,String>> createList(){
        List<Map<String,String>> list=new ArrayList<>();
        Map<String,String> map=new HashMap<>();
        map.put("name","大阪");
        map.put("q","Osaka");
        list.add(map);
        map= new HashMap<>();
        map.put("name","神戸");
        map.put("q","Kobe");
        list.add(map);
        map= new HashMap<>();
        map.put("name","三重");
        map.put("q","Mie");
        list.add(map);
        return list;
    }
    @UiThread
    private void receiveWeatherInfo(final String urlFull){
        WeatherInfoBackgroundReciever backgroundReciever = new WeatherInfoBackgroundReciever(urlFull);
        ExecutorService executorService= Executors.newSingleThreadExecutor();
       executorService.submit(backgroundReciever);
    }
    private class WeatherInfoBackgroundReciever implements Runnable{

        private final String _urlFull;
        public WeatherInfoBackgroundReciever(String urlFull){
            _urlFull=urlFull;
        }

        @WorkerThread
        @Override
        public void run(){
            String result="";

            HttpURLConnection con=null;
            InputStream is=null;
            try {
                URL url=new URL(_urlFull);
                con=(HttpURLConnection) url.openConnection();
                con.setConnectTimeout(1000);
                con.setReadTimeout(1000);
                con.setRequestMethod("GET");
                con.connect();
                int statusCode = con.getResponseCode();
                Log.d(DEBUG_TAG, "HTTP Status Code: " + statusCode);
                is=con.getInputStream();
                result=is2String(is);
            }catch (MalformedURLException ex){
                Log.e(DEBUG_TAG,"URL変換失敗",ex);
            }catch (SocketTimeoutException ex) {
                Log.w(DEBUG_TAG, "通信失敗", ex);
            } catch (IOException ex) {  // 追加
                Log.e(DEBUG_TAG, "通信エラー", ex);
            }finally {
                if (con!=null){
                    con.disconnect();
                }
                if (is!=null){
                    try {
                        is.close();
                    }catch (IOException ex){
                        Log.e(DEBUG_TAG,"InputStream解放失敗",ex);
                    }
                }
            }

            return result;
        }
    }

    private class ListItemClickListener implements AdapterView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id){
            Map<String,String> item=_list.get(position);
            String q=item.get("q");
            String urlFull=WEATHERINFO_URL + "&q=" + q + "&appid="+APP_ID+"&lang=ja";

            Log.d(DEBUG_TAG, "リストアイテムがクリックされました: " + q);  // ★追加
            Log.d(DEBUG_TAG, "リクエストURL: " + urlFull);  // ★追加

            receiveWeatherInfo(urlFull);

        }

    }
    private String is2String(InputStream is)throws IOException{
        BufferedReader reader=new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuffer sb=new StringBuffer();
        char[] b= new char[1024];
        int line;
        while (0<=(line=reader.read(b))){
            sb.append(b,0,line);
        }
        return sb.toString();
    }

    @UiThread
    private void showWeatherInfo(String result){
        String cityName="";
        String weather="";
        String latitude="";
        String longitude="";
        try {
            JSONObject rootJSON=new JSONObject(result);
            cityName = rootJSON.getString("name");
            JSONObject coordJSON=rootJSON.getJSONObject("coord");
            latitude=coordJSON.getString("lat");
            longitude=coordJSON.getString("lon");
            Log.d(DEBUG_TAG, "お天気情報："+latitude+longitude);
            JSONArray weatherJSONArray=rootJSON.getJSONArray("weather");
            JSONObject weatherJSON=weatherJSONArray.getJSONObject(0);
            weather=weatherJSON.getString("description");
            Log.d(DEBUG_TAG, "お天気情報："+latitude+longitude+weather);  // ★追加
        }catch (JSONException ex){
            Log.e(DEBUG_TAG,"JSON解析失敗",ex);
        }


        String telop=cityName + "の天気";
        String desc ="現在は"+weather+"です。\n 緯度は" + latitude +"度で経度は"+ longitude+"です。";

        TextView tvWeatherTelop=findViewById(R.id.tvWeatherTelop);
        TextView tvWeatherDesc=findViewById(R.id.tvWeatherDesc);

        tvWeatherTelop.setText(telop);
        tvWeatherDesc.setText(desc);
    }
//git練習
}