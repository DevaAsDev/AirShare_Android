package com.darkappstore.share;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


@RequiresApi(api = Build.VERSION_CODES.R)
public class MainActivity extends AppCompatActivity {

    static Thread Thread,Thread1 = null;
    static volatile String SERVER_IP;
    static volatile int SERVER_PORT = 8888;
    static volatile String fileName;
    static volatile String fileSize = "0";
    static volatile Uri uris;
    static volatile int currentSize = 0;
    static volatile boolean threadIsRunning = true;

    ProgressBar pr;
    TextView textView;


    int PERMISSION_ALL = 152;
    String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
    };


    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("AirShare");
        allPermissions();
        finishInit();


        CardView btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(v -> {

            threadIsRunning = true;
            EditText editText = findViewById(R.id.ip);
            SERVER_IP = String.valueOf(editText.getText());

            Thread1 = new Thread(new Thread1());
            Thread1.start();

            Thread = new Thread(new Thread2());
            Thread.start();
            textView.setText(R.string.initializing);
        });

        CardView reset = findViewById(R.id.btnReset);
        reset.setOnClickListener(v -> {
            threadIsRunning = false;
            try {
                MainActivity.Thread1.stop();
            }catch (Exception ignore){}
        });

        CardView locate = findViewById(R.id.locate);
        locate.setOnClickListener(v -> showFileChooser());

        pr = findViewById(R.id.progressBar);
        textView = findViewById(R.id.progress);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId()== R.id.exit){
            finish();
        }
        return super.onOptionsItemSelected(item);

    }

    private void allPermissions() {
        if (hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return true;
                }
            }
        }
        return false;
    }

    public void showFileChooser() {

        mGetContent.launch("*/*");

    }

    ActivityResultLauncher<String> mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    // Handle the returned Uri
                    uris = uri;
                    fileName = getFileName(uris);
                    fileSize = getFileSize(uris);

                    TextView tx = findViewById(R.id.name);
                    tx.setText(fileName);
                }
            });


    private String getFileName(Uri uri) throws IllegalArgumentException {
        // Obtain a cursor with information regarding this uri
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor.getCount() <= 0) {
            cursor.close();
            throw new IllegalArgumentException("Can't obtain file name, cursor is empty");
        }

        cursor.moveToFirst();

        String fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
        cursor.close();

        return fileName;
    }

    private String getFileSize(Uri uri) throws IllegalArgumentException {
        // Obtain a cursor with information regarding this uri
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor.getCount() <= 0) {
            cursor.close();
            throw new IllegalArgumentException("Can't obtain file name, cursor is empty");
        }

        cursor.moveToFirst();
        String fileSize = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
        cursor.close();

        return fileSize;
    }


    private void finishInit() {

        //creating AirShare folder, if not created
        try {
            File folder = new File(Environment.getExternalStorageDirectory() + "/Download/AirShare");
            boolean success = true;
            if (!folder.exists()) {
                success = folder.mkdirs();
            }
            if (success) {
                print_log("folder created");
            } else {
                print_log("folder not created, maybe exist or missing permission");
            }
        } catch (Exception e) {
            print_log("Exception while creating 'AirShare' folder  " + e);
        }


    }

    static public void print_log(String log) {
        Log.e("TAG", log);
    }

    class Thread1 implements Runnable {
        @Override
        public void run() {
            Socket socket;
            try {

                socket = new Socket(SERVER_IP, SERVER_PORT);

                byte[] fileInfo;
                String st = fileName+","+fileSize;
                fileInfo = st.getBytes(StandardCharsets.UTF_8);
                OutputStream streams = socket.getOutputStream();
                streams.write(fileInfo, 0, fileInfo.length);
                streams.flush();

                runOnUiThread(() -> textView.setText(R.string.connected));

                ParcelFileDescriptor fileData = getContentResolver().
                        openFileDescriptor(uris, "r");

                InputStream is = new FileInputStream(fileData.getFileDescriptor());
                byte[] bytes = new byte[1024];       //creating byte with size of 1024

                runOnUiThread(() -> textView.setText(R.string.transferring));
                OutputStream stream = socket.getOutputStream();

                int count = is.read(bytes, 0, 1024);
                synchronized (this){
                    currentSize += bytes.length;
                }
                while (count != -1) {
                    stream.write(bytes, 0, 1024);
                    /*synchronized (this){
                        currentSize += bytes.length;
                    }*/
                    count = is.read(bytes, 0, 1024);
                }

                socket.close();
                stream.close();
                streams.close();
                threadIsRunning = false;
                synchronized (this){
                    currentSize = 0;
                }


            } catch (FileNotFoundException noFile) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), "File Not Fount.", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class Thread2 implements Runnable {

        @Override
        public void run() {

            try {
                while (threadIsRunning){
                    java.lang.Thread.sleep(500);
                    runOnUiThread(() -> {
                        pr.setMax(Integer.parseInt(fileSize));
                        pr.setProgress(currentSize);
                    });
                }
                runOnUiThread(() -> textView.setText(R.string.finish));
            }catch (Exception ignore){}
        }
    }


}