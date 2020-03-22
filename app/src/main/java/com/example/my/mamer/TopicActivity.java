package com.example.my.mamer;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chinalwb.are.AREditText;
import com.chinalwb.are.styles.toolbar.IARE_Toolbar;
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_AlignmentCenter;
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_AlignmentLeft;
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_AlignmentRight;
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_At;
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Bold;
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Hr;
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Image;
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Italic;
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Link;
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_ListBullet;
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_ListNumber;
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Quote;
import com.chinalwb.are.styles.toolitems.ARE_ToolItem_Underline;
import com.chinalwb.are.styles.toolitems.IARE_ToolItem;
import com.example.my.mamer.bean.TopicDIvid;
import com.example.my.mamer.util.HttpUtil;
import com.example.my.mamer.util.LoadingDraw;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

import static com.example.my.mamer.config.Config.DISMISS_DIALOG;
import static com.example.my.mamer.config.Config.HTTP_OK;
import static com.example.my.mamer.config.Config.HTTP_USER_ERROR;
import static com.example.my.mamer.config.Config.HTTP_USER_FORMAT_ERROR;
import static com.example.my.mamer.config.Config.HTTP_USER_GET_INFORMATION;
import static com.example.my.mamer.config.Config.HTTP_USER_NULL;
import static com.example.my.mamer.config.Config.MEDIA_TYPE_IMAGE;
import static com.example.my.mamer.config.Config.MESSAGE_ERROR;
import static com.example.my.mamer.config.Config.SHOW_DIALOG;
import static com.example.my.mamer.config.Config.USER_AVATAR_IMG;
import static com.example.my.mamer.config.Config.USER_SET_INFORMATION;

//该activity用于编辑文章和新建文章
public class TopicActivity extends AppCompatActivity implements View.OnClickListener{

//    头部栏
    private TextView tvClose;
    private TextView tvTitle;
    private Button btnNext;
//    标题
    private EditText editTitle;
    private String strTopicTitle;
//    标签
    private LinearLayout layoutFlag;
    private LinearLayout layoutFlagT;
    private TextView tvFlag;
    private Object flagStr;
    private TextView tvFlagClose;
//    底部工具栏
    private IARE_Toolbar mToolbar;
    private LinearLayout layoutBottom;
//    富文本
    private AREditText mEditText;
//    键盘的收起与弹出
    private TextView tvKeyboardDown;

    ArrayList arrayList=new ArrayList();
    LoadingDraw loadingDraw;

    private final Handler msgHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case USER_SET_INFORMATION:
//                    显示标签
                    layoutFlag.setVisibility(View.GONE);
                    layoutFlagT.setVisibility(View.VISIBLE);
                    tvFlag.setText("#"+msg.obj+"#");
                    break;
                case MESSAGE_ERROR:
                    Toast.makeText(TopicActivity.this,(String)msg.obj,Toast.LENGTH_LONG).show();
                    break;
                case DISMISS_DIALOG:
                    loadingDraw.dismiss();
                    break;
                case SHOW_DIALOG:
                    flagDialog((ArrayList) msg.obj);
                    break;
                case HTTP_OK:
                    Toast.makeText(TopicActivity.this,(String)msg.obj,Toast.LENGTH_LONG).show();
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
        setContentView(R.layout.activity_topic);
        loadingDraw=new LoadingDraw(this);
        initTitle();
        initToolbar();
    }
//初始化title
    @SuppressLint("ClickableViewAccessibility")
    private void initTitle(){
        tvClose=findViewById(R.id.title_tv_close);
        tvTitle=findViewById(R.id.title_tv_name);
        btnNext=findViewById(R.id.title_btn_next);
        editTitle=findViewById(R.id.topic_title_);
        layoutFlag=findViewById(R.id.topic_flag_);
        layoutFlagT=findViewById(R.id.layout_topic_flag);
        tvFlag=findViewById(R.id.topic_flag_t);
        tvFlagClose=findViewById(R.id.topic_flag_t_close);
        layoutBottom=findViewById(R.id.topic_bottombar);
        tvKeyboardDown=findViewById(R.id.topic_keybaord_down);

        Drawable tvClosePic=ContextCompat.getDrawable(this,R.mipmap.ic_title_close);
        tvClose.setBackground(tvClosePic);
        tvClose.setOnClickListener(this);
        tvTitle.setText("撰写文章");
        btnNext.setText("发布");
        editTitle.addTextChangedListener(etWatcher);
        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        editTitle.requestFocus();
                    }
                }, 3);
//        添加监听,标题不可用富文本样式
        editTitle.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus){
                    layoutBottom.setVisibility(View.VISIBLE);
                }else {
                    layoutBottom.setVisibility(View.GONE);
                }
            }
        });
        btnNext.setTextColor(getResources().getColor(R.color.colorPop));
        btnNext.setOnClickListener(this);
        layoutFlag.setOnClickListener(this);
        tvFlagClose.setOnClickListener(this);
        tvKeyboardDown.setOnClickListener(this);

    }

    private void initToolbar() {
        mToolbar = this.findViewById(R.id.topic_areToolbar_);
//        加粗
        IARE_ToolItem bold = new ARE_ToolItem_Bold();
//        斜体
        IARE_ToolItem italic = new ARE_ToolItem_Italic();
//        下划线
        IARE_ToolItem underline = new ARE_ToolItem_Underline();
//        “
        IARE_ToolItem quote = new ARE_ToolItem_Quote();
//        列表123
        IARE_ToolItem listNumber = new ARE_ToolItem_ListNumber();
//        列表abc
        IARE_ToolItem listBullet = new ARE_ToolItem_ListBullet();
//        分割线
        IARE_ToolItem hr = new ARE_ToolItem_Hr();
//        链接
        IARE_ToolItem link = new ARE_ToolItem_Link();
//        左对齐
        IARE_ToolItem left = new ARE_ToolItem_AlignmentLeft();
//        居中
        IARE_ToolItem center = new ARE_ToolItem_AlignmentCenter();
//        右对齐
        IARE_ToolItem right = new ARE_ToolItem_AlignmentRight();
//        图片
        IARE_ToolItem image = new ARE_ToolItem_Image();
//        @
        IARE_ToolItem at = new ARE_ToolItem_At();
        mToolbar.addToolbarItem(bold);
        mToolbar.addToolbarItem(italic);
        mToolbar.addToolbarItem(underline);
        mToolbar.addToolbarItem(quote);
        mToolbar.addToolbarItem(listNumber);
        mToolbar.addToolbarItem(listBullet);
        mToolbar.addToolbarItem(hr);
        mToolbar.addToolbarItem(link);
        mToolbar.addToolbarItem(left);
        mToolbar.addToolbarItem(center);
        mToolbar.addToolbarItem(right);
        mToolbar.addToolbarItem(image);
        mToolbar.addToolbarItem(at);

        mEditText = this.findViewById(R.id.topic_arEditText_);
        mEditText.setToolbar(mToolbar);
    }

    private void setHtml() {
        mEditText.getImageStrategy();
//        转为html
        String m=mEditText.getHtml();
        Log.e("html:",m);
//        将html展示出来
        mEditText.fromHtml(m);
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int menuId = item.getItemId();
//        if (menuId == com.chinalwb.are.R.id.action_save) {
//            String html = this.mEditText.getHtml();
//            Util.saveHtml(this, html);
//            return true;
//        }
//        if (menuId == R.id.action_show_tv) {
//            String html = this.mEditText.getHtml();
//            Intent intent = new Intent(this, TextViewActivity.class);
//            intent.putExtra(HTML_TEXT, html);
//            startActivity(intent);
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Uri uri=data.getData();
        Log.e("html", String.valueOf(uri));
//        判断图片的大小是否符合上传条件
//        得到返回后，先处理图片，然后上传图片，上传失败得有返回值
        if (resultCode==RESULT_OK){
            //        判断手机系统版本号
            try {
                String imgPath=handleImageOnKitKat(data);
                postNewTopicPics(imgPath,requestCode,resultCode);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }



    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_tv_close:
                finish();
                break;
            case R.id.title_btn_next:
//                发布文章按钮，判断内容合法性，判断标签是否选择
                if (!tvFlag.getText().equals("")){

                }
                break;
            case R.id.topic_flag_:
//                話題選擇標簽列表爲空，請求數據
                if (arrayList.size()==0){
                    loadingDraw.show();
                    getTopicDivid();
                }
//                选择标签弹出dialog,选择后显示#xxxx#
                break;
            case R.id.topic_flag_t_close:
                Message msg1 = new Message();
                msg1.what = SHOW_DIALOG;
                msg1.obj = arrayList;
                msgHandler.sendMessage(msg1);
                break;
//                隐藏键盘
            case R.id.topic_keybaord_down:

                break;
                default:
                    break;
        }
    }
//    判断标题合法性
    private void getTopicTitleEditStr() {
        strTopicTitle=editTitle.getText().toString().trim();
    }
    //    计算位数
    private static int calculatePlaces(String str) {
        int m = 0;
        if (str!=null){
            char arr[] = str.toCharArray();
            for (int i = 0; i < arr.length; i++) {
                char c = arr[i];
//            中文字符
                if ((c >= 0x0391 && c <= 0xFFE5)) {
                    m = m + 1;
                } else if ((c >= 0x0000 && c <= 0x00FF)) {
                    m = m + 1;
                }
            }
        }
        return m;
    }
//      标题输入监听
    TextWatcher etWatcher= new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
        @Override
        public void afterTextChanged(Editable editable) {
            getTopicTitleEditStr();
            int cal=calculatePlaces(strTopicTitle);
            if (!((cal>=2) && (cal<=50))){
                Message msg1=new Message();
                msg1.what=MESSAGE_ERROR;
                msg1.obj="标题应在2至50个字符内";
                msgHandler.sendMessage(msg1);
            }
        }
    };



    //    选择标签
    private void flagDialog(ArrayList listItem){
        final List list=new ArrayList();
        if (listItem.size()!=0){
            for (int i=0;i<listItem.size();i++) {
                list.add(listItem.get(i));
            }
            Log.e("listItem",listItem.size()+"---------");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("请选择标签");
            builder.setSingleChoiceItems((String[])list.toArray(new String[list.size()]), 0,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Log.e("tag:", String.valueOf(list.get(which)));
                            flagStr=list.get(which);
                        }
                    });
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    Message msg=new Message();
                    msg.what=USER_SET_INFORMATION;
                    msg.obj=flagStr;
                    msgHandler.sendMessage(msg);

                }
            });
            builder.show();
        }else {
            AlertDialog.Builder dialog=new AlertDialog.Builder(this);
            dialog.setMessage("当前话题标签不可选");
            dialog.setNegativeButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }

    }

    //    处理图片
    private String handleImageOnKitKat(Intent data) throws JSONException {
        String imagePath=null;
        Uri uri=data.getData();
        if (DocumentsContract.isDocumentUri(this,uri)){
            String docId=DocumentsContract.getDocumentId(uri);
            if ("com.android.providers.media.documents".equals(uri.getAuthority())){
//                解析出数字格式id
                String id=docId.split(":")[1];
                String selection=MediaStore.Images.Media._ID+"="+id;
                imagePath=getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
            }else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                Uri contentUri=ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
                imagePath=getImagePath(contentUri,null);
            }
        }else if ("content".equalsIgnoreCase(uri.getScheme())){
            imagePath=getImagePath(uri,null);
        }else if ("file".equalsIgnoreCase(uri.getScheme())){
            imagePath=uri.getPath();
        }
        return imagePath;
    }

    //    图片路径
    private String getImagePath(Uri uri,String selection){
        String path=null;
        Cursor cursor=getContentResolver().query(uri,null,selection,null,null);
        if (cursor!=null){
            if (cursor.moveToFirst()){
                path=cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }


    //    提交图片
    private void postNewTopicPics(String imagesPaths, final int requestCode, final int resultCode){


        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        File file = new File(imagesPaths);
        builder.addFormDataPart("image", file.getName(), RequestBody.create(MEDIA_TYPE_IMAGE, file))
                .addFormDataPart("type", "topic");
        MultipartBody requestBody = builder.build();
        HttpUtil.sendOkHttpRequestAvatars(USER_AVATAR_IMG, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                Message msg2 = new Message();
                msg2.what = MESSAGE_ERROR;
                msg2.obj = "上传失败";
                msgHandler.sendMessage(msg2);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject jresp = new JSONObject(response.body().string());
                    switch (response.code()) {
//                        201
                        case HTTP_OK:
                            Message msg2 = new Message();
                            msg2.what = response.code();
                            msg2.obj = "图片上传成功";
                            msgHandler.sendMessage(msg2);

                            Intent data=new Intent();
//                            將路徑替換成網絡圖片路徑
                            Uri imgPath= Uri.parse(jresp.getString("path"));
                            Intent dat=data.setData(imgPath);
                            mToolbar.onActivityResult(requestCode, resultCode,dat);
                            break;
//                            422
                        case HTTP_USER_NULL:
                            Message msg6 = new Message();
                            msg6.what = response.code();
                            msg6.obj = "上传失败，图片可能过大或过小";
                            msgHandler.sendMessage(msg6);
                            break;
//                            403
                        case HTTP_USER_FORMAT_ERROR:
                            Message msg8 = new Message();
                            msg8.what = response.code();
                            msg8.obj ="格式不支持";
                            msgHandler.sendMessage(msg8);
                            break;
//                    401
                        case HTTP_USER_ERROR:

                            Authenticator authenticator = new Authenticator() {
                                @Override
                                public Request authenticate(Route route, Response response) throws IOException {
//    刷新token
                                    return response.request().newBuilder().addHeader("Authorization",  MyApplication.globalUserInfo.tokenType +  MyApplication.globalUserInfo.token).build();
                                }
                            };
                        default:
                            break;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }
    //    获取话题分类
    private void getTopicDivid(){
        HttpUtil.sendOkHttpGetTopicDivid(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Message msg9 = new Message();
                msg9.what = DISMISS_DIALOG;
                msg9.obj = loadingDraw;
                msgHandler.sendMessage(msg9);

                Message msg1 = new Message();
                msg1.what = MESSAGE_ERROR;
                msg1.obj = "服务器异常,请检查网络";
                msgHandler.sendMessage(msg1);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JSONObject jresp=null;
                JSONArray jsonArray=null;
                try {
                    jresp=new JSONObject(response.body().string());
                    switch (response.code()){
                        case HTTP_USER_GET_INFORMATION:
                            if (jresp.has("data")){
                                jsonArray=jresp.getJSONArray("data");
                                int jsonSize=jsonArray.length();
                                for (int i = 0; i < jsonSize; i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    TopicDIvid topicDIvid=new TopicDIvid();
                                    topicDIvid.setCategoryId(jsonObject.getString("id"));
                                    topicDIvid.setCategoryName(jsonObject.getString("name"));
                                    topicDIvid.setCategoryDesc(jsonObject.getString("description"));
                                    arrayList.add(topicDIvid.getCategoryName());
                                }
                                Message msg9 = new Message();
                                msg9.what = DISMISS_DIALOG;
                                msg9.obj = loadingDraw;
                                msgHandler.sendMessage(msg9);

                                Message msg1 = new Message();
                                msg1.what = SHOW_DIALOG;
                                msg1.obj = arrayList;
                                msgHandler.sendMessage(msg1);
                                //                修改标签


                            }
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
    @Override
    protected void onResume() {
        super.onResume();


    }
}
