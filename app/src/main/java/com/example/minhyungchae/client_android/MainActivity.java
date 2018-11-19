package com.example.minhyungchae.client_android;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.drm.DrmStore;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    // 서버 접속 여부를 판별하기 위한 변수
    boolean isConnect = false;
    EditText edit1;
    Button btn1;
    LinearLayout container;
    ScrollView scroll;
    ProgressDialog pro;
    // 어플 종료시 스레드 중지를 위해...
    boolean isRunning=false;
    // 서버와 연결되어있는 소켓 객체
    Socket member_socket;
    // 사용자 닉네임( 내 닉넴과 일치하면 내가보낸 말풍선으로 설정 아니면 반대설정)
    String user_nickname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edit1 = findViewById(R.id.editText);
        btn1 = findViewById(R.id.button);
        container=findViewById(R.id.container);
        scroll=findViewById(R.id.scroll);
    }

    // 버튼과 연결된 메소드
    public void btnMethod(View v) {
        if (isConnect == false) {   //접속전
            //사용자가 입력한 닉네임을 받는다.
            String nickName = edit1.getText().toString();
            if (nickName.length() > 0 && nickName != null) {
                //서버에 접속한다.
                pro = ProgressDialog.show(this, null, "접속중입니다");
                // 접속 스레드 가동
                ConnectionThread thread = new ConnectionThread();
                thread.start();

            }
            // 닉네임이 입력되지않을경우 다이얼로그창 띄운다.
            else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("닉네임을 입력해주세요");
                builder.setPositiveButton("확인", null);
                builder.show();
            }
        } else {                  // 접속 후
            // 입력한 문자열을 가져온다.
            String msg=edit1.getText().toString();
            // 송신 스레드 가동
            SendToServerThread thread=new SendToServerThread(member_socket,msg);
            thread.start();
        }
    }
    //end of btnMethod method

    // 서버접속 처리하는 스레드 클래스 - 안드로이드에서 네트워크 관련 동작은 항상
    // 메인스레드가 아닌 스레드에서 처리해야 한다.
    class ConnectionThread extends Thread {

        @Override
        public void run() {
            try {
                // 접속한다.
                final Socket socket = new Socket("192.168.56.1", 1024);
                member_socket=socket;
                // 미리 입력했던 닉네임을 서버로 전달한다.
                String nickName = edit1.getText().toString();
                user_nickname=nickName;     // 화자에 따라 말풍선을 바꿔주기위해
                // 스트림을 추출
                OutputStream os = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(os);
                // 닉네임을 송신한다.
                dos.writeUTF(nickName);
                // ProgressDialog 를 제거한다.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pro.dismiss();
                        edit1.setText("");
                        edit1.setHint("메세지 입력");
                        btn1.setText("전송");
                        // 접속 상태를 true로 셋팅한다.
                        isConnect=true;
                        // 메세지 수신을 위한 스레드 가동
                        isRunning=true;
                        MessageThread thread=new MessageThread(socket);
                        thread.start();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //end of ConnectonThread class



    class MessageThread extends Thread {
        Socket socket;
        DataInputStream dis;

        public MessageThread(Socket socket) {
            try {
                this.socket = socket;
                InputStream is = socket.getInputStream();
                dis = new DataInputStream(is);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try{
                while (isRunning){
                    // 서버로부터 데이터를 수신받는다.
                    final String msg=dis.readUTF();
                    // 화면에 출력
                    runOnUiThread(new Runnable() {
                        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
                        @Override
                        public void run() {
                            // 텍스트뷰의 객체를 생성
                            TextView tv=new TextView(MainActivity.this);
                            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,15);

                            // 메세지의 시작 이름이 내 닉네임과 일치한다면
                            if(msg.startsWith(user_nickname)){
                                //tv.setBackgroundResource(R.drawable.me);
                                tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                                tv.setTextColor(Color.RED);
                            }
                            else{
                                //tv.setBackgroundResource(R.drawable.you);
                                tv.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                                tv.setTextColor(Color.BLACK);
                            }

                            tv.setText(msg);
                            container.addView(tv);

                            // 제일 하단으로 스크롤 한다
                            scroll.fullScroll(View.FOCUS_DOWN);

                        }
                    });
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    //end of MessageThread


    // 서버에 데이터를 전달하는 스레드
    class SendToServerThread extends Thread{
        Socket socket;
        String msg;
        DataOutputStream dos;

        public SendToServerThread(Socket socket, String msg){
            try{
                this.socket=socket;
                this.msg=msg;
                OutputStream os=socket.getOutputStream();
                dos=new DataOutputStream(os);
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try{
                // 서버로 데이터를 보낸다.
                dos.writeUTF(msg);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        edit1.setText("");
                    }
                });
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    //end of SendToServerThread class


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            member_socket.close();
            isRunning=false;

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    //end of onDestroy method
}
