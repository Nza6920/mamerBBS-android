package com.example.my.mamer;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.my.mamer.bean.User;
import com.example.my.mamer.util.BaseUtils;
import com.example.my.mamer.util.HttpUtil;
import com.example.my.mamer.util.LoadingDraw;
import com.example.my.mamer.util.StringToDate;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.my.mamer.MyApplication.getContext;
import static com.example.my.mamer.config.Config.DISMISS_DIALOG;
import static com.example.my.mamer.config.Config.HTTP_OK;
import static com.example.my.mamer.config.Config.HTTP_OVERNUM;
import static com.example.my.mamer.config.Config.HTTP_USER_ERROR;
import static com.example.my.mamer.config.Config.HTTP_USER_GET_INFORMATION;
import static com.example.my.mamer.config.Config.HTTP_USER_NULL;
import static com.example.my.mamer.config.Config.JSON;
import static com.example.my.mamer.config.Config.MESSAGE_ERROR;
import static com.example.my.mamer.config.Config.PHONE_NUMBER;
import static com.example.my.mamer.config.Config.PIC_CODE;
import static com.example.my.mamer.config.Config.REQUEST_OK;
import static com.example.my.mamer.config.Config.REQUEST_OK_SMSLOGIN;
import static com.example.my.mamer.config.Config.SET_TEXTVIEW;
import static com.example.my.mamer.config.Config.SMS_LOGIN;
import static com.example.my.mamer.config.Config.USER_INFORMATION;
import static com.example.my.mamer.config.Config.USER_SET_INFORMATION;

public class SMSVerifyActivity extends AppCompatActivity {

    private LoadingDraw loadingDraw;
    private EditText etPNum;
    private RelativeLayout SMSpiclayout;
    private TextView tvPicCodeRefresh;
    private ImageView picImg;
    private EditText picNumEdit;
    private RelativeLayout SMSnumlayout;
    private EditText smsNumEdit;
    private String picCode;
    private String smsNum;
    private Button picCodeBtn;
    private Button smsNumBtn;
    private Button loginBtn;
    private TextView tvClose;
    private String picCodeKey;
    private String smsCodeKey;
    private SharedPreferences prefs;
    private String pNum;
    private SharedPreferences.Editor editor;


