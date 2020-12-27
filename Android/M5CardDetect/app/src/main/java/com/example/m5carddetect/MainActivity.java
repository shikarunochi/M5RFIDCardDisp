//NFC読み込み
//https://qiita.com/zaburo/items/6a34dfd8f87d7ffba56a

//http通信
//https://qiita.com/f-paico/items/f03f48d3ecacba7dcc13

package com.example.m5carddetect;

import androidx.appcompat.app.AppCompatActivity;

import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Formatter;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TextView txt01;
    Button btn01;
    Button btn02;

    EditText txtIpAddress;

    //NfcAdapterを初期化
    NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //UIのマーツをマッピング
        txt01 = findViewById(R.id.txt01);
        btn01 = findViewById(R.id.btn01);
        btn02 = findViewById(R.id.btn02);
        txtIpAddress = findViewById(R.id.ipAddress);

        //nfcAdapter初期化
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        //Reader Mode Offボタンのenabledをfalseに（トグルにするため）
        btn02.setEnabled(false);

        btn01.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //トグル機能
                btn01.setEnabled(false);
                btn02.setEnabled(true);

                //Redermode On
               // nfcAdapter.enableReaderMode(MainActivity.this,new MyReaderCallback(),NfcAdapter.FLAG_READER_NFC_F,null);
                nfcAdapter.enableReaderMode(MainActivity.this,new MyReaderCallback(),NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,null);

            }
        });

        btn02.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //トグル機能
                btn01.setEnabled(true);
                btn02.setEnabled(false);

                //Readermode Off
                nfcAdapter.disableReaderMode(MainActivity.this);

                //表示初期化
                txt01.setText("Read ID ...");
            }
        });
    }

    //Callback Class
    private class MyReaderCallback implements NfcAdapter.ReaderCallback{
        @Override
        public void onTagDiscovered(Tag tag){

            Log.d("Hoge","Tag discoverd.");

            //get idm
            byte[] idm = tag.getId();
            final String idmString = bytesToHexString(idm);


            //親スレッドで画面描画
            final Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(new Runnable() {
               @Override
                public void run() {
                   txt01.setText(idmString);
              }
            });

            //取得したStringをhttp送信
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String response = "";
                    String  ipAddress = txtIpAddress.getText().toString();;
                    response = getAPI("http://" + ipAddress + "/m5card?id=" + idmString);
                }
            });
            thread.start();
        }
    }

    //bytes列を16進数文字列に変換
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        Formatter formatter = new Formatter(sb);
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return sb.toString().toUpperCase(Locale.getDefault());
    }
    public String getAPI(String URLString){
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        String result = "";
        String str = "";
        try {
            URL url = new URL(URLString);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);
            urlConnection.addRequestProperty("User-Agent", "Android");
            urlConnection.addRequestProperty("Accept-Language", Locale.getDefault().toString());
            urlConnection.setRequestMethod("GET");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(false);
            urlConnection.connect();
            int statusCode = urlConnection.getResponseCode();
            if (statusCode == 200){
                inputStream = urlConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"));
                result = bufferedReader.readLine();
                while (result != null){
                    str += result;
                    result = bufferedReader.readLine();
                }
                bufferedReader.close();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }

}