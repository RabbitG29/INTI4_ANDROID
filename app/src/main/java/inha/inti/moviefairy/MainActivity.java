package inha.inti.moviefairy;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    long mNow;
    Date mDate;
    SimpleDateFormat mFormat = new SimpleDateFormat("yyyyMMdd");
    TextView datetest;
    private long lastTimeBackPressed;
    Context mcontext = this;
    final String url = "http://172.20.10.6:8080/theaters";
    final String url2 = "http://m.cgv.co.kr/Schedule/?tc=";
    //GPS를 위한 변수들
    private final int PERMISSIONS_ACCESS_FINE_LOCATION = 1000;
    private final int PERMISSIONS_ACCESS_COARSE_LOCATION = 1001;
    private boolean isAccessFineLocation = false;
    private boolean isAccessCoarseLocation = false;
    private boolean isPermission = false;
    ProgressBar progress;
    ArrayAdapter adapter;

    static final List<String> myList = new LinkedList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*----Button 정의-----*/
        Button locationButton= (Button) findViewById(R.id.locationButton);
        Button CGVButton = (Button) findViewById(R.id.CGVButton);
        callPermission(); // GPS Permission 질의
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, myList) ;
        progress = (ProgressBar) findViewById(R.id.progressBar);
        ListView listview = (ListView) findViewById(R.id.movies) ;
        listview.setAdapter(adapter) ;
        datetest = (TextView) findViewById(R.id.datetest);
        datetest.setText(getTime());

        progress.setVisibility(View.INVISIBLE); // Progress bar가 일단 안보이게
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        /*---위치 조회 눌렀을 경우 위경도 GPS로 받아오고 주소로 변환하기---*/
        locationButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                progress.setVisibility(View.VISIBLE);
                String location="";
                // 권한 요청을 해야 함
                if (!isPermission) {
                    Log.e("button","?????????????????????");
                    callPermission();
                    return;
                }
                else {
                    Log.e("button","????");
                    /*-----통신을 위한 AsyncTask-----*/
                    movieTask movietask = new movieTask("인천광역시");
                    movietask.execute();
                    Log.e("tag","execute");
                }
            }
        });
        /*---CGv 눌렀을 경우 해당 회차로 이동하기(임시)---*/
        CGVButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "웹 브라우저로 이동합니다.",
                        Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url2+"0198"+"&t=T&ymd="+getTime()+"src=&src_name="));
                //0198이 영화관코드, 20190224가 해당하는 상영일자
                startActivity(intent);
            }
        });
    } // onCreate 끝
    /* 뒤로가기 두번 누르면 종료 */
    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - lastTimeBackPressed < 1500) {
            finish();
            return;
        }
        Toast.makeText(this, "'뒤로' 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
        lastTimeBackPressed = System.currentTimeMillis();
    }

    /*----GPS 권한이 있는지 확인----*/
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_ACCESS_FINE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            isAccessFineLocation = true;

        } else if (requestCode == PERMISSIONS_ACCESS_COARSE_LOCATION
                && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            isAccessCoarseLocation = true;
        }

        if (isAccessFineLocation && isAccessCoarseLocation) {
            isPermission = true;
        }
    }

    /*-----GPS 권한 요청----*/
    private void callPermission() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_ACCESS_FINE_LOCATION);

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){

            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_ACCESS_COARSE_LOCATION);
        } else {
            isPermission = true;
        }
    }

    /*-----주소를 보내고 영화 시간표를 받아오는 AsyncTask-----*/
    // TODO : 서버로 request 보내고 받아온 response 정리하기 + 생성자 처리하기
    class movieTask extends AsyncTask<String, Integer, JSONObject> {
        String location;
        String text;
        public movieTask(String location) {
            this.location = location;
        }
        /*----전처리----*/
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        /*-----JSON화 후 통신-------*/
        @Override
        protected JSONObject doInBackground(String... params) {
            Log.e("url", url);
            JSONObject result;
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("uuid", "hi"); // JSON 생성
            } catch (JSONException e) {
                e.printStackTrace();
            }
            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            result = requestHttpURLConnection.request(url, jsonObject, "POST");
            Log.e("Async", "Async");
            //Log.e("result",result.toString());
            return result;
        }
        /*----통신의 결과값을 이용----*/
        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
            Toast.makeText(mcontext, "정상적으로 통신이 완료되었습니다.", Toast.LENGTH_SHORT).show();
            progress.setVisibility(View.INVISIBLE);
            try {
                //text = "영화관 : "+result.getString("name") + "\n 코드 : " + result.getString("code") + "\n 영화제목 : " + result.getString("movie")
                       // + "\n 상영관 : " + result.getString("theater") + "\n 시작시간 : " + result.getString("starttime") + "\n 종료시간 : " + result.getString("endtime");
                //datetest.setText(result.toString());
                myList.add(result.toString());
                myList.add(result.keys().toString());
                adapter.addAll(myList);
            } catch(NullPointerException e) {
                Toast.makeText(mcontext, "NULL입니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    /*---현재 날짜 가져오기---*/
    private String getTime(){
        mNow = System.currentTimeMillis();
        mDate = new Date(mNow);
        return mFormat.format(mDate);
    }
}
