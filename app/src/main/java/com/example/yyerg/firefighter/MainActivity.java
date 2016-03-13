package com.example.yyerg.firefighter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;

import org.apache.http.NameValuePair;

import org.apache.http.client.ClientProtocolException;

import org.apache.http.client.entity.UrlEncodedFormEntity;

import org.apache.http.client.methods.HttpPost;

import org.apache.http.impl.client.DefaultHttpClient;

import org.apache.http.message.BasicNameValuePair;

import org.apache.http.protocol.HTTP;

import org.apache.http.util.EntityUtils;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    final String APP_TAG = "Firefighter";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public static String HEART_RATE_MEASUREMENT = "00002a37";

    private BluetoothManager mbluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private Integer mConnectionState;
    private final Integer REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;
    private boolean mScanning;
    private boolean mConnected;

    private ArrayList mLeDevices = new ArrayList<BluetoothDevice>();
    private List<BluetoothGattService> mLeServices;
    private List<BluetoothGattCharacteristic> mLeCharacteristic;
    private BluetoothGattService mLeHeartRateService;
    private BluetoothGattCharacteristic mLeHeartRateCharacteristic;
    private Handler mHandler;

    private TextView tvStatus;

    private SensorManager mSensorManager;
    private Sensor orientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        orientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);


        tvStatus = (TextView) this.findViewById(R.id.tvStatus);
        tvStatus.setText("AAAAAAAAAAAAAAAAAAAAAAAAAAAA");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        mHandler = new Handler();
        mScanning = true;
        setupBLE();
        Thread thread = new Thread(httpThread);
        thread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupBLE(){
        this.mbluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = mbluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        scanLeDevice(true);
    }

    private Runnable httpThread = new Runnable() {
        public void run() {
            sendPostDataToInternet();
        }
    };

    private void sendPostDataToInternet() {
        HttpPost httpRequest = new HttpPost("http://chasewind.co/yyergg/test.php");
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        try {

            httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
            if (httpResponse.getStatusLine().getStatusCode() == 200)

            {
                String strResult = EntityUtils.toString(httpResponse.getEntity());
                Log.d(APP_TAG,strResult);

            }
//            URL url = new URL("http://chasewind.co/yyergg/test.php");
//            String sql = URLEncoder.encode("username", "UTF-8")
//                    + "=" + URLEncoder.encode("yyergg", "UTF-8");
//            sql += "&" + URLEncoder.encode("type", "UTF-8")
//                    + "=" + URLEncoder.encode("heartrate", "UTF-8");
//            sql += "&" + URLEncoder.encode("value", "UTF-8")
//                    + "=" + URLEncoder.encode("133", "UTF-8");

//            URLConnection conn = url.openConnection();
//            conn.setDoOutput(true);
//            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
//            wr.write(sql);
        }catch(Exception e){
            Log.d(APP_TAG,e.toString());
        }
    }


    private void scanLeDevice(final boolean enable) {
        mLeDevices.clear();
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    // Log.d(APP_TAG,"found something!!!!!!!!!!!!!1");
                    if(device.getAddress().equals("DF:F8:60:A6:B5:62")) {
                    //if(device.getAddress().equals("00:22:D0:81:0C:89")){
                        Log.d(APP_TAG, "GOT YOU");
                        if(!mLeDevices.contains(device)) {
                            mLeDevices.add(device);
                            connect(device);
                        }
                        mBluetoothAdapter.stopLeScan(null);

                    }
                }
            };

    public boolean connect(BluetoothDevice device) {
        if (mBluetoothAdapter == null) {
            Log.d(APP_TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (device == null) {
            Log.d(APP_TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        return true;
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.d(APP_TAG, "Connected to device.");
                mBluetoothGatt.discoverServices();
                // Attempts to discover services after successful connection.
                Log.i(APP_TAG, "Attempting to start service discovery");

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(APP_TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mLeServices = mBluetoothGatt.getServices();
                Integer i;
                for(i=0;i<mLeServices.size();i++){
                    if(mLeServices.get(i).getUuid().toString().substring(0,8).equals("0000180d")) {
                        mLeHeartRateService=mLeServices.get(i);
                        break;
                    }
                }
                mLeCharacteristic = mLeHeartRateService.getCharacteristics();
                for(i=0;i<mLeCharacteristic.size();i++){
                    if(mLeCharacteristic.get(i).getUuid().toString().substring(0,8).equals("00002a37")) {
                        mLeHeartRateCharacteristic = mLeCharacteristic.get(i);
                        break;
                    }
                }

                Log.d(APP_TAG,"***********"+mLeHeartRateCharacteristic.getUuid());
                List<BluetoothGattDescriptor> mLeDescriptor;
                mLeDescriptor = mLeHeartRateCharacteristic.getDescriptors();

                for(i=0;i<mLeDescriptor.size();i++){
                    Log.d(APP_TAG,mLeDescriptor.get(i).getUuid().toString());
                }

                BluetoothGattDescriptor descriptor = mLeHeartRateCharacteristic.getDescriptor(mLeDescriptor.get(0).getUuid());
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                mBluetoothGatt.setCharacteristicNotification(mLeHeartRateCharacteristic, true);
                if(!mBluetoothGatt.readCharacteristic(mLeHeartRateCharacteristic)){
                    Log.d(APP_TAG,"read");
                }
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(APP_TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
            Log.d(APP_TAG, "onCharacteristicRead");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            Log.d(APP_TAG, "onCharacteristicChanged");
        }
    };


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.
        // Data parsing is carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (characteristic.getUuid().toString().substring(0,8).equals(HEART_RATE_MEASUREMENT)) {
            int dataFlags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            int heartRateMeasurementValue = 0;
            boolean heartRateValueFormat = false; // UINT8 or UINT16
            boolean sensorContactSupportedStatus = false; // "not supported" or "supported"
            boolean sensorContactDetectedStatus = false; // "not detected" or "detected"
            boolean energyExpendedStatus = false; // "not present" or "not present"
            boolean RrInterval = false; //"not present" or "one or more values are present. unit 1/1024s"
            heartRateValueFormat = ((dataFlags & 1) != 0);
            sensorContactSupportedStatus = ((dataFlags>>2 & 1) != 0);
            sensorContactDetectedStatus = ((dataFlags>>1 & 1) != 0);
            energyExpendedStatus = ((dataFlags>>3 & 1) != 0);
            RrInterval = ((dataFlags>>4 & 1) != 0);

            if (heartRateValueFormat) {
                heartRateMeasurementValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
            } else {
                heartRateMeasurementValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            }
            final byte[] data = ByteBuffer.allocate(4).putInt(heartRateMeasurementValue).array();
            Log.d(APP_TAG, String.format("Received heart rate: %d", heartRateMeasurementValue));
            intent.putExtra(ACTION_DATA_AVAILABLE,data);
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                invalidateOptionsMenu();
            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                invalidateOptionsMenu();
                clearUI();
            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                Bundle data = intent.getExtras();
                try {
                    displayData((byte[])data.get(ACTION_DATA_AVAILABLE));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    void clearUI(){
        this.tvStatus.setText("disconnected");
    }

    void displayData(byte[] data) throws MalformedURLException {
        Integer i = ByteBuffer.wrap(data).getInt();
        Log.d(APP_TAG, "displayData" + i.toString());
        this.tvStatus.setText(i.toString());
    }

    @Override
    protected void onDestroy(){
        mBluetoothGatt.disconnect();
        mBluetoothGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        mSensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        mSensorManager.unregisterListener(this);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        float azimuth_angle = event.values[0];
        float pitch_angle = event.values[1];
        float roll_angle = event.values[2];
        //Log.d(APP_TAG,Float.toString(azimuth_angle)+" "+Float.toString(pitch_angle)+" "+Float.toString(roll_angle));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}
