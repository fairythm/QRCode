package com.axion.xthon.qrcode;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AlertDialog;
import android.support.v4.app.ActivityCompat;

import android.Manifest;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import android.util.Log;

import com.axion.xthon.zxing.android.CaptureActivity;

public class MainActivity extends AppCompatActivity {

    //组件声明
    EditText inputString;
    Button executeQRCode;
    ImageView qrCode;
    Bitmap tobeRecognized;
    EditText showResult;
    android.support.v7.widget.Toolbar toolbar;//原始的Toolbar来自于android.widget.Toolbar，使用这个Toolbar是错误的

    static int requestCode=0xFF16;//在打开图片中作为事件传递参数而使用
    static int Codescanned=0xFF19;//在相机扫描完后作为事件传递参数使用
    static String DECODED_CONTENT_KEY="codedContent";
    Uri uri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //组件实例化
        inputString=findViewById(R.id.inputURL);            //输入框
        executeQRCode=findViewById(R.id.execute);           //执行按钮
        qrCode=findViewById(R.id.qrCode);                   //用于二维码展示的ImageView
        //recognizeQRCode=findViewById(R.id.recognizeQRCode); //用于识别二维码的按钮
        //tobeRecognizedView=findViewById(R.id.tobeRecognized);//选取的待识别的图片就储存在这里
        //scanQRCodeByCamera=findViewById(R.id.scanQRCodeByCamera);//用照相机识别二维码
        showResult=findViewById(R.id.showResult);           //二维码的扫描结果

