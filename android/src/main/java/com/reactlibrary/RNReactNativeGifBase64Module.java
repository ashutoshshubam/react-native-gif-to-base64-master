
package com.reactlibrary;

import com.facebook.react.ReactActivity;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.PermissionListener;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.waynejo.androidndkgif.GifDecoder;
import com.waynejo.androidndkgif.GifEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

public class RNReactNativeGifBase64Module extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    private ArrayList<Bitmap> mFacesBitmapArray = new ArrayList<>();

    private static final int MY_PERMISSIONS_REQUEST = 1001;

    protected Callback callback;
    private ReadableMap options;

    private ResponseHelper responseHelper = new ResponseHelper();

    public RNReactNativeGifBase64Module(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNReactNativeGifBase64";
    }


    private boolean permissionsCheck(@NonNull final Activity activity,
                                     @NonNull final Callback callback,
                                     @NonNull final int requestCode)
    {
        final int writePermission = ActivityCompat
                .checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        final boolean permissionsGrated = writePermission == PackageManager.PERMISSION_GRANTED ;

        if (!permissionsGrated)
        {
            final Boolean dontAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA);

            if (dontAskAgain)
            {

                return false;
            }
            else
            {
                String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
                if (activity instanceof ReactActivity)
                {
                    ((ReactActivity) activity).requestPermissions(PERMISSIONS,MY_PERMISSIONS_REQUEST,listener);
                }

                return false;
            }
        }
        return true;
    }

    private PermissionListener listener = new PermissionListener()
    {
        public boolean onRequestPermissionsResult(final int requestCode,
                                                  @NonNull final String[] permissions,
                                                  @NonNull final int[] grantResults)
        {
            boolean permissionsGranted = true;
            for (int i = 0; i < permissions.length; i++)
            {
                final boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                permissionsGranted = permissionsGranted && granted;
            }

            if (callback == null || options == null)
            {
                return false;
            }

            if (!permissionsGranted)
            {
                responseHelper.invokeError(callback, "Permissions weren't granted");
                return false;
            }

            switch (requestCode)
            {
                case MY_PERMISSIONS_REQUEST:
                    processGIF(options, callback);
                    break;

            }
            return true;
        }
    };

    @ReactMethod
    public void getBase64String(final ReadableMap options, final Callback callback) {

        if (options == null)
        {
            responseHelper.invokeError(callback, "Invalid Data");
            return;
        }

        this.callback = callback;
        this.options = options;

        if (!permissionsCheck(getCurrentActivity(), callback, MY_PERMISSIONS_REQUEST))
        {
            return;
        }

        new LongOperation(options ,callback).execute();

    }

    public String processGIF(final ReadableMap options, final Callback callback) {

        ArrayList mFacesURLArray     =  options.getArray("faceArr").toArrayList();
        ArrayList mGifArray         =   options.getArray("gifArr").toArrayList();

        if (mFacesURLArray.size() == 0 || mGifArray == null || mGifArray.size() == 0) {
            responseHelper.invokeError(callback, "Invalid Data");
        }

        downloadFaces(mFacesURLArray);

        HashMap gifDataObj = (HashMap) mGifArray.get(0);

        String url = gifDataObj.get("url_gif").toString();
        String gif_id = gifDataObj.get("giphy_id").toString();

        String downloadedGifPath = downloadGifAndGetPath(url, gif_id);

        String gif_path = createNewGif(new JSONObject(gifDataObj), downloadedGifPath);

        return gif_path;

    }


    private String createNewGif(JSONObject gifDataObj, String downloadedGifPath) {
        try {
            GifDecoder gifDecoder = new GifDecoder();
            boolean isSucceeded = gifDecoder.load(downloadedGifPath);
            if (isSucceeded) {
                GifEncoder gifEncoder = new GifEncoder();
                gifEncoder.init(gifDecoder.frame(0).getWidth(),
                        gifDecoder.frame(0).getHeight(), downloadedGifPath, GifEncoder.EncodingType.ENCODING_TYPE_NORMAL_LOW_MEMORY);
                JSONArray maps = gifDataObj.optJSONArray("maps");
                double ratio = gifDataObj.optDouble("ratio");
                if (maps != null) {

                    if (maps.length() != gifDecoder.frameNum() || maps.length() == 0) {
                        //TODO:
                    }

                    Bitmap overlayedBitmap;

                    for (int i = 0; i < gifDecoder.frameNum(); i++) {
                        ArrayList<JSONObject> filteredFrameArray = new ArrayList<>();
                        for (int j = 0; j < maps.length(); j++) {
                            JSONObject mapsFrameObj = (JSONObject) maps.get(j);
                            int frame_number = mapsFrameObj.optInt("frame_number");
                            if (frame_number == i) {
                                filteredFrameArray.add(mapsFrameObj);
                            }
                        }
                        overlayedBitmap = createOverlayBitmapNew(filteredFrameArray, gifDecoder.frame(i), ratio);

                        gifEncoder.encodeFrame(overlayedBitmap, gifDecoder.delay(i));
                    }
                }
                gifEncoder.close();
                return downloadedGifPath;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private Bitmap createOverlayBitmapNew(ArrayList<JSONObject> filteredFrameArray, Bitmap baseBitmap, double ratio) {
        Bitmap bmOverlay = Bitmap.createBitmap(baseBitmap.getWidth(), baseBitmap.getHeight(), baseBitmap.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(baseBitmap, new Matrix(), new Paint(Paint.FILTER_BITMAP_FLAG));
        for (JSONObject frame : filteredFrameArray) {
            double x = frame.optDouble("x");
            double y = frame.optDouble("y");
            double zoom = frame.optDouble("zoom");
            double angle = frame.optDouble("angle");
            double newWidth = 400 * ratio * (zoom / 100);
            double newHeight = 400 * ratio * (zoom / 100);

            int face_number = frame.optInt("face_number");

            // validate face should exist in the faces array

            if(mFacesBitmapArray.size() > face_number ){

                Bitmap faceBitmap = mFacesBitmapArray.get(frame.optInt("face_number"));
                Matrix matrix = new Matrix();
                matrix.setRotate((float) angle);
                Bitmap scaledFaceBitmap = Bitmap.createScaledBitmap(faceBitmap, (int) newWidth, (int) newHeight, true);
                Bitmap finalFaceBitmap = Bitmap.createBitmap(scaledFaceBitmap, 0, 0, (int) newWidth, (int) newHeight, matrix, true);

                x = x - (finalFaceBitmap.getWidth() / 2);
                y = y - (finalFaceBitmap.getHeight() / 2);

                canvas.drawBitmap(finalFaceBitmap, (float) x, (float) y, new Paint(Paint.FILTER_BITMAP_FLAG));

            }

        }
        return bmOverlay;
    }


    //    -------------------------------------UTILITY----------------------------------

    private class LongOperation extends AsyncTask<Void, Void, String> {

        ReadableMap options;
        Callback callback;

        public LongOperation(ReadableMap options, Callback callback){

            this.options = options;
            this.callback = callback;
        };

        protected String doInBackground(Void... params) {

            return processGIF(options,callback);

        }

        protected void onPostExecute(String result) {

            String base64 = getBase64StringFromFile(result);
            responseHelper.putString("base64",base64);
            responseHelper.putString("path", result);
            responseHelper.invokeResponse(callback);
            responseHelper.cleanResponse();
        }

    }

    // Convert Image to Base64

    private String getBase64StringFromFile(String absoluteFilePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File(absoluteFilePath));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        byte[] bytes;
        byte[] buffer = new byte[8192];
        int bytesRead;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        bytes = output.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    // Download GIF from URL save to directory

    private String downloadGifAndGetPath(String strUrl, String gifName) {
        URL url;
        try {
            url = new URL(strUrl);
            URLConnection conection = url.openConnection();
            conection.connect();
            // download the file
            InputStream input = new BufferedInputStream(url.openStream());
            // Output stream
            OutputStream output;
            String filePath = Environment
                    .getExternalStorageDirectory().getAbsolutePath() + "/gifFolder/"
                    + gifName;
            output = new FileOutputStream(filePath);
            byte data[] = new byte[4096];
            int count;
            while ((count = input.read(data)) != -1) {
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
            return filePath;
        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
        }
        return null;
    }


    private void downloadFaces(ArrayList facesUrl) {
        for (int i = 0; i < facesUrl.size(); i++) {
            HashMap url = (HashMap)facesUrl.get(i);

            downloadFile("FACE-" + i, url.get("url").toString());
        }
    }

    private void downloadFile(String fileName, String downloadUrl) {

        Ion.with(this.reactContext).load(downloadUrl).withBitmap().asBitmap()
                .setCallback(new FutureCallback<Bitmap>() {
                    @Override
                    public void onCompleted(Exception e, Bitmap result) {
                        // do something with your bitmap
                        mFacesBitmapArray.add(result);
                    }
                });
    }


    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}

class ResponseHelper
{
    private WritableMap response = Arguments.createMap();

    public void cleanResponse()
    {
        response = Arguments.createMap();
    }

    public @NonNull
    WritableMap getResponse()
    {
        return response;
    }

    public void putString(@NonNull final String key,
                          @NonNull final String value)
    {
        response.putString(key, value);
    }

    public void putInt(@NonNull final String key,
                       final int value)
    {
        response.putInt(key, value);
    }

    public void putBoolean(@NonNull final String key,
                           final boolean value)
    {
        response.putBoolean(key, value);
    }

    public void putDouble(@NonNull final String key,
                          final double value)
    {
        response.putDouble(key, value);
    }

    public void invokeCustomButton(@NonNull final Callback callback,
                                   @NonNull final String action)
    {
        cleanResponse();
        response.putString("customButton", action);
        invokeResponse(callback);
    }

    public void invokeCancel(@NonNull final Callback callback)
    {
        cleanResponse();
        response.putBoolean("didCancel", true);
        invokeResponse(callback);
    }

    public void invokeError(@NonNull final Callback callback,
                            @NonNull final String error)
    {
        cleanResponse();
        response.putString("error", error);
        invokeResponse(callback);
    }

    public void invokeResponse(@NonNull final Callback callback)
    {
        callback.invoke(response);
    }
}