    private final Handler msgHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case DISMISS_DIALOG:
                    ((LoadingDraw)msg.obj).dismiss();
                    break;
                case MESSAGE_ERROR:
                    Toast.makeText(SMSVerifyActivity.this,(String)msg.obj,Toast.LENGTH_SHORT).show();
                    break;
                case HTTP_USER_ERROR:
                    Toast.makeText(SMSVerifyActivity.this,(String)msg.obj,Toast.LENGTH_SHORT).show();
                    break;
                case HTTP_USER_NULL:
                    Toast.makeText(SMSVerifyActivity.this,(String)msg.obj,Toast.LENGTH_SHORT).show();
                    break;
                case SET_TEXTVIEW:
                    JSONObject jresp = null;
                    try {
                        jresp = new JSONObject((String)msg.obj);
                        picCodeKey=jresp.getString("captcha_key");
                        RequestOptions options=new RequestOptions()
                                .error(R.mipmap.ic_image_error)
                                .placeholder(R.mipmap.ic_image_error);
                        Glide.with(getContext())
                                .asBitmap()
                                .load( loadPicCodeImg(jresp.getString("captcha_image_content")))
                                .apply(options)
                                .into(picImg);
                        SMSpiclayout.setVisibility(View.VISIBLE);
                        picCodeBtn.setVisibility(View.GONE);
                        smsNumBtn.setVisibility(View.VISIBLE);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                  break;
                case REQUEST_OK:
                    JSONObject resp = null;
                    try {
                        resp = new JSONObject((String)msg.obj);
                        smsCodeKey=resp.getString("key");
                        SMSpiclayout.setVisibility(View.GONE);
                        SMSnumlayout.setVisibility(View.VISIBLE);
                        smsNumBtn.setVisibility(View.GONE);
                        loginBtn.setVisibility(View.VISIBLE);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case REQUEST_OK_SMSLOGIN:
                    JSONObject jrespSMS = null;
                    try {
                        jrespSMS = new JSONObject((String)msg.obj);
                        String key=jrespSMS.getString("access_token");
                        String type=jrespSMS.getString("token_type");
                        MyApplication.globalUserInfo.token=key;
                        MyApplication.globalUserInfo.tokenType=type;
                        editor=PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
                        editor.putString("key",key);
                        editor.putString("type",type);
                        editor.putBoolean("loginFlag",true);
                        editor.apply();
                        loginSuccess();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case USER_SET_INFORMATION:
                    JSONObject jrespInfo = null;
                    JSONArray jsonArray=null;
                    try {
                        jrespInfo = new JSONObject((String)msg.obj);
                        User user=new User();
                        MyApplication.globalUserInfo.user=user;
                        MyApplication.globalUserInfo.user.setUserId(jrespInfo.getString("id"));
                        MyApplication.globalUserInfo.user.setUserName(jrespInfo.getString("name"));
                        MyApplication.globalUserInfo.user.setUserEmail(jrespInfo.getString("email"));
                        MyApplication.globalUserInfo.user.setUserImg(jrespInfo.getString("avatar"));
                        MyApplication.globalUserInfo.user.setAvatarType(BaseUtils.reverseString(jrespInfo.getString("avatar")));
                        String introduction=jrespInfo.getString("introduction");
                        if (introduction.equals("null")){
                            MyApplication.globalUserInfo.user.setUserIntroduction("");
                        }else {
                            MyApplication.globalUserInfo.user.setUserIntroduction(introduction);
                        }
                        //                            是否绑定
                        MyApplication.globalUserInfo.user.setBoundPhone(jrespInfo.getBoolean("bound_phone"));
                        //                            是否验证邮箱
                        MyApplication.globalUserInfo.user.setEmail_verified(jrespInfo.getBoolean("email_verified"));
                        //                            注册
                        MyApplication.globalUserInfo.user.setUserBornDate(StringToDate.stringToShort(jrespInfo.getString("created_at")));

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smsverify);
        loadingDraw =new LoadingDraw(this);

        init();
    }
    private void init(){
        etPNum=findViewById(R.id.sms_p);
        SMSpiclayout=findViewById(R.id.sms_pic);
        picImg=findViewById(R.id.sms_pic_img);
        picNumEdit=findViewById(R.id.sms_pic_num);
        SMSnumlayout=findViewById(R.id.sms);
        smsNumEdit=findViewById(R.id.sms_num);
        picCodeBtn=findViewById(R.id.sms_pic_btn);
        smsNumBtn=findViewById(R.id.sms_pic_code);
        loginBtn=findViewById(R.id.sms_login_btn);
        tvClose=findViewById(R.id.title_tv_close);
        tvPicCodeRefresh=findViewById(R.id.sms_pic_refresh);

        Drawable tvClosePic=ContextCompat.getDrawable(this,R.mipmap.ic_title_close);
        tvClose.setBackground(tvClosePic);


        tvClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        picCodeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                发送图片验证请求
                postPicCode();
            }
        });
        smsNumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                发送短信验证码请求
                postSMSCode();
            }
        });
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                短信登录
                postSMSLogin();
            }
        });
        tvPicCodeRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                重新请求验证码
                postPicCode();
            }
        });

    }
    //获取图片验证码
    private void postPicCode(){

        JSONObject jsonParam=new JSONObject();
        pNum=etPNum.getText().toString().trim();
        try {
            jsonParam.put("phone",pNum);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody requestBody=RequestBody.create(JSON,jsonParam.toString());
        HttpUtil.sendOkHttpRequest(PHONE_NUMBER, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Message msg1=new Message();
                msg1.what=DISMISS_DIALOG;
                msg1.obj=loadingDraw;
                msgHandler.sendMessage(msg1);

                Message msg2=new Message();
                msg2.what=MESSAGE_ERROR;
                msg2.obj="服务器异常,请检查网络";
                msgHandler.sendMessage(msg2);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Message msg=new Message();
                msg.what=DISMISS_DIALOG;
                msg.obj=loadingDraw;
                msgHandler.sendMessage(msg);

                try {
                    JSONObject jresp=new JSONObject(response.body().string());

                    switch (response.code()){
                        case HTTP_OK:


                            Message msg1 = new Message();
                            msg1.what = SET_TEXTVIEW;
                            msg1.obj = jresp.toString();
                            msgHandler.sendMessage(msg1);
                            break;
                        case HTTP_USER_NULL:
                            Message msg3=new Message();
                            msg3.what=HTTP_USER_NULL;
                            msg3.obj=jresp.getJSONObject("errors").getString("phone");
                            msgHandler.sendMessage(msg3);
                            break;
                        default:
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    //    加载验证图片
    private Bitmap loadPicCodeImg(String bicCodes){

        Bitmap bitmap=null;
        try {
            byte[] bitmapArray=Base64.decode(bicCodes.split(",")[1],Base64.DEFAULT);
            bitmap=BitmapFactory.decodeByteArray(bitmapArray,0,bitmapArray.length);
        }catch (Exception e){
            e.printStackTrace();
        }
        return bitmap;
    }
//    获取短信验证码
    private void postSMSCode(){
        loadingDraw.show();
        JSONObject jsonParam=new JSONObject();
        picCode=picNumEdit.getText().toString().trim();
        try {
            jsonParam.put("captcha_key",picCodeKey);
            jsonParam.put("captcha_code",picCode);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        RequestBody requestBody=RequestBody.create(JSON,jsonParam.toString());
        HttpUtil.sendOkHttpRequest(PIC_CODE, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Message msg1=new Message();
                msg1.what=DISMISS_DIALOG;
                msg1.obj=loadingDraw;
                msgHandler.sendMessage(msg1);

                Message msg2=new Message();
                msg2.what=MESSAGE_ERROR;
                msg2.obj="服务器异常,请检查网络";
                msgHandler.sendMessage(msg2);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Message msg=new Message();
                msg.what=DISMISS_DIALOG;
                msg.obj=loadingDraw;
                msgHandler.sendMessage(msg);
                try {
                    JSONObject jresp=new JSONObject(response.body().string());

                    switch (response.code()){
                        case HTTP_OK:
                            Message msg1 = new Message();
                            msg1.what = REQUEST_OK;
                            msg1.obj = jresp.toString();
                            msgHandler.sendMessage(msg1);
                          break;
                        case HTTP_USER_ERROR:
                            Message msg5=new Message();
                            msg5.what=response.code();
                            msg5.obj=jresp.getString("message");
                            msgHandler.sendMessage(msg5);
                            break;
                        case HTTP_USER_NULL:
                            Message msg2=new Message();
                            msg2.what=response.code();
                            msg2.obj=jresp.getString("message");
                            msgHandler.sendMessage(msg2);
                            break;
                        case HTTP_OVERNUM:
                            Message msg7=new Message();
                            msg7.what=response.code();
                            msg7.obj="请求次数过多,请稍后再试";
                            msgHandler.sendMessage(msg7);
                        default:
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
//    登录
    private void postSMSLogin(){
    loadingDraw.show();
    JSONObject jsonParam=new JSONObject();
    smsNum=smsNumEdit.getText().toString().trim();
    try {
        jsonParam.put("verification_key",smsCodeKey);
        jsonParam.put("verification_code",smsNum);
        jsonParam.put("phone",pNum);
    } catch (JSONException e) {
        e.printStackTrace();
    }
    RequestBody requestBody=RequestBody.create(JSON,jsonParam.toString());
    HttpUtil.sendOkHttpRequest(SMS_LOGIN, requestBody, new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            Message msg1=new Message();
            msg1.what=DISMISS_DIALOG;
            msg1.obj=loadingDraw;
            msgHandler.sendMessage(msg1);

            Message msg2=new Message();
            msg2.what=MESSAGE_ERROR;
            msg2.obj="服务器异常,请检查网络";
            msgHandler.sendMessage(msg2);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            Message msg=new Message();
            msg.what=DISMISS_DIALOG;
            msg.obj=loadingDraw;
            msgHandler.sendMessage(msg);
            try {
                JSONObject jresp=new JSONObject(response.body().string());

                switch (response.code()){
                    case HTTP_OK:
                        Message msg1 = new Message();
                        msg1.what = REQUEST_OK_SMSLOGIN;
                        msg1.obj = jresp.toString();
                        msgHandler.sendMessage(msg1);
//登录成功获取用户信息

                        break;
                    case HTTP_USER_ERROR:
                        Message msg5=new Message();
                        msg5.what=response.code();
                        msg5.obj=jresp.getString("message");
                        msgHandler.sendMessage(msg5);
                        break;
                    case HTTP_USER_NULL:
                        Message msg2=new Message();
                        msg2.what=response.code();
                        msg2.obj=jresp.getString("message");
                        msgHandler.sendMessage(msg2);
                        break;
                    case HTTP_OVERNUM:
                        Message msg7=new Message();
                        msg7.what=response.code();
                        msg7.obj="请求次数过多,请稍后再试";
                        msgHandler.sendMessage(msg7);
                    default:
                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    });
}

    //    获取当前用户信息
    private void loginSuccess()  {
        loadingDraw.show();
        HttpUtil.sendOkHttpRequestGet(USER_INFORMATION, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Message msg1=new Message();
                msg1.what=DISMISS_DIALOG;
                msg1.obj=loadingDraw;
                msgHandler.sendMessage(msg1);

                Message msg2=new Message();
                msg2.what=MESSAGE_ERROR;
                msg2.obj="服务器异常,请检查网络";
                msgHandler.sendMessage(msg2);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Message msg=new Message();
                msg.what=DISMISS_DIALOG;
                msg.obj=loadingDraw;
                msgHandler.sendMessage(msg);

                JSONObject jresp = null;
                JSONArray jsonArray=null;
                try {
                    jresp=new JSONObject(response.body().string());
                    switch (response.code()){
                        case HTTP_USER_GET_INFORMATION:
                            Message msg1 = new Message();
                            msg1.what = USER_SET_INFORMATION;
                            msg1.obj = jresp.toString();
                            msgHandler.sendMessage(msg1);
                            //获取用户信息成功后返回首页

                            finish();
                            break;
                        //                            令牌失效，重新请求
                        case HTTP_USER_ERROR:

                            break;
                        default:
                            break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
