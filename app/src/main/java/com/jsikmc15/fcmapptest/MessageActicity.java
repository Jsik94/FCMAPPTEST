package com.jsikmc15.fcmapptest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.Map;
import java.util.Set;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MessageActicity extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_layout);
        Log.i(MyFirebaseMessagingService.TAG, "onCreate: ");
        Intent intent =  getIntent();
        Bundle bundle = intent.getBundleExtra("data");

        //
//        ((TextView)findViewById(R.id.tv_Msg)).setText(String.format("제목 : %s\n내용 : %s이름 : $s\n ",bundle.getString("name")));

        StringBuffer sb = new StringBuffer();
        for(String key : bundle.keySet()){
            sb.append(key +" - " + bundle.getString(key)+"\n");
        }

        Log.i(MyFirebaseMessagingService.TAG, "번들있음?"+bundle.toString());
        Log.i(MyFirebaseMessagingService.TAG, "번들있음?"+sb.toString());
        ((TextView)findViewById(R.id.tv_Msg)).setText(sb.toString());
    }
}
