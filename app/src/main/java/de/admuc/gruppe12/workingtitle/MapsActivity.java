package de.admuc.gruppe12.workingtitle;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static android.content.SharedPreferences.Editor;
import static de.admuc.gruppe12.workingtitle.SpotDetailDialog.NoticeDialogListener;

/**
 * This class creates and wraps the Google Maps View.
 * It also zooms to the last known location and generates periodic updates to the users location
 */

public class MapsActivity extends FragmentActivity implements
        ConnectionCallbacks, OnConnectionFailedListener,
        LocationListener, OnInfoWindowClickListener,
        NoticeDialogListener, CreateNewSpotDialog.NoticeDialogListener {

    private static final String url = "http://mmc-xmpp.cloudapp.net/v1/pois";
    private static final String url_to_poi = "http://mmc-xmpp.cloudapp.net/v1/poi/";

    private HashMap<Marker, JSONObject> markerMap;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    /**
     * Entry point to the GoogleMaps API
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * A reference to allow only one tempmarker at a time
     */
    private Marker tempMarker = null;

    /**
     * The Google API needs this one
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void makeToast(String text) {
        Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();

    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        moveCameraToLastLocation();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        buildGoogleApiClient();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        // setup the marker map
        markerMap = new HashMap<>();

        // check for internet connection and if there is none listen for changes
        if (isNetworkAvailable()) {
            downloadPOIs();
        } else {
            registerReceiver(new ConnectivityChangeReceiver(0),
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void downloadPOIs() {

        new Thread(new Runnable() {
            public void run() {
                JSONParser jParser = new JSONParser();
                // Getting JSON from URL

                JSONArray json = (jParser.getJSONFromUrl(url));
                initiateMarkerMap(json);
            }
        }).start();
    }

    private Marker createMarker(JSONObject point) {
        String name = null;
        double latitude = 0;
        double longitude = 0;
        double rating = 0;
        try {
            name = point.getString("title");
            latitude = point.getDouble("latitude");
            longitude = point.getDouble("longitude");
            rating = point.getDouble("rating");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Marker m = mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(latitude, longitude))
                        .title(name)
                        .snippet("This spot has a rating of " +
                                (String.valueOf(String.format("%.1f", rating))))
        );
        if (!markerMap.containsKey(m)) {
            markerMap.put(m, point);
        }
        m.showInfoWindow();
        return m;

    }

    private void initiateMarkerMap(final JSONArray json) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {
                    if (json != null)
                        for (int i = 0; i < json.length(); i++) {
                            JSONObject point = json.getJSONObject(i);
                            createMarker(point);

                        }
                } catch (JSONException e) {
                    Log.e(e.getClass().getName(), e.getMessage(), e);

                }
            }
        });


    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {

        // basic ui settings
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // listen for clicks on the map, create a dialog for adding a new marker :)
        mMap.setOnMapClickListener(new OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                createTempMarker(point);
                // somehow crashes on emulator, works fine on USB device.
                new AddressLookup(MapsActivity.this.getApplicationContext()).run();
            }
        });
        mMap.setOnInfoWindowClickListener(this);

    }

    private void createTempMarker(LatLng p) {
        // create a new marker at the clicked location
        if (tempMarker != null)
            tempMarker.remove();
        tempMarker = null;
        tempMarker = mMap.addMarker(new MarkerOptions()
                .position(p)
                .title("No Address found")
                .snippet("Tap to create a new Spot."));
        // listen for clicks on the window
        mMap.setOnInfoWindowClickListener(this);
    }

    private void moveCameraToLastLocation() {
        /*
      The last location as provided by the Maps API
     */
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            CameraUpdate center =
                    CameraUpdateFactory.newLatLng(new LatLng(mLastLocation.getLatitude(),
                            mLastLocation.getLongitude()));
            CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);

            mMap.moveCamera(center);
            mMap.animateCamera(zoom);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override

    public void onLocationChanged(Location location) {
    }


    /**
     * This gets called when someone taps on the marker description
     */

    @Override
    public void onInfoWindowClick(Marker marker) {

        // check whether the marker exists already
        if (markerMap.containsKey(marker)) {
            // marker exists on the server
            // create a rating dialog here
            DialogFragment newFragment = new SpotDetailDialog();
            Bundle b = new Bundle();
            try {
                b.putString("title", markerMap.get(marker).getString("title"));
                b.putDouble("rating", markerMap.get(marker).getDouble("rating"));
                b.putInt("id", markerMap.get(marker).getInt("id"));
            } catch (JSONException e) {
                Log.e(e.getClass().getName(), e.getMessage(), e);
            }
            newFragment.setArguments(b);
            newFragment.show(getFragmentManager(), "detailDialog");
        } else {
            DialogFragment newFragment = new CreateNewSpotDialog();
            newFragment.show(getFragmentManager(), "newSpot");
        }
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, int id, String spotName, float spotRating) {

        if (dialog instanceof CreateNewSpotDialog) {
            queuePOI(spotName, spotRating, tempMarker.getPosition());

        } else if (dialog instanceof SpotDetailDialog) {
            sendRating(id, spotRating);
            Toast toast = Toast.makeText(getApplicationContext(), "Rated the spot! : " + spotRating, Toast.LENGTH_SHORT);
            toast.show();

            // works, do the actual work here (sending rating to the server) (i mean delegate it)
        }

    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.getDialog().cancel();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_search:
                // clear markers
                mMap.clear();
                removeMarkers();

                // reload POIs from server
                downloadPOIs();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void removeMarkers() {
        if (markerMap != null) {
            for (Marker m : markerMap.keySet()) {
                m.remove();
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    // helper method to store long in sharedPreferences
    Editor putDouble(final Editor edit, final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }

    // helper method to get long from sharedPreferences
    double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }

    private void queuePOI(final String spotName, final float spotRating, final LatLng pos) {

        if (isNetworkAvailable()) {
            // if connection available, send the point right away
            sendPOI(spotName, spotRating, pos);
        } else {
            // network is unavailable
            // store poi in shared preferences and send
            SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
            Editor editor = sharedPref.edit();
            editor.putString("title", spotName);

            putDouble(editor, "latitude", pos.latitude);
            putDouble(editor, "longitude", pos.longitude);
            editor.putFloat("rating", spotRating);
            editor.commit();

            registerReceiver(new ConnectivityChangeReceiver(1),
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }


    }

    private class ConnectivityChangeReceiver extends BroadcastReceiver {

        private int reason;

        public ConnectivityChangeReceiver(int reason) {
            this.reason = reason;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (reason) {
                case 0:
                    // no connection at startup
                    downloadPOIs();
                    break;
                case 1:
                    // no connection when sending POI
                    if (isNetworkAvailable()) {
                        // send the poi now
                        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                        String name = sharedPref.getString("title", "no title found in shared preferences");
                        float rating = sharedPref.getFloat("rating", 0);
                        LatLng pos = new LatLng(getDouble(sharedPref, "latitude", 0), getDouble(sharedPref, "longitude", 0));
                        sendPOI(name, rating, pos);
                        makeToast("Sent the POI now!");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Wrong argument for the ConnectivityChangedBroadcastReceiver");


            }
        }
    }


    private void sendPOI(final String spotName, final float spotRating, final LatLng pos) {

        JSONSendingClient client = new JSONSendingClient(MapsActivity.this, spotName, pos, spotRating);
        client.execute(url);
    }

    protected void sendRating(final int id, final float spotRating) {
        final Thread t = new Thread() {
            public void run() {
                Looper.prepare(); //For Preparing Message Pool for the child Thread
                HttpClient client = new DefaultHttpClient();
                HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
                HttpResponse response;
                HttpPost post = new HttpPost(url_to_poi + id);

                try {
                    List<NameValuePair> nameValuePairs = new ArrayList<>(2);
                    nameValuePairs.add(new BasicNameValuePair("rating", Float.toString(spotRating)) {
                    });

                    post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    response = client.execute(post);

                    // create a JSON Object from the response
                    String responseString = new BasicResponseHandler().handleResponse(response);

                } catch (Exception e) {
                    Log.e(e.getClass().getName(), e.getMessage(), e);
                    //("Error", "Cannot Estabilish Connection");
                }

                Looper.loop(); //Loop in the message queue
            }
        };

        t.start();
    }

    /**
     * Looks up a nearby address when placing a new marker
     * Needs the context for creating a Geocoder object
     */
    private class AddressLookup extends Thread {
        Geocoder gc;

        public AddressLookup(Context context) {
            gc = new Geocoder(context, Locale.getDefault());
        }

        @Override
        public void run() {
            try {
                if (isNetworkAvailable()) {
                    List<Address> addresses;
                    addresses = gc.getFromLocation(tempMarker.getPosition().latitude, tempMarker.getPosition().longitude, 1);
                    String address = addresses.get(0).getAddressLine(0);
                    //String city = addresses.get(0).getAddressLine(1);
                    //String country = addresses.get(0).getAddressLine(2);
                    if (address != null) {
                        tempMarker.setTitle(address);
                    }
                }
                tempMarker.showInfoWindow();
            } catch (IOException e) {
                Log.e(e.getClass().getName(), e.getMessage(), e);

            }

        }

    }

    public class JSONSendingClient extends AsyncTask<String, Void, JSONObject> {

        private String title = "";
        private LatLng point;
        private float rating;

        private ProgressDialog progressDialog;

        public JSONSendingClient(Context context, String title, LatLng point, float rating) {
            this.title = title;
            this.point = point;
            this.rating = rating;
            progressDialog = new ProgressDialog(context);
        }

        private String convertStreamToString(InputStream is) {
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


        public JSONObject connect(String url) {
            HttpClient httpclient = new DefaultHttpClient();

            // Prepare a request object
            HttpPost post = new HttpPost(url);

            try {
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
                Log.i("Praeda", response.getStatusLine().toString());

                // Get hold of the response entity
                HttpEntity entity = response.getEntity();

                if (entity != null) {

                    // A Simple JSON Response Read
                    InputStream instream = entity.getContent();
                    String result = convertStreamToString(instream);

                    // A Simple JSONObject Creation
                    JSONObject json = null;
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

            progressDialog.setMessage("Sending your Spot to the server. Please wait..");
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();

        }

        @Override
        protected JSONObject doInBackground(String... urls) {
            return connect(urls[0]);
        }

        @Override
        protected void onPostExecute(JSONObject json) {
            createMarker(json);
            tempMarker = null;
            progressDialog.dismiss();
        }
    }


}

