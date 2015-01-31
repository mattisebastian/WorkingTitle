package de.admuc.gruppe12.workingtitle;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class JSONSendingClient extends AsyncTask<String, Void, JSONObject>{
    SendingTaskComplete sendingTaskComplete;
    private static String title = "";
    private static LatLng point;
    private static float rating;
    private ProgressDialog progressDialog;

    public interface SendingTaskComplete{
        public void onSendingTaskComplete(JSONObject jsonResponse);

    }

    public void onSendingTaskCompleteListener(SendingTaskComplete s){
        sendingTaskComplete = s;
    }

    public JSONSendingClient(Context context, String title, LatLng point, float rating){
        this.title = title;
        this.point = point;
        this.rating = rating;
        progressDialog = new ProgressDialog(context);
    }
    private static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the BufferedReader.readLine()
         * method. We iterate until the BufferedReader return null which means
         * there's no more data to read. Each line will appended to a StringBuilder
         * and returned as String.
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }


    public static JSONObject connect(String url)
    {
        HttpClient httpclient = new DefaultHttpClient();

        // Prepare a request object
        HttpPost post= new HttpPost(url);

        try{
            List<NameValuePair> nameValuePairs = new ArrayList<>(2);
            nameValuePairs.add(new BasicNameValuePair("title", title));
            nameValuePairs.add(
                    new BasicNameValuePair("latitude", Double.toString(point.latitude)));
            nameValuePairs.add(
                    new BasicNameValuePair("longitude", Double.toString(point.longitude)));
            nameValuePairs.add(
                    new BasicNameValuePair("rating", Float.toString(rating)) {
                    });
            post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        // Execute the request
        HttpResponse response;

            response = httpclient.execute(post);
            // Examine the response status
            Log.i("Praeda",response.getStatusLine().toString());

            // Get hold of the response entity
            HttpEntity entity = response.getEntity();

            if (entity != null) {

                // A Simple JSON Response Read
                InputStream instream = entity.getContent();
                String result= convertStreamToString(instream);

                // A Simple JSONObject Creation
                JSONObject json= null;
                try {
                    json = new JSONObject(result);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Closing the input stream will trigger connection release
                instream.close();

                return json;
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void onPreExecute() {

        progressDialog.setMessage("Loading..Please wait..");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();

    }

    @Override
    protected JSONObject doInBackground(String... urls) {
        return connect(urls[0]);
    }

    @Override
    protected void onPostExecute(JSONObject json ) {
        sendingTaskComplete.onSendingTaskComplete(json);

        progressDialog.dismiss();
    }
}