package android.carrierapi.cts;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MainActivity extends Activity {

    private static final String TAG = "Ylw_Main_Cts";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        permission();
    }


    private void permission() {
        String readPhoneState = Manifest.permission.READ_PHONE_STATE;
        String modifyPhoneState = Manifest.permission.MODIFY_PHONE_STATE;
        String accessNetworkState = Manifest.permission.ACCESS_NETWORK_STATE;
        String[] permissions = {readPhoneState, accessNetworkState, modifyPhoneState};
        if (checkPermission(accessNetworkState, android.os.Process.myPid(), android.os.Process.myUid()) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(permissions, 100);
        }
    }

    private void testGetIccAuthentication(TelephonyManager telephonyManager) {
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

    public void test(View view) {
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(),
                    PackageManager.GET_SIGNATURES
                            | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS);
            Signature[] signatures = packageInfo.signatures;
            Signature signature = signatures[0];
            byte[] certHash = getCertHash(signature, "SHA-1");
            byte[] certHash256 = getCertHash(signature, "SHA-256");
            Log.i(TAG, "test: " + Arrays.toString(certHash));
            Log.i(TAG, "test: " + Arrays.toString(certHash256));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "test: ", e);
        }
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        testGetIccAuthentication(telephonyManager);
    }

    private static byte[] getCertHash(Signature signature, String algo) {
        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            return md.digest(signature.toByteArray());
        } catch (NoSuchAlgorithmException ex) {
            Log.e(TAG, "NoSuchAlgorithmException: " + ex);
        }
        return null;
    }

}