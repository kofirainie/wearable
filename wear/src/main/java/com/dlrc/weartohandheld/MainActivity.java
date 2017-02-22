package com.dlrc.weartohandheld;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public class MainActivity extends WearableActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        CapabilityApi.CapabilityListener {

    private static final String TAG = "MainAcitivy";
    private static final String SENSOR_DATA_CAPABILITY_NAME = "imu_and_gps";
    private static final String TRANSFER_FILE_PATH = "/transfer-file";

    private TextView mTextView;
    private Button mButton;

    private GoogleApiClient mGoogleApiClient;
    private String mSensorDataNodeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.text);
        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOGD(TAG, "onClick(): clicked send button");

                if (mGoogleApiClient!= null && mSensorDataNodeId != null) {
                    Wearable.ChannelApi.openChannel(
                            mGoogleApiClient, mSensorDataNodeId, TRANSFER_FILE_PATH).setResultCallback(new ResultCallback<ChannelApi.OpenChannelResult>() {
                        @Override
                        public void onResult(@NonNull ChannelApi.OpenChannelResult openChannelResult) {
                            LOGD(TAG, "onResult(): channel opened");

//                            String directoryPath = Environment.getExternalStorageDirectory().getPath() + "/TraceDataCollection";
//                            File file = new File(directoryPath + "/data.txt");
//                            openChannelResult.getChannel().sendFile(mGoogleApiClient, Uri.fromFile(file));

                            openChannelResult.getChannel().getOutputStream(mGoogleApiClient).setResultCallback(new ResultCallback<Channel.GetOutputStreamResult>() {
                                @Override
                                public void onResult(@NonNull Channel.GetOutputStreamResult getOutputStreamResult) {
                                    OutputStream os = getOutputStreamResult.getOutputStream();
                                    try {
                                        String directoryPath = Environment.getExternalStorageDirectory().getPath() + "/TraceDataCollection";
                                        File file = new File(directoryPath + "/data.txt");
                                        if (!file.exists()) {
                                            //TODO:: need to handle the absence of file, such as toast or error alert
                                            return;
                                        }
                                        double totalSize = (double) file.length() / 1024.0f; // in KB
                                        FileInputStream fis = new FileInputStream(file);
                                        int bufferSize = 100;
                                        byte[] buffer = new byte[bufferSize * 1024];
                                        int count = 0;
                                        while (fis.read(buffer) != -1) {
                                            os.write(buffer);
                                            count++;
                                            double transferredSize = (count * bufferSize < totalSize) ? (count * bufferSize) : totalSize;
                                            double progress = transferredSize / totalSize;
                                            System.out.println(String.format("percent: %.0f", progress * 100));
                                        }
                                        os.close();
                                        fis.close();

                                    }  catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }
                            });
                        }
                    });
                }
            }
        });

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
            Wearable.CapabilityApi.removeCapabilityListener(mGoogleApiClient, this, SENSOR_DATA_CAPABILITY_NAME);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle conectionHint) {
        LOGD(TAG, "onConnected(): Successfully connected to Google API client");
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient, this, SENSOR_DATA_CAPABILITY_NAME);

        Wearable.CapabilityApi.getCapability(
                mGoogleApiClient, SENSOR_DATA_CAPABILITY_NAME, CapabilityApi.FILTER_REACHABLE).setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
            @Override
            public void onResult(@NonNull CapabilityApi.GetCapabilityResult getCapabilityResult) {
                updateSensorDataCapability(getCapabilityResult.getCapability());
            }
        });

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
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        updateSensorDataCapability(capabilityInfo);
    }

    private void updateSensorDataCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        mSensorDataNodeId = pickBestNodeId(connectedNodes);
        LOGD(TAG, "updateSensorDataCapability(): mSensorDataNodeId = " + mSensorDataNodeId);
    }

    private String pickBestNodeId(Set<Node> nodes) {    // Find a nearby node or pick one arbitrarily
        String bestNodeId = null;
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    private static void LOGD(final String tag, String message) {
//        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
//        }
    }

}
