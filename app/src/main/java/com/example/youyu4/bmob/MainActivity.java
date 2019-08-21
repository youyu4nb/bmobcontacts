package com.example.youyu4.bmob;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.bmob.v3.Bmob;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.datatype.BmobDate;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.SaveListener;

public class MainActivity extends AppCompatActivity {
    private Content obj;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Bmob.initialize(this, "689746b23ef2b3fe9e87e9785b00d371");//初始化BmobSDK
        obj = new Content();
        Button sendData = findViewById(R.id.send_data);
        Button getData = findViewById(R.id.get_data);
        sendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CONTACTS ) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_CONTACTS,Manifest.permission.WRITE_CONTACTS},1);
                }else {
                    getPhoneContacts();
                }
            }
        });
        getData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new  TaskThread().start();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    getPhoneContacts();
                }else {
                    Toast.makeText(this,"You denied the permission",Toast.LENGTH_SHORT).show();
                }
        }
    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                {
                    System.out.println("-->回到主线程刷新ui任务");
                    Toast.makeText(MainActivity.this, "联系人同步完成", Toast.LENGTH_SHORT).show();
                }
                break;

                default:
                    break;
            }
        };
    };

    class TaskThread extends Thread {
        public void run() {
            System.out.println("-->做一些耗时的任务");
            getData();
            handler.sendEmptyMessage(0);
        };
    };

    /**
     * 根据时间从后台获取数据
     * */
    private void getData() {
        BmobQuery<Content> query = new BmobQuery<Content>();
        List<BmobQuery<Content>> and = new ArrayList<BmobQuery<Content>>();
        //大于00：00：00
        BmobQuery<Content> q1 = new BmobQuery<Content>();
        String start = "2019-08-21 18:28:00";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = sdf.parse(start);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        q1.addWhereGreaterThanOrEqualTo("createdAt", new BmobDate(date));
        and.add(q1);
        query.and(and);
        query.findObjects(new FindListener<Content>() {
            @Override
            public void done(List<Content> list, BmobException e) {
                for (int i = 0; i < list.size(); i++) {
                    testAddContacts(list.get(i).getName(), list.get(i).getNum());
                }
            }

        });
    }

    /**
     * 向后台发送联系人数据
     */
    public void upData(String name, String number) {
        obj.setName(name);
        obj.setNum(number);
        obj.save(new SaveListener<String>() {
            @Override
            public void done(String s, BmobException e) {
                Log.e("------", s);//objectId
            }
        });
    }


    /**
     * 从手机中获取联系人
     */
    private void getPhoneContacts() {
        List<Content> list = new ArrayList();
        final String[] PHONES_PROJECTION = new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
        ContentResolver resolver = getContentResolver();
        try {
            // 获取手机联系人
            Cursor phoneCursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    PHONES_PROJECTION, null, null, null);
            if (phoneCursor != null) {
                while (phoneCursor.moveToNext()) {
                    // 得到手机号码
                    String num = phoneCursor
                            .getString(1);
                    // 当手机号码为空的或者为空字段 跳过当前循环
                    if (TextUtils.isEmpty(num))
                        continue;
                    // 得到联系人名称
                    String name = phoneCursor
                            .getString(0);
                    //创建实体类解析联系人
                    Content mContent = new Content(name, num);
                    list.add(mContent);  //添加到集合里
                }
                phoneCursor.close();  //关流
                setData(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setData(List<Content> list) {
        if (list != null && list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                upData(list.get(i).getName(), list.get(i).getNum());
            }
            Toast.makeText(this, "联系人同步完成", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "手机中暂无联系人", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 添加联系人
     * 数据一个表一个表的添加，每次都调用insert方法
     */
    public void testAddContacts(String name, String num) {
        /**
         * 往 raw_contacts 中添加数据，并获取添加的id号
         **/
        Uri uri = Uri.parse("content://com.android.contacts/raw_contacts");
        ContentResolver resolver = this.getContentResolver();
        ContentValues values = new ContentValues();
        long contactId = ContentUris.parseId(resolver.insert(uri, values));
        /* 往 data 中添加数据（要根据前面获取的id号） */
        // 添加姓名
        uri = Uri.parse("content://com.android.contacts/data");
        values.put("raw_contact_id", contactId);
        values.put("mimetype", "vnd.android.cursor.item/name");
        values.put("data2", name);
        resolver.insert(uri, values);
        // 添加电话
        values.clear();
        values.put("raw_contact_id", contactId);
        values.put("mimetype", "vnd.android.cursor.item/phone_v2");
        values.put("data2", "2");
        values.put("data1", num);
        resolver.insert(uri, values);

    }

}
