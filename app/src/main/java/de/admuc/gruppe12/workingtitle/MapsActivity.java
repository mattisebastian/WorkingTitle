package de.admuc.gruppe12.workingtitle;

import android.app.DialogFragment;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
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

// why were these imports static?

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


    /**
     * The Google API needs this one
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        //createLocationRequest();
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


                createMarker(point);

//                Toast toast = Toast.makeText(context, text, duration);
//                toast.show();
            }
        });

    }

    private void createMarker(LatLng p) {
        // create a marker at the clicked location
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
        newFragment.show(getFragmentManager(), "createNewSpotDialog");
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

    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.getDialog().cancel();
    }
}
