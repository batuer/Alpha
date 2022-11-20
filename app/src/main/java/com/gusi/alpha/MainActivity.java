package com.gusi.alpha;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Random;

public class MainActivity extends Activity {

    private static final String TAG = "Ylw_Main";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permission();
//        builder.setOnAudioFocusChangeListener();
        WebView webView = findViewById(R.id.webView);
        webView.loadUrl("https://www.baidu.com/");
        webView.getSettings().setJavaScriptEnabled(true);
    }


    private String getDataFromNet() {
        return "";
    }


    private void permission() {
        String readPhoneState = Manifest.permission.READ_PHONE_STATE;
        String accessNetworkState = Manifest.permission.ACCESS_NETWORK_STATE;
        String[] permissions = {readPhoneState, accessNetworkState};
        if (checkPermission(readPhoneState, android.os.Process.myPid(), android.os.Process.myUid()) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(permissions, 100);
        }
    }

    public void getMobileIfaces(View view) {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network[] allNetworks = connectivityManager.getAllNetworks();
        for (Network allNetwork : allNetworks) {
            Log.i(TAG, "getMobileIfaces: " + allNetwork.toString());
        }
    }

    public void insert(View view) {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        SubscriptionManager subscriptionManager =
                (SubscriptionManager) getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE);

        Log.i(TAG, "insert: " + telephonyManager.getLine1Number());
        Log.i(TAG, "insert: " + telephonyManager.getSubscriberId());
        Log.i(TAG,
                "insert: " + subscriptionManager.getDefaultSubscriptionId() + ":" + telephonyManager.isVoiceCapable());

//        StringBuilder sb = new StringBuilder("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF09915155255155F4FFFFFFFFFFFF");
//        String str = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF09915155255155F4FFFFFFFFFFFF";
//        byte[] record = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 9, -111, 81, 85,
//        37, 81
//                , 85, -12, -1, -1, -1, -1, -1, -1};
//        int footerOffset = record.length - 14; // 50
//
//        int numberLength = 0xff & record[footerOffset];
//        Log.i(TAG, "insert: " + footerOffset +":" + numberLength +":" +record.length);
//        String s = PhoneNumberUtils.calledPartyBCDToString(record, footerOffset + 1, numberLength);
//        Log.i(TAG, "insert: " + s);

//        testGetIccAuthentication(telephonyManager);
    }

    public void testGetIccAuthentication(TelephonyManager telephonyManager) {
        // EAP-SIM rand is 16 bytes.
        String base64Challenge = "ECcTqwuo6OfY8ddFRboD9WM=";
        String base64Challenge2 = "EMNxjsFrPCpm+KcgCmQGnwQ=";

        try {
            String iccAuthentication = telephonyManager.getIccAuthentication(TelephonyManager.APPTYPE_USIM,
                    TelephonyManager.AUTHTYPE_EAP_AKA, "");
            Log.i(TAG, "testGetIccAuthentication: " + iccAuthentication);
//            assertNull("getIccAuthentication should return null for empty data.",
//                    iccAuthentication);
            String response = telephonyManager.getIccAuthentication(TelephonyManager.APPTYPE_USIM,
                    TelephonyManager.AUTHTYPE_EAP_SIM, base64Challenge);
            Log.i(TAG, "testGetIccAuthentication: " + response);
//            assertTrue("Response to EAP-SIM Challenge must not be Null.", response != null);
            // response is base64 encoded. After decoding, the value should be:
            // 1 length byte + SRES(4 bytes) + 1 length byte + Kc(8 bytes)
            byte[] result = android.util.Base64.decode(response, android.util.Base64.DEFAULT);
//            assertTrue("Result length must be 14 bytes.", 14 == result.length);
            Log.i(TAG, "testGetIccAuthentication: " + Arrays.toString(result));
            String response2 = telephonyManager.getIccAuthentication(TelephonyManager.APPTYPE_USIM,
                    TelephonyManager.AUTHTYPE_EAP_SIM, base64Challenge2);
            Log.i(TAG, "testGetIccAuthentication: " + response2);
//            assertTrue("Two responses must be different.", !response.equals(response2));
        } catch (SecurityException e) {
            Log.e(TAG, "testGetIccAuthentication: ", e);
        }
    }


    public void query(View view) {
        ContentResolver contentResolver = getContentResolver();
        Uri uri = Uri.parse("content://com.gusi.androidx.provider/user");
        String[] projection = {"_id", "name", "address", "sex", "auto"};
        Cursor cursor = contentResolver.query(uri, projection, null, null, null);
        Log.i(TAG, "query: " + cursor.getCount());
        while (cursor.moveToNext()) {
            StringBuilder sb = new StringBuilder();
            int idIndex = cursor.getColumnIndex("_id");
            int nameIndex = cursor.getColumnIndex("name");
            int addressIndex = cursor.getColumnIndex("address");
            int sexIndex = cursor.getColumnIndex("sex");
            int autoIndex = cursor.getColumnIndex("auto");
            Log.i(TAG, "query: " + idIndex + ":" + nameIndex + ":" + addressIndex + ":" + sexIndex + ":" + autoIndex);
            Log.i(TAG,
                    "query: " + cursor.getString(idIndex) + ":" + cursor.getString(nameIndex) + ":" + cursor.getString(addressIndex) + ":" + cursor.getString(sexIndex) + ":" + cursor.getString(autoIndex));
        }
        cursor.close();
    }


    private String getRandomStr(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(getRandomChar());
        }
        return sb.toString();
    }

    private char getRandomChar() {
        String str = "";
        int hightPos; //
        int lowPos;

        Random random = new Random();

        hightPos = (176 + Math.abs(random.nextInt(39)));
        lowPos = (161 + Math.abs(random.nextInt(93)));

        byte[] b = new byte[2];
        b[0] = (Integer.valueOf(hightPos)).byteValue();
        b[1] = (Integer.valueOf(lowPos)).byteValue();

        try {
            str = new String(b, "GBK");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "getRandomChar: ", e);
        }
        return str.charAt(0);
    }

    public void data(View view) {
        startActivity(new Intent(this, DataActivity.class));
    }

    private class MyPhoneStateListener extends PhoneStateListener {
        @Override
        public void onDataActivity(int direction) {
            super.onDataActivity(direction);
//            Log.i(TAG, "onDataActivity: " + direction);
        }
    }
}