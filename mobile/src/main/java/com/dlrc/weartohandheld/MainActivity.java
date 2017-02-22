package com.dlrc.weartohandheld;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.IOException;
import java.util.Timer;

public class MainActivity extends Activity implements
        ConnectionCallbacks,
        OnConnectionFailedListener,
        ChannelApi.ChannelListener {

    private final String TAG = "MainActivity";
    private static final String TRANSFER_FILE_PATH = "/transfer-file";

    private GoogleApiClient mGoogleApiClient;
    Timer mTransferProgressTimer = new Timer();

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.text);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Wearable.ChannelApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle conectionHint) {
        LOGD(TAG, "onConnected(): Successfully connected to Google API client");
        Wearable.ChannelApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }

    @Override
    public void onChannelOpened(Channel channel) {
        LOGD(TAG, "onChannelOpened(): Successfully opened the channel");

        if (channel.getPath().equals(TRANSFER_FILE_PATH)) {
            try {
                String directoryPath = Environment.getExternalStorageDirectory().getPath() + "/TraceDataCollection";
                File dir = new File(directoryPath);
                if (!dir.exists())
                    dir.mkdir();
                File file = new File(directoryPath + "/recieveddata.txt");
                if (!file.exists())
                    file.createNewFile();
                mTextView.setText("file transfering");
                channel.receiveFile(mGoogleApiClient, Uri.fromFile(file), false);
//
//                channel.getInputStream(mGoogleApiClient).setResultCallback(new ResultCallback<Channel.GetInputStreamResult>() {
//                    @Override
//                    public void onResult(@NonNull Channel.GetInputStreamResult getInputStreamResult) {
//                        InputStream is = getInputStreamResult.getInputStream();
//                        try {
//                            int bufferSize = 100;
//                            byte[] buffer = new byte[bufferSize * 1024];
//                            int count = 0;
//                            while (is.read(buffer) != -1) {
//                                count++;
//                                // TODO: write data to file
//                            }
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onChannelClosed(Channel channel, int i, int i1) {
        LOGD(TAG, "onChannelClosed(): Successfully closed the channel");
    }

    @Override
    public void onInputClosed(Channel channel, int i, int i1) {
        LOGD(TAG, "onInputClosed(): Successfully received the file");
        if (channel.getPath().equals(TRANSFER_FILE_PATH)) {
            mTextView.setText("file received");
        }
    }

    @Override
    public void onOutputClosed(Channel channel, int i, int i1) {
        LOGD(TAG, "onOutputClosed(): Successfully sended the file");
    }

    public static void LOGD(final String tag, String message) {
//        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
//        }
    }

}
