package es.upm.geo.appparqueanimales;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Toast;

import com.carto.core.MapPos;
import com.carto.datasources.LocalVectorDataSource;
import com.carto.graphics.Color;
import com.carto.layers.CartoBaseMapStyle;
import com.carto.layers.CartoOnlineVectorTileLayer;
import com.carto.layers.VectorLayer;
import com.carto.projections.Projection;
import com.carto.styles.LineStyleBuilder;
import com.carto.styles.PointStyleBuilder;
import com.carto.styles.PolygonStyle;
import com.carto.styles.PolygonStyleBuilder;
import com.carto.ui.MapView;
//import com.carto.utils.Log;
import com.carto.vectorelements.Point;
import com.carto.vectorelements.Polygon;
import com.carto.core.MapPosVector;

import android.util.Log;

public class LocalizarParkActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private LocationManager locationManager;
    private LocationListener locationListener;
    LocalVectorDataSource vectorDataSource;

    final String LICENSE = "XTUN3Q0ZDSHJJTDNFZ2k3TGZxRllFNWt0YzJDRnV6b09BaFJLQ1pEY1V6em9pMERjcmp5VCthNkRjMC92Y3c9PQoKYXBwVG9rZW49ZjJkZmIzYjgtMjMxYS00ZGY1LTg1NWUtN2YwNTcxZTcxMmMwCnBhY2thZ2VOYW1lPWVzLnVwbS5nZW8uYXBwcGFycXVlYW5pbWFsZXMKb25saW5lTGljZW5zZT0xCnByb2R1Y3RzPXNkay1hbmRyb2lkLTQuKgp3YXRlcm1hcms9Y2FydG9kYgo=";
    final int ALPHARGB = 255;
    final int GENRED = 102;
    final int GENGREEN = 102;
    final int GENBLUE = 102;

    private MapView _mapView;
    LocalVectorDataSource vectorDataSource1;
    LocalVectorDataSource _source;
    Projection _projection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localizar_park);

        MapView.registerLicense(LICENSE, this);
        _mapView = (MapView) this.findViewById(R.id.mapView);

        // Create basemap layer with bundled style
        CartoOnlineVectorTileLayer baseLayer = new CartoOnlineVectorTileLayer(CartoBaseMapStyle.CARTO_BASEMAP_STYLE_DARKMATTER);

        _mapView.getLayers().add(baseLayer);

        _projection = _mapView.getOptions().getBaseProjection();

        // 1. Initialize an local vector data source
        vectorDataSource = new LocalVectorDataSource(_projection);

        // 2. Initialize a vector layer with the previous data source
        VectorLayer vectorLayer = new VectorLayer(vectorDataSource);

        // 3. Add the previous vector layer to the map
        _mapView.getLayers().add(vectorLayer);

        if( !checkPermissions() ) requestPermissionsCustom();
        if( !verificarInternet() ) return;

        onPermissionGranted();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if( !checkPermissions() ) requestPermissionsCustom();
        else if ( !verificarInternet() )  return;
        onPermissionGranted();
    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onPermissionGranted();
                } else {
                    finish();
                }
            }
        }
    }

    void onPermissionGranted() {

        Color lightAppleBlue = Colors.toCartoColor(Colors.LIGHT_TRANSPARENT_APPLE_BLUE);
        Color darkAppleBlue = Colors.toCartoColor(Colors.DARK_TRANSPARENT_APPLE_BLUE);

        // Style for GPS My Location circle
        PolygonStyleBuilder polygonBuilder = new PolygonStyleBuilder();
        polygonBuilder.setColor(lightAppleBlue);

        // Add a nice darker border to our accuracy marker
        LineStyleBuilder borderBuilder = new LineStyleBuilder();
        borderBuilder.setColor(darkAppleBlue);
        borderBuilder.setWidth(1);
        polygonBuilder.setLineStyle(borderBuilder.buildStyle());

        final Polygon accuracyMarker = new Polygon(new MapPosVector(), polygonBuilder.buildStyle());

        PointStyleBuilder pointBuilder = new PointStyleBuilder();
        pointBuilder.setColor(darkAppleBlue);
        final Point userMarker = new Point(new MapPos(), pointBuilder.buildStyle());

        // Initially empty and invisible
        accuracyMarker.setVisible(false);
        userMarker.setVisible(false);

        locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {

                accuracyMarker.setPoses(Utils.createLocationCircle(location, _projection));
                accuracyMarker.setVisible(true);

                MapPos position = new MapPos(location.getLongitude(), location.getLatitude());
                position = _projection.fromWgs84(position);

                userMarker.setPos(position);
                userMarker.setVisible(true);

                _mapView.setFocusPos(position, 3);
                _mapView.setZoom(16, 3);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) { }
            @Override
            public void onProviderEnabled(String s) { }
            @Override
            public void onProviderDisabled(String s) { }
        };

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // User has maybe disabled location services / GPS
        if (locationManager.getProviders(true).size() == 0) {
            Log.i("ERROR-LOCATION", "ERROR-LOCATION: ");
        }

        // Use all enabled device providers with same parameters
        for (String provider : locationManager.getProviders(true)) {

            int fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            int coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

            if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            locationManager.requestLocationUpdates(provider, 1000, 50, locationListener);
        }

        vectorDataSource.add(accuracyMarker);
        vectorDataSource.add(userMarker);
    }

    @Override
    public void onDestroy() {
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
        super.onDestroy();
    }

    private void requestPermissionsCustom() {
        // Verifica si el usuario ha denegado el acceso a la localización
        // True --> Da denegado el acceso a la localización
        // False --> No ha contestado
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);

        Log.i(TAG, "RESPUESTA USUARIO -> "+shouldProvideRationale);
        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "F0001 -> Mensaje: El usuario no ha permitido el acceso a la localización.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(LocalizarParkActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "F0002 -> Mensaje: Pregunta al usuario para el uso de localización");
            ActivityCompat.requestPermissions(LocalizarParkActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    private boolean isNetDisponible() {

        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo actNetInfo = connectivityManager.getActiveNetworkInfo();

        return (actNetInfo != null && actNetInfo.isConnected());
    }

    public Boolean isOnlineNet() {

        try {
            Process p = java.lang.Runtime.getRuntime().exec("ping -c 1 www.google.es");
            int val           = p.waitFor();
            boolean reachable = (val == 0);
            return reachable;

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    public boolean verificarInternet()
    {
        boolean _r = true;
        if(!(isNetDisponible() && isOnlineNet()))
        {
            _r = false;
            showSnackbar(R.string.error_internet,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {}
                    });
        }
        return _r;
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
}