package com.jsikmc15.fcmapptest;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;


/*
    메니페스트 파일에 반드시 등록할것 서비스니까!!
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {


    /*
        포그라운드 상태
        -  Message만 보내면됨 ->단 이때는 getData에서 찾아야됨

        - data를 안보내면 getData 는 당연히 null
        - 데이터가 있을 때 -> 이때는 알림메세지를 데이터 메세지에 넣어서 쓰자

        백그라운드 일때
        - message + token 과 같이보내야함 -> 노티는 알림메시지가 뜨고 데이터 메세지는 Activity로 전송
        노티피케이션으로 날려주면 onMessageReceived가 실행이 안된단다
출처: https://yamea-guide.tistory.com/entry/안드로이드-FCM-백그라운드에서-진동-오도록-하는-방법 [기타치는 개발자의 야매 가이드]

        - 데이터가 있을 때, 알림메세지는 노티로 , 데이터 메세지는 인텐트 부가정보로 전송하는식으로 구현하면 백과 프론트를 다잡을 수 있음
     */
    private NotificationManager notificationManager;

    public final static String TAG = "FORLOG";

    //메세지 받았을때 이벤트용
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        //super 빼면
        // 포그라운드 상태 - 푸쉬 알람 안옴
        super.onMessageReceived(remoteMessage);

        //직접 푸쉬를 보내기 위한 메세지
        Map<String,String> pushMessage = new HashMap<>();


        Log.i(TAG, "onMessageReceived: From - "+ remoteMessage.getFrom() );


        if(remoteMessage.getNotification() !=null){
            Log.i(TAG, "noti : title - " + remoteMessage.getNotification().getTitle() + " body - " +remoteMessage.getNotification().getBody());
            pushMessage.put("title",remoteMessage.getNotification().getTitle());
            pushMessage.put("body",remoteMessage.getNotification().getBody());
        }

        //데이터 메세지란 , firebase에서 keyValue 쌍의 선택사항임
        if(remoteMessage.getData()!=null && remoteMessage.getData().size()>0){
            Map<String,String> map = remoteMessage.getData();
            Set<String> keys = map.keySet();

            for(String key:keys){
                String value = map.get(key);
                Log.i(TAG, "onMessageReceived: " + String.format("데이터 메시지 : 키 %s , 값 %s",key,value));
                pushMessage.put(key,value);
            }
        }else{
            //알림만 있는 경우 데이터 메세지로 변경
            Log.i(TAG, "onMessageReceived: 응 데이터 없어~~ 알림만 있어 ~");


        }

        Log.i(TAG, "모든 푸쉬메세지");
        for(Map.Entry<String,String> entry : pushMessage.entrySet()){

            Log.i(TAG, String.format("데이터 메시지 : 키 %s , 값 %s",entry.getKey(),entry.getValue()));
        }

        if(remoteMessage.getData()!=null && remoteMessage.getData().size()>0){

        }


        //푸쉬메세지를 받으면 앱의 상태바에 notification을 띄우기
        showNotification(pushMessage);


    }



    // 푸쉬 메세지 도착시  상태바에 noti 표시 및 드래그 해서 MainActivity(디폴트는 런처로 가게됨) 가 아닌, 다른 액티비티로 전환
    //즉 흐름을 분기 하기 위한 것임
    private void showNotification(Map<String, String> pushMessage) {
        //서비스는 context를 상속받았으므로 this가 가능함
        Intent intent = new Intent(this,MessageActicity.class);

        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : pushMessage.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }

        intent.putExtra("data",bundle);

        //인텐트의 플래그 설정 - 화면전환 설정
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        //팬딩 인텐트로 설정
        PendingIntent pendingIntent = PendingIntent.getActivity(this,100,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = createNotificationBuilder();
        //InboxStyle을 Notification의 스타일로 사용하기위한
        //InboxStyle객체 생성
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setBigContentTitle(pushMessage.get("Title"));
        //내용은 addLine()으로 추가
        inboxStyle.addLine(pushMessage.get("body"));

        for(Map.Entry<String,String> entry : pushMessage.entrySet()){
            if(!(entry.getKey().equals("title")|| entry.getKey().equals("body"))){
                inboxStyle.addLine(entry.getValue());
            }
        }

        //InboxStyle를 빌더에 적용
        builder.setStyle(inboxStyle);
        builder.setContentIntent(pendingIntent);
        Notification notification= builder.build();
        //시스템 서비스로 NotificationManager객체 얻기
        notificationManager=(NotificationManager)getSystemService(this.NOTIFICATION_SERVICE);
        //오레오 부터 아래 코드 추가해야 함 시작
        int importance=NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel notificationChannel = new NotificationChannel("CHANNEL_ID","CHANNEL_NAME",importance);
        notificationChannel.enableLights(true);//스마트폰에 노티가 도착했을때 빛을 표시할지 안할지 설정
        notificationChannel.setLightColor(Color.RED);//위 true설정시 빛의 색상
        notificationChannel.enableVibration(true);//노티 도착시 진동 설정
        notificationChannel.setVibrationPattern(new long[]{100,200,300,400,500,400,300,200,100});//진동 시간(1000분의 1초)

        notificationManager.createNotificationChannel(notificationChannel);;
        notificationManager.notify(2,notification);
    }


    private NotificationCompat.Builder createNotificationBuilder(){
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher_round);
        return new NotificationCompat.Builder(this,"CHANNEL_ID")
                .setSmallIcon(android.R.drawable.ic_dialog_email)//노티 도착시 상태바에 표시되는 아이콘
                .setLargeIcon(largeIcon)
                .setContentTitle("한국 소프트웨어 인재개발원")//노티 드래그시 보이는 제목
                .setContentText("너무 졸려오요오")//노티 드래그시 보이는 내용
                .setTicker("KOSMO")//상태바에 표시되는 티커
                .setAutoCancel(true)//노티 드래그후 클릭시 상태바에서 자동으로 사라지도록 설정
                .setWhen(SystemClock.currentThreadTimeMillis())//노티 전달 시간
                .setDefaults(Notification.DEFAULT_VIBRATE)//노티시 알림 방법
                ;
    }

    //토큰 보낼떄
    //FCM에서 발행된 토큰임
    @Override
    public void onNewToken(@NonNull String token) {
//        super.onNewToken(s);

        Log.i(TAG, "onNewToken: " + token);
        sendNewTokenToMyServer(token);
    }

    //내가만든 웹 서비스와 연동하기 위한 코드
    private void sendNewTokenToMyServer(String token) {
        Log.i(TAG, "sendNewTokenToMyServer: ");
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(ScalarsConverterFactory.create())
                .baseUrl("http:192.168.0.18:8950")
                .build();
        //http:192.168.0.18:8950
        //localhost:8950/
        TokenService tokenService = retrofit.create(TokenService.class);
        Call<String> call = tokenService.postToken(token);
        Log.i(TAG, "전송시작!");

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {

                if(response.isSuccessful()){
                    Log.i(TAG, "200|"+response.body());


                }else{
                    Log.i(TAG, "ERROR"+response.code());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                t.printStackTrace();
            }
        });

    }

    private String GetDevicesUUID(Context mContext){
        String androidId = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);

        return androidId;
    }


}
