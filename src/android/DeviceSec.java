package com.cordova.DeviceSec;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;

import android.content.Intent;
import android.provider.Settings;

import android.widget.Toast;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileOutputStream;


import java.lang.Process;
import java.lang.Runtime;
import java.util.Date;
import android.os.Debug;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.provider.Settings;
import com.scottyab.rootbeer.RootBeer;

/**
 * This class echoes a string called from JavaScript.
 */
public class DeviceSec extends CordovaPlugin {

    // Load the native library
    // static {
    //     System.loadLibrary("zygisk_detection");
    // }
    // // Native method declaration
    // public native int isZygiskRunning();

    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    private static final String TAG = "DeviceSec";

    private String[] binaryPaths= {
            "/data/local/",
            "/data/local/bin/",
            "/data/local/xbin/",
            "/data/local/tmp/",
            "/sbin/",
            "/su/bin/",
            "/system/bin/",
            "/system/bin/.ext/",
            "/system/bin/failsafe/",
            "/system/sd/xbin/",
            "/system/usr/we-need-root/",
            "/system/xbin/",
            "/system/app/Superuser.apk",
            "/cache",
            "/data",
            "/dev",
            "/system/"
    };

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("isTampered")) {
            // String sig = args.getString(0);
            this.isTampered(callbackContext);
            return true;
        }
        if (action.equals("isRooted")) {
            // String message = args.getString(0);
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    // Your task logic here
                    // For example, invoking a method in your plugin
                    isRooted(callbackContext);
                }
            });
            return true;
        }
        if (action.equals("isDebuggingEnabled")) {
            // String message = args.getString(0);
            this.isDebuggingEnabled(callbackContext);
            return true;
        }
        return false;
    }

    public void isTampered(CallbackContext callbackContext) {

        Context context = cordova.getActivity().getApplicationContext();
        JSONObject obj = new JSONObject();
        JSONArray sigs = new JSONArray();

        try {
            // Get the package info for the given app
            PackageManager packageManager = context.getPackageManager();
            String packageName = context.getPackageName();
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);

            // Get the signatures associated with the app
            Signature[] signatures = packageInfo.signatures;

            // Iterate through all signatures (usually there's only one)
            for (Signature signature : signatures) {
                // Get the signature as a byte array
                byte[] signatureBytes = signature.toByteArray();

                // Compute the SHA-256 hash of the signature
                String signatureHash = computeSHA256(signatureBytes);
                sigs.put(signatureHash);

                // // Compare with the expected signature hash
                // if (sig.equals(signatureHash)) {
                //     // Signature is valid
                //     // return true;
                //     obj.put("status", true);
                //     // callbackContext.success(obj);
                //     break;
                // }
            }
            
            obj.put("dh", sigs);

            // obj.put("status", false);
            callbackContext.success(obj);

        } catch (PackageManager.NameNotFoundException e) {
            // Log.e(TAG, "Package not found: " + e.getMessage());
            callbackContext.error("Package not found: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            // Log.e(TAG, "SHA-256 algorithm not found: " + e.getMessage());
            callbackContext.error("SHA-256 algorithm not found: " + e.getMessage());
        } catch (Exception e) {
            // Log.e(TAG, e.getMessage());
            callbackContext.error(e.getMessage());
        }

    }

    private static String computeSHA256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(data);

        // Convert the byte array to a hexadecimal string
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    private void isRooted(CallbackContext callbackContext) {
        
        
        Context context = cordova.getActivity().getApplicationContext();
        JSONObject obj = new JSONObject();
        RootBeer rootBeer = new RootBeer(context);

        // this.checkForBusyBoxBinary() || 
        try {
            // int result = isZygiskRunning();
            // Log.e(TAG, "zyresult "+ Integer.toString(result));
            boolean isRootbearRooted = rootBeer.isRooted();

            if (
                rootBeer.isRooted() ||
                this.checkZyPresence() ||
                this.checkForSuBinary() ||
                this.checkForMuBinary() || 
                this.checkForSuBackupBinary() || 
                this.checkSuExists() || 
                this.canUpdateRoot() ||
                this.checkForMagiskZygisk() ||
                this.canExecuteCommand("/system/xbin/su") || this.canExecuteCommand("/system/bin/su") ||
                this.canExecuteCommand("/system/bin/magisk") ||
                this.canExecuteCommand("/system/bin/magiskpolicy")) {
                obj.put("status", true);
                callbackContext.success(obj);
            } else {
                obj.put("status", false);
                callbackContext.success(obj);
            }
        } catch (Exception e) {
            Log.e(TAG, "isRooted "+ e.getMessage());
            callbackContext.error(e.toString());
        }

    }

    private boolean checkZyPresence() {
        String command = "ps | grep zygisk";

        try {
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                return true;
            }
            reader.close();

            int exitCode = process.waitFor();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean canExecuteCommand(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            int exitValue = process.waitFor();
            return exitValue == 0;
        } catch (Exception e) {
            // Exception occurred, indicating no root access
            e.printStackTrace();
            Log.e(TAG, "canExecuteCommand "+command+" " + e.getMessage());
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private boolean checkForSuBinary() {
        return checkForBinary("su"); // function is available below
    }

    private boolean checkForMuBinary() {
        return checkForBinary("mu"); // function is available below
    }

    private boolean checkForSuBackupBinary() {
        return checkForBinary("su-backup"); // function is available below
    }

    private boolean checkForBusyBoxBinary() {
        return checkForBinary("busybox");//function is available below
    }

    private boolean checkForMagiskZygisk() {
        return (checkForBinary("magisk") || checkForBinary("zygisk"));//function is available below
    }

    /**
     * @param filename - check for this existence of this 
     * file("su","busybox")
     * @return true if exists
     */
    private boolean checkForBinary(String filename) {
        for (String path : binaryPaths) {
            File f = new File(path, filename);
            boolean fileExists = f.exists();
            if (fileExists) {
                return true;
            }
        }
        return false;
    }

    /**
     * A variation on the checking for SU, this attempts a 'which su'
     * different file system check for the su binary
     * @return true if su exists
     */
    private boolean checkSuExists() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]
                    {"/system /xbin/which", "su"});
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line = in.readLine();
            process.destroy();
            return line != null;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "su exists check " + e.getMessage());
            if (process != null) {
                process.destroy();
            }
            return false;
        }
    }

    private boolean checkScriptRunning() { //unused
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]
                    {"/system /xbin/which", "su"});
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line = in.readLine();
            process.destroy();
            return line != null;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "su exists check " + e.getMessage());
            if (process != null) {
                process.destroy();
            }
            return false;
        }
    }

    private boolean canUpdateRoot() {

            try{
                Date date = new Date();
                File f = new File("/root/", "ccheck.txt");
                // boolean fileExists = f.exists();
                // if (fileExists) {

                // } else {

                // }
                if (!f.exists()) {
                    f.createNewFile();
                }
                FileOutputStream writer = new FileOutputStream(f);
                writer.write(date.toString().getBytes());
                writer.close();
                return true;
                // Log.e("TAG", "Wrote to file: "+fileName);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "canUpdateRoot " + e.getMessage());
                return false;
            }
            
    }
    
    private void isDebuggingEnabled(CallbackContext callbackContext) {
        
        Context context = cordova.getActivity().getApplicationContext();
        JSONObject obj = new JSONObject();
        try {
            obj.put("status", false);
            if (this.isUSBDebuggingEnabled(context)) {
                obj.put("status", true);
                callbackContext.success(obj);
            } else {
                obj.put("status", false);
                callbackContext.success(obj);
            }
        } catch (Exception e) {
            // callbackContext.error(e.toString());
                callbackContext.success(obj);
        }
        // Context context = cordova.getActivity().getApplicationContext();

    }

    public static boolean isUSBDebuggingEnabled(Context context) {
        int adb = Settings.Secure.getInt(context.getContentResolver(),  Settings.Global.ADB_ENABLED, 0);
        return (Debug.isDebuggerConnected() || adb == 1);
    }
}