        toolbar=(android.support.v7.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //parseResult=findViewById(R.id.parseResult);         //点击后，开始解析扫码结果
        //flipflop=new Button(MainActivity.this);

        //按钮系列监听器
        executeQRCode.setOnClickListener(new View.OnClickListener() {   //按下按钮，执行二维码的生成工作
            @Override
            public void onClick(View v){
                Bitmap bitMapLogo;
                bitMapLogo = QRCodeLogo.createQRCodeBitmap(
                        inputString.getText().toString(),               //第一个参数：输入的字符串
                        720,                                       //第二个参数：二维码大小，（像素）
                        BitmapFactory.decodeResource(getResources(), R.drawable.njuptlogo),//第三个参数：中心logo的打开
                        (float)0.1);                                    //第四个参数：中心logo占全图比重
                qrCode.setImageBitmap(bitMapLogo);

                //下面为qrCode设置长按保存功能
                qrCode.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        //使用一个AlertDialog来弹出保存提示以供用户去按一下
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setItems(new String[]{getResources().getString(R.string.save_picture)}, new DialogInterface.OnClickListener() {
                            //给用户去按一下的东西
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                qrCode.setDrawingCacheEnabled(true);
                                Bitmap imageBitmap = qrCode.getDrawingCache();
                                if (imageBitmap != null) {
                                    new SaveImageTask().execute(imageBitmap);
                                }
                            }
                        });
                        builder.show();
                        return true;
                    }
                });
                //长按保存功能设置完毕
            }
        });

    }


    private class SaveImageTask extends AsyncTask<Bitmap, Void, String> {
        @Override
        protected String doInBackground(Bitmap... params) {
            String result = getResources().getString(R.string.save_picture_failed);
            try {
                //不出意外的话sdcard应该会等于storage/emulated/0
                String sdcard = Environment.getExternalStorageDirectory().toString();
                Log.d("手机",sdcard);

                //动态授予写入外存的权限,Android6.0以后,静态注册权限已经不行了,需要动态授予权限
                //一般手机的话，直接就可以了，无需操作，小米手机的话可能还需要用户去点一下允许才行
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1000);

                File file = new File(sdcard + "/Download");

                if (!file.exists()) {
                    file.mkdirs();
                }

                File imageFile = new File(file.getAbsolutePath(),new Date().getTime()+".jpg");
                FileOutputStream outStream = null;
                outStream = new FileOutputStream(imageFile);
                Bitmap image = params[0];
                image.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                outStream.flush();
                outStream.close();
                result = getResources().getString(R.string.save_picture_success,  file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getApplicationContext(),result,Toast.LENGTH_SHORT).show();
            //不管保存成功与否，toast给用户提示
            qrCode.setDrawingCacheEnabled(false);
        }
    }

    private void selectPic(){
        Intent intent=new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        //选定图片完成，返回一个结果码以唤醒接下来的操作，这是重写自AppCompatActivity的方法
        startActivityForResult(intent,requestCode);
    }

    /**
     * 其他活动结束时返回值触发活动，用于
     * 1.选择二维码图片后触发后续事件储存该图
     * 2.相机扫码完成后触发后续事件
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        Log.e("requestCode",String.valueOf(requestCode));

        Log.e("resultCode",String.valueOf(resultCode));


        if(requestCode==MainActivity.requestCode){

            if(data==null) return;//用户没有选择图片直接返回了
            Uri uri=data.getData();//选取图片的地址数据返回给data，传递给uri
            Log.e("path",uri.toString());

            //调用content接口
            ContentResolver cr=this.getContentResolver();
            try{
                tobeRecognized=BitmapFactory.decodeStream(cr.openInputStream(uri));//使用inputstring解析并抽取uri地址的内容，即导入了选取的图片
                //打开结果保存到一个Bitmap类型变量tobeRecognized中
                //tobeRecognizedView.setImageBitmap(bitmap);//同时将打开的结果通过tobeRecognizedView展示给用户看
            }catch(FileNotFoundException e){
                Log.e("Exception:",e.getMessage());
            }
            //打开图片完毕，已经将bitmap储存于toberecognized

            //下面开始解析这张图
            String contents = QRCodeUtils.getStringFromQRCode(tobeRecognized);
            //解析完毕显示解析结果给用户看
            showResult.setText(contents);
            //触发浏览器，文本阅读器，ssr
            Intent intent=new Intent(Intent.ACTION_VIEW);
            if(isHttpUrl(contents)){//判断二维码解析结果是否为网址，是则启动系统浏览器

                //判断该网址是否已经含有http://头，因为必须要有http://头才能启动浏览器，没有头的就给他加头
                if(contents.startsWith("https://")||contents.toString().startsWith("http://")) {
                    uri=Uri.parse(contents);
                }else{
                    uri=Uri.parse("https://"+contents);//默认加https://头而不是http头,不加头的话，不会默认启动浏览Activity，导致没有Activity可启动而会崩溃
                }
                intent.setData(uri);
                startActivity(intent);
            }else{
                /*
                 *增加格外功能：如果是ssr链接，则打开ssr
                 * ssr链接以ssr://为开头自动识别
                 */
                if(contents.startsWith("ssr://")){
                    uri=Uri.parse(contents);
                    intent.setData(uri);
                    startActivity(intent);
                }

                else {
                    //不是网址，启动系统文本阅读器查看
                    intent.setAction(android.content.Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(contents), "text/html");

                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
        }
        if(requestCode==MainActivity.Codescanned&&resultCode==RESULT_OK){
            Log.e("scanned","next");
            //以下intent改名为intentScanned
            //以下contents改名为contentsScanned
            if(data==null)return;
            String contentsScanned=data.getStringExtra(DECODED_CONTENT_KEY);
            showResult.setText(contentsScanned);
            Intent intentScanned = new Intent(Intent.ACTION_VIEW);
            if(isHttpUrl(contentsScanned)){//判断二维码解析结果是否为网址，是则启动系统浏览器

                    //判断该网址是否已经含有http://头，因为必须要有http://头才能启动浏览器，没有头的就给他加头
                    if(contentsScanned.startsWith("https://")||contentsScanned.toString().startsWith("http://")) {
                        uri=Uri.parse(contentsScanned);
                    }else{
                        uri=Uri.parse("https://"+contentsScanned);//默认加https://头而不是http头,不加头的话，不会默认启动浏览Activity，导致没有Activity可启动而会崩溃
                    }
                intentScanned.setData(uri);
                startActivity(intentScanned);
            }else{
                /*
                 *增加格外功能：如果是ssr链接，则打开ssr
                 * ssr链接以ssr://为开头自动识别
                 */
                if(contentsScanned.startsWith("ssr://")){
                    uri=Uri.parse(contentsScanned);
                    intentScanned.setData(uri);
                    startActivity(intentScanned);
                }

                else {
                    //不是网址，启动系统文本阅读器查看
                    intentScanned.setAction(android.content.Intent.ACTION_VIEW);
                    intentScanned.setDataAndType(Uri.parse(contentsScanned), "text/html");

                    intentScanned.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intentScanned);
                }
            }
        }
    }

    /**
     * 使用正则表达式判断字符串是否为URL
     * @param urls 用户头像key
     * @return true:是URL、false:不是URL
     */
    public static boolean isHttpUrl(String urls) {
        boolean isurl = false;
        String regex = "(((https|http)?://)?([a-z0-9]+[.])|(www.))"
                + "\\w+[.|\\/]([a-z0-9]{0,})?[[.]([a-z0-9]{0,})]+((/[\\S&&[^,;\u4E00-\u9FA5]]+)+)?([.][a-z0-9]{0,}+|/?)";//设置正则表达式
        Pattern pat = Pattern.compile(regex.trim());//比对
        Matcher mat = pat.matcher(urls.trim());
        isurl = mat.matches();//判断是否匹配
        if (isurl) {
            isurl = true;
        }
        return isurl;
    }

    //判断系统是否设置了默认浏览器
    public  boolean hasPreferredApplication(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        ResolveInfo info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return !"android".equals(info.activityInfo.packageName);
    }
    //如果info.activityInfo.packageName为android,则没有设置,否则,有默认的程序.

    //设置Toolbar上的menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.toolbar,menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.settings:
                //这中间写按下settings的代码
                break;
            case R.id.camera:
                //这里写按下相机扫码的代码
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA},1000);
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.VIBRATE},1000);
                Intent intent=new Intent(MainActivity.this,CaptureActivity.class);
                startActivityForResult(intent,Codescanned);
                break;
            case R.id.choosepic:
                //这里写按下选择图片的代码
                selectPic();
                break;
            default:
        }
        return true;
    }
}


