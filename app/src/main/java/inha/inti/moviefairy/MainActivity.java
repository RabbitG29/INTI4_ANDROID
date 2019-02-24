package inha.inti.moviefairy;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    long mNow;
    Date mDate;
    SimpleDateFormat mFormat = new SimpleDateFormat("yyyyMMdd");
    TextView datetest;
    ListView listview;
    String inDate;
    EditText inputDate;
    EditText search;
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
    private GPSInfo gps;
    ProgressBar progress;

    public class list_item {
        private String movies;
        private String code;

        public list_item(String movies, String code) {
            this.movies = movies;
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
    ArrayAdapter adapter;

    static final List<String> myList = new LinkedList<>();
    static final List<String> myList2 = new LinkedList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*----Button 정의-----*/
        Button locationButton= (Button) findViewById(R.id.locationButton);
        inputDate = (EditText)findViewById(R.id.inputDate);
        search = (EditText)findViewById(R.id.search);
        callPermission(); // GPS Permission 질의
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, myList) ;
        progress = (ProgressBar) findViewById(R.id.progressBar);
        listview = (ListView) findViewById(R.id.movies) ;
        listview.setAdapter(adapter) ;
        datetest = (TextView) findViewById(R.id.datetest);

        progress.setVisibility(View.INVISIBLE); // Progress bar가 일단 안보이게
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // input창에 검색어를 입력시 "addTextChangedListener" 이벤트 리스너를 정의한다.
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                // input창에 문자를 입력할때마다 호출된다.
                // search 메소드를 호출한다.
                String text = search.getText().toString();
                search(text);
            }
        });
        /*---위치 조회 눌렀을 경우 위경도 GPS로 받아오고 주소로 변환하기---*/
        locationButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                double latitude;
                double longitude;
                String inDate;
                if(inputDate.getText().toString().equals("") || inputDate.length() != 8){
                    Toast.makeText(MainActivity.this, "잘못 입력하여 오늘 날짜로 설정됩니다.", Toast.LENGTH_LONG).show();
                    inDate = getTime();
                } else {
                    inDate = inputDate.getText().toString();
                }
                progress.setVisibility(View.VISIBLE);
                myList2.clear();
                adapter.clear();
                String location="";
                // 권한 요청을 해야 함
                if (!isPermission) {
                    Log.e("button","?????????????????????");
                    callPermission();
                    return;
                }
                else {
                            GPSInfo gps = new GPSInfo(MainActivity.this);
                            // GPS 사용유무 가져오기
                            if (gps.isGetLocation()) {
                                // 위도, 경도 구하기
                                latitude = gps.getLatitude();
                                longitude = gps.getLongitude();
                                location = ""+latitude+","+longitude;
                                String address = getAddress(mcontext,latitude,longitude);
                                datetest.setText(address+"\n주변 영화관 상영시간표("+inDate+")");
                            }
                }
                    /*-----통신을 위한 AsyncTask-----*/
                    movieTask movietask = new movieTask(location,inDate);
                    movietask.execute();
                    Log.e("tag","execute");
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
        String date;
        public movieTask(String location, String date) {
            this.location = location;
            this.date = date;
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
                jsonObject.put("location", location); // JSON 생성
                jsonObject.put("date", date);
                Log.e("json", jsonObject.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            result = requestHttpURLConnection.request(url, jsonObject, "POST");
            Log.e("Async", "Async");
            return result;
        }
        /*----통신의 결과값을 이용----*/
        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
            Toast.makeText(mcontext, "정상적으로 통신이 완료되었습니다.", Toast.LENGTH_SHORT).show();
            progress.setVisibility(View.INVISIBLE);
            JSONArray jsonArray = new JSONArray();
            JSONArray jsonArray2 = new JSONArray();
            JSONArray jsonArray3 = new JSONArray();
            JSONArray jsonArray4 = new JSONArray();
            JSONObject movies = new JSONObject();
            JSONObject movies2 = new JSONObject();
            JSONObject movies3 = new JSONObject();
            JSONObject movies4 = new JSONObject();
            JSONObject movies5 = new JSONObject();
            String code="";
            try {
               jsonArray = result.getJSONArray("result"); // 전체 JSONArray 가져오기
            }catch (JSONException e) {

            }
            for(int i = 0 ; i<jsonArray.length(); i++){
                try {
                    movies = jsonArray.getJSONObject(i); // 영화관별로 가져오기
                } catch (JSONException e) {

                }
                Iterator ii = movies.keys();
                while (ii.hasNext()) {
                    String temp = ii.next().toString();
                    try {
                        movies2 = movies.getJSONObject(temp); //영화관별 JSONObject
                    } catch (JSONException e) {

                    }
                    try {
                        code = movies2.getString("TheaterCode"); // 영화관 코드
                        jsonArray2 = movies2.getJSONArray("timetable");
                    }catch (JSONException e) {

                    }
                    Log.e("movies3", jsonArray2.toString()); // 영화관별 JSONArray
                    for(int j=0;j<jsonArray2.length(); j++) {
                        try {
                            movies3 = jsonArray2.getJSONObject(j); // 영화관별 JSONObject
                        } catch(JSONException e) {

                        }
                        Iterator iii = movies3.keys();
                        while(iii.hasNext()) {
                            String temp2 = iii.next().toString(); // 영화명
                            try {
                                jsonArray3 = movies3.getJSONArray(temp2); // 영화별 JSONArray
                            } catch(JSONException e) {

                            }
                            //myList.add(temp+" "+temp2+" "+jsonArray3.toString());
                            for(int k=0;k<jsonArray3.length();k++) {
                                try {
                                    movies4 = jsonArray3.getJSONObject(k); // 관별 Object
                                }catch (JSONException e) {

                                }
                                Iterator iiii = movies4.keys();
                                while(iiii.hasNext()) {
                                    String temp3 = iiii.next().toString(); // 관명
                                    try {
                                        jsonArray4 = movies4.getJSONArray(temp3); // 관별 Array
                                    }catch(JSONException e) {

                                    }
                                    for(int l=0;l<jsonArray4.length();l++) {
                                        try {
                                            movies5 = jsonArray4.getJSONObject(l); // 마지막
                                            myList.add(temp+" <"+temp2+"> "+" "+temp3+"\n시작시간 : "+movies5.getString("startTime")
                                                    +"\n종료시간 : "+movies5.getString("endTime")+"\n여석 : "+movies5.getString("available") +"\n극장 코드 : "+code);
                                            } catch(JSONException e) {
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                adapter.addAll(myList);
                myList2.addAll(myList);
                listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Toast.makeText(getApplicationContext(), "웹 브라우저로 이동합니다.", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url2
                                +adapter.getItem(position).toString().substring(adapter.getItem(position).toString().length()-4)+"&t=T&ymd="
                                +inputDate.getText().toString()+"src=&src_name="));
                        startActivity(intent);
                    }
                });
            }
        }
    }
    /*---현재 날짜 가져오기---*/
    private String getTime(){
        mNow = System.currentTimeMillis();
        mDate = new Date(mNow);
        return mFormat.format(mDate);
    }
    /*---주소 읽어오기----*/
    public static String getAddress(Context mContext, double lat, double lng) {
        String nowAddress ="현재 위치를 확인 할 수 없습니다.";
        Geocoder geocoder = new Geocoder(mContext, Locale.KOREA);
        List<Address> address;
        try {
            if (geocoder != null) {
                //세번째 파라미터는 좌표에 대해 주소를 리턴 받는 갯수로
                //한좌표에 대해 두개이상의 이름이 존재할수있기에 주소배열을 리턴받기 위해 최대갯수 설정
                address = geocoder.getFromLocation(lat, lng, 1);

                if (address != null && address.size() > 0) {
                    // 주소 받아오기
                    String currentLocationAddress = address.get(0).getAddressLine(0).toString();
                    nowAddress  = currentLocationAddress;

                }
            }

        } catch (IOException e) {
            Toast.makeText(mContext, "주소를 가져 올 수 없습니다.", Toast.LENGTH_LONG).show();

            e.printStackTrace();
        }
        return nowAddress;
    }
    /*---검색하기---*/
    public void search(String charText) {
        //Log.e("list",myList2.toString());
        // 문자 입력시마다 리스트를 지우고 새로 뿌려준다.
        adapter.clear();
        // 문자 입력이 없을때는 모든 데이터를 보여준다.
        if (charText.length() == 0) {
            adapter.addAll(myList2);
        }
        // 문자 입력을 할때..
        else
        {
            // 리스트의 모든 데이터를 검색한다.
            for(int i = 0;i < myList2.size(); i++)
            {
                // arraylist의 모든 데이터에 입력받은 단어(charText)가 포함되어 있으면 true를 반환한다.
                if (myList2.get(i).toLowerCase().contains(charText))
                {
                    // 검색된 데이터를 리스트에 추가한다.
                    adapter.add(myList2.get(i));
                }
            }
        }
        // 리스트 데이터가 변경되었으므로 아답터를 갱신하여 검색된 데이터를 화면에 보여준다.
        adapter.notifyDataSetChanged();
    }
}
