package com.example.hackeru.shir.shirapplication;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;

import at.huber.youtubeExtractor.VideoMeta;
import at.huber.youtubeExtractor.YouTubeExtractor;
import at.huber.youtubeExtractor.YtFile;

public class YoutubeExtractorActivity extends Activity {

    private static final int REQUEST_PERMISSION_SETTING = 99;
    private static final String[] PARAMS_SAVE_MUSIC = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int RESULT_PARAMS_SAVE_MUSIC = 11;

    private static String youtubeLink;
//    private LinearLayout mainLayout;
    private ProgressBar mainProgressBar;
    private DownloadManager downloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_extractor);

        //mainLayout = (LinearLayout) findViewById(R.id.main_layout);
        mainProgressBar = (ProgressBar) findViewById(R.id.prgrBar);

//        //set filter to only when download is complete and register broadcast receiver
//        IntentFilter filter = new IntentFilter(Intent.ACTION_SEND);
//        registerReceiver(downloadReceiver, filter);

        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (savedInstanceState == null && Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String ytLink = intent.getStringExtra(Intent.EXTRA_TEXT);

                if (ytLink != null
                        && (ytLink.contains("://youtu.be/") ||
                            ytLink.contains("youtube.com/watch?v="))) {
                    youtubeLink = ytLink;

                    // We have a valid link
                    getYoutubeDownloadUrl(youtubeLink);
                    //displayAlert();

                } else {
                    Toast.makeText(this, "Not a valid YouTube link!", Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        } else if (savedInstanceState != null && youtubeLink != null) {
            getYoutubeDownloadUrl(youtubeLink);
        } else {
            finish();
        }
    }

    private void getYoutubeDownloadUrl(String youtubeLink) {
        new YouTubeExtractor(this) {

            @Override
            public void onExtractionComplete(SparseArray<YtFile> ytFiles, VideoMeta vMeta) {
                mainProgressBar.setVisibility(View.GONE);

                if (ytFiles == null) {
                    // Something went wrong we got no urls. Always check this.
                    finish();
                    return;
                }
                // Iterate over itags
                for (int i = 0, itag; i < ytFiles.size(); i++) {
                    itag = ytFiles.keyAt(i);
                    // ytFile represents one file with its url and meta data
                    YtFile ytFile = ytFiles.get(itag);

                    // Just add videos in a decent format => height -1 = audio
                    if (ytFile.getFormat().getHeight() == -1) {

                        String filename;
                        String videoTitle = vMeta.getTitle();
                        if (videoTitle.length() > 55) {
                            filename = videoTitle.substring(0, 55) + "." + ytFile.getFormat().getExt();
                        } else {
                            filename = videoTitle + "." + ytFile.getFormat().getExt();
                        }

                        filename = filename.replaceAll("\\\\|>|<|\"|\\||\\*|\\?|%|:|#|/", "");
                        downloadFromUrl(ytFile.getUrl(), videoTitle, filename);
                        finish();
                    }
                }
            }
        }.extract(youtubeLink, true, false);
    }

    private void downloadFromUrl(String youtubeDlUrl, String downloadTitle, String fileName) {
        Uri uri = Uri.parse(youtubeDlUrl);

        if (uri != null){
            if (canSaveMusic()){
                // Create request for android download manager
                downloadManager = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
                DownloadManager.Request request = new DownloadManager.Request(uri);
                request.setTitle(downloadTitle);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                try{
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, fileName);

                    //Enqueue download and save into referenceId
                    downloadManager.enqueue(request);
                }catch (Exception e){
                    Toast.makeText(YoutubeExtractorActivity.this,
                            e.getMessage(), Toast.LENGTH_LONG);
                }
            }else {
                ActivityCompat.requestPermissions(this, netPermisssion(PARAMS_SAVE_MUSIC), RESULT_PARAMS_SAVE_MUSIC);
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RESULT_PARAMS_SAVE_MUSIC) {

            if (canSaveMusic()) {

                Toast.makeText(this, "You can save image", Toast.LENGTH_SHORT).show();

            } else if (!(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))) {


                final AlertDialog.Builder settingDialog = new AlertDialog.Builder(YoutubeExtractorActivity.this);
                settingDialog.setTitle("Permissioin");
                settingDialog.setMessage("Now you need to enable permisssion from the setting because without permission this app won't run properly \n\n  goto -> setting -> appInfo");
                settingDialog.setCancelable(false);

                settingDialog.setPositiveButton("Setting", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        dialogInterface.cancel();

                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
                        Toast.makeText(getBaseContext(), "Go to Permissions to Grant all permission ENABLE", Toast.LENGTH_LONG).show();
                    }
                });
                settingDialog.show();

                Toast.makeText(this, "You need to grant permission from setting", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PERMISSION_SETTING) {

            if (canSaveMusic()) {
                Toast.makeText(this, "You can save music", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //  This method return  permission denied String[] so we can request again
    private String[] netPermisssion(String[] wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String permission : wantedPermissions) {
            if (!hasPermission(permission)) {
                result.add(permission);
            }
        }

        return (result.toArray(new String[result.size()]));
    }

    private boolean canSaveMusic() {
        return (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) &&
                hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }

    private boolean hasPermission(String permissionString) {
        return (ContextCompat.checkSelfPermission(this, permissionString) ==
                PackageManager.PERMISSION_GRANTED);
    }

//    private void displayAlert()
//    {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setMessage("Are you sure you want to exit?").setCancelable(
//                false).setPositiveButton("Yes",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        dialog.cancel();
//                    }
//                }).setNegativeButton("No",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        dialog.cancel();
//                    }
//                });
//        AlertDialog alert = builder.create();
//        alert.show();
//    }
}
