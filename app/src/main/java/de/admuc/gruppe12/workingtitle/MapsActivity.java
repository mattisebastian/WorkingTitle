package de.admuc.gruppe12.workingtitle;

import android.app.DialogFragment;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This class creates and wraps the Google Maps View.
 * It also zooms to the last known location and generates periodic updates to the users location
 */

public class MapsActivity extends FragmentActivity implements
        ConnectionCallbacks, OnConnectionFailedListener,
        LocationListener, OnInfoWindowClickListener,
        CreateNewSpotDialog.NoticeDialogListener {

    public static final String TEMP_MARKER_LAT = "de.admuc.gruppe12.workingtitle.TEMP_MARKER_LAT";
    public static final String TEMP_MARKER_LONG = "de.admuc.gruppe12.workingtitle.TEMP_MARKER_LONG";
    static final LatLng KIEL = new LatLng(53.551, 9.993);
    private static String url = "http://mmc-xmpp.cloudapp.net/v1/pois";

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    /**
     * Entry point to the GoogleMaps API
     */
    private GoogleApiClient mGoogleApiClient;
    /**
     * The last location as provided by the Maps API
     */
    private Location mLastLocation;

    /**
     * A reference to allow only one tempmarker at a time
     */
    private Marker tempMarker = null;

    private int id;
    private float rating;
    private String title;
    private LatLng latLng;

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
        downloadPOIs();

    }

    private void downloadPOIs() {
        new FetchDataAsync().execute();

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
        //mMap.setMyLocationEnabled(true);
        //mMap.getUiSettings().setZoomControlsEnabled(true);
        Marker kiel = mMap.addMarker(new MarkerOptions()
                .position(KIEL)
                .title("Kiel")
                .snippet("Kiel is cool")
                .icon(BitmapDescriptorFactory
                        .fromResource(R.drawable.ic_launcher)));

        // listen for clicks on the map, create a dialog for adding a new marker :)
        mMap.setOnMapClickListener(new OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {

                Context context = getApplicationContext();
                CharSequence text = "Hello toast! Pos: " + point.latitude + ":" + point.longitude;
                int duration = Toast.LENGTH_SHORT;
                createTempMarker(point);

//                Toast toast = Toast.makeText(context, text, duration);
//                toast.show();
            }
        });

    }

    private void createTempMarker(LatLng p) {
        // create a new marker at the clicked location
        if (tempMarker != null)
            tempMarker.remove();
        tempMarker = null;
        tempMarker = mMap.addMarker(new MarkerOptions()
                .position(p)
                .title("TempMarker")

                        // TODO: try to guess address of marker
                .snippet("Tap to create a new Spot."));
        // listen for clicks on the window
        mMap.setOnInfoWindowClickListener(this);
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(tempMarker.getPosition().latitude, tempMarker.getPosition().longitude, 1);
            String address = addresses.get(0).getAddressLine(0);
            //String city = addresses.get(0).getAddressLine(1);
            //String country = addresses.get(0).getAddressLine(2);
            if (address != null) {
                tempMarker.setTitle(address);
            }
            tempMarker.showInfoWindow();
        } catch (IOException e) {
            e.printStackTrace();

        }



    }

    private void moveCameraToLastLocation() {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
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
        // create the "new spot" mask here
        DialogFragment newFragment = new CreateNewSpotDialog();
        newFragment.show(getFragmentManager(), "newSpot");
    }

    /**
     * User confirmed the creation of a new spot
     *
     * @param dialog
     * @param spotName
     * @param spotRating
     */

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String spotName, float spotRating) {

        Toast toast = Toast.makeText(getApplicationContext(), spotName + " rated: " + spotRating, Toast.LENGTH_SHORT);
        toast.show();
        sendJson(spotName, spotRating, tempMarker.getPosition());

    }

    /**
     * Try sending data to the server
     * @param spotName
     * @param spotRating
     */
    protected void sendJson(final String spotName, final float spotRating, final LatLng pos) {
        final Thread t = new Thread() {

            public void run() {
                Looper.prepare(); //For Preparing Message Pool for the child Thread
                HttpClient client = new DefaultHttpClient();
                HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); //Timeout Limit
                HttpResponse response;
                HttpPost post = new HttpPost("http://mmc-xmpp.cloudapp.net/v1/pois");

                try {
                    List<NameValuePair> nameValuePairs = new ArrayList<>(2);
                    nameValuePairs.add(new BasicNameValuePair("title", spotName));
                    nameValuePairs.add(new BasicNameValuePair("latitude", Double.toString(pos.latitude)));
                    nameValuePairs.add(new BasicNameValuePair("longitude", Double.toString(pos.longitude)));
                    nameValuePairs.add(new BasicNameValuePair("rating", Float.toString(spotRating)) {
                    });

                    post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                    response = client.execute(post);

                    // create a JSON Object from the response
                    String responseString = new BasicResponseHandler().handleResponse(response);
                    JSONObject jsonOb = new JSONObject(responseString);

                    Toast toast = Toast.makeText(getApplicationContext(), jsonOb.get("title").toString(), Toast.LENGTH_LONG);
                    toast.show();

                } catch (Exception e) {
                    Log.e(e.getClass().getName(), e.getMessage(), e);
                    //("Error", "Cannot Estabilish Connection");
                }

                Looper.loop(); //Loop in the message queue
            }
        };

        t.start();
    }


    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.getDialog().cancel();
    }

    /**
     * Created by matti on 22.01.2015.
     */
    private class FetchDataAsync extends AsyncTask<String, String, JSONObject> {

        private static final String TAG_ID = "id";
        private static final String TAG_TITLE = "title";
        private static final String TAG_LAT = "latitude";
        private static final String TAG_LONG = "longitude";
        private static final String TAG_RATING = "rating";

        //    public FetchDataAsync(Activity main){
//        this.main = main;
//    }
//    private ProgressDialog progressDialog = new ProgressDialog(this);
//        InputStream inputStream = null;
//        String result = "";

        protected void onPreExecute() {
//        progressDialog.setMessage("Downloading your data...");
//        progressDialog.show();
//        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
//            public void onCancel(DialogInterface arg0) {
//                FetchDataAsync.this.cancel(true);
//            }
//        });
        }

        @Override
        protected JSONObject doInBackground(String... params) {

            JSONParser jParser = new JSONParser();
            // Getting JSON from URL
            JSONObject json = null;
            try {
                json = (jParser.getJSONFromUrl(url)).getJSONObject(0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;

        } // protected Void doInBackground(String... params)

        protected void onPostExecute(JSONObject json) {
            try {
                if (json != null) {
                    id = json.getInt(TAG_ID);
                    rating = json.getLong(TAG_RATING);
                    title = json.getString(TAG_TITLE);
                    latLng = new LatLng(json.getLong(TAG_LAT), json.getLong(TAG_LONG));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
//            createMarker(latLng);
//            Toast a = new Toast(MapsActivity.this);
//            a.setText(latLng.toString());

        } // protected void onPostExecute(Void v)
    } //class MyAsyncTask extends AsyncTask<String, String, Void>

}

