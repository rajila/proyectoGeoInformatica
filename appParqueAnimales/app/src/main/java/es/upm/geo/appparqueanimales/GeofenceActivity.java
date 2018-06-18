package es.upm.geo.appparqueanimales;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationBuilderWithBuilderAccessor;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.carto.core.MapPos;
import com.carto.core.Variant;
import com.carto.geometry.FeatureCollection;
import com.carto.geometry.GeoJSONGeometryReader;
import com.carto.geometry.LineGeometry;
import com.carto.geometry.MultiGeometry;
import com.carto.geometry.PointGeometry;
import com.carto.geometry.PolygonGeometry;
import com.carto.projections.Projection;
import com.carto.styles.BillboardOrientation;
import com.carto.styles.GeometryCollectionStyleBuilder;
import com.carto.styles.LineStyleBuilder;
import com.carto.styles.PointStyleBuilder;
import com.carto.styles.PolygonStyleBuilder;
import com.carto.styles.TextStyleBuilder;
import com.carto.ui.MapView;
import com.carto.vectorelements.GeometryCollection;
import com.carto.vectorelements.Point;
import com.carto.vectorelements.Text;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.internal.Objects;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.security.acl.NotOwnerException;
import java.util.ArrayList;
import java.util.Random;

public class GeofenceActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener{

    private GoogleMap _mMap;
    SupportMapFragment mapFragment;

    private static final int MY_PERMISSION_REQUEST_CODE = 7192;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 300193;

    private LocationRequest _mLocationRequest;
    private GoogleApiClient _mGoogleApiClient;
    private Location _mLastLocation;

    private static int UPDATE_INTERVAL = 5000;
    private static int FASTET_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    DatabaseReference _ref;
    GeoFire _geoFire;

    Marker _mMarker;
    private MapView _mapView;
    Projection _projection;
    ArrayList<Variant> dataGeojson = new ArrayList<Variant>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geofence);

        _mapView = (MapView) this.findViewById(R.id.mapView);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.g_map);
        mapFragment.getMapAsync(this);

        _ref = FirebaseDatabase.getInstance().getReference("MyLocation");
        _geoFire = new GeoFire(_ref);

        // Verificamos que haya internet
        if( !verificarInternet() ) return;
        
        sepUplocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case MY_PERMISSION_REQUEST_CODE:
                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    if(checkPlayServices())
                    {
                        buildGoogleApxiClient();
                        createLocationRequest();
                        displayLocation();
                    }
                }
                break;
        }
    }

    /**
     * Función que actualiza la ubicación actual del dispositivo
     */
    private void sepUplocation() {
        if( ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_REQUEST_CODE);
        }else{
            if(checkPlayServices())
            {
                buildGoogleApxiClient();
                createLocationRequest();
                displayLocation();
            }
        }
    }

    /**
     * Función en la que definimos la localizacion actual
     * Para este caso en esta parte asignamos una localizacion personalizada para mostrar el mensaje de GEOFENCE
     */
    private void displayLocation() {
        if( ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        _mLastLocation = LocationServices.FusedLocationApi.getLastLocation(_mGoogleApiClient);
        if( _mLastLocation != null)
        {
            // Ubicación automatica
            //final double _la = _mLastLocation.getLatitude();
            //final double _lg = _mLastLocation.getLongitude();

            // Localización de Caceres
            //final double _la = 39.47;
            //final double _lg =-6.37;

            // Localización de parque
            final double _la = 39.459682;
            final double _lg =-6.381063;


            _geoFire.setLocation("You", new GeoLocation(_la, _lg), new GeoFire.CompletionListener() {
                @Override
                public void onComplete(String key, DatabaseError error) {
                    if(_mMarker != null) _mMarker.remove();
                    _mMarker = _mMap.addMarker(new MarkerOptions()
                                                .position(new LatLng(_la,_lg))
                                                .title("Youu"));
                    _mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(_la,_lg),12.0f));
                }
            });

            Log.i("OK","ok");
        }
    }

    /**
     * Configuramos el intervalo de actualizacion de la Localizacion
     */
    private void createLocationRequest() {
        _mLocationRequest = new LocationRequest();
        _mLocationRequest.setInterval(UPDATE_INTERVAL);
        _mLocationRequest.setFastestInterval(FASTET_INTERVAL);
        _mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        _mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    /**
     * Configuramos los parametros del Cliente de Google
     */
    private void buildGoogleApxiClient() {
        _mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        _mGoogleApiClient.connect();
    }

    /**
     * Verificamos que nuestro dispositivo tenga una verion de Play Store que soporte la utilizaocion de Mapas
     * @return
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if(resultCode != ConnectionResult.SUCCESS)
        {
            if( GooglePlayServicesUtil.isUserRecoverableError(resultCode))
            {
                GooglePlayServicesUtil.getErrorDialog(resultCode,this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            }else{
                Toast.makeText(this,"Este dispositivo no soporta Google Maps", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        _mMap = googleMap;
        loadDataParque();
        Log.i("DIM-LISTA", "DIM-LISTA: "+dataGeojson.size());
        try {
            Thread.sleep(3000); // Esperamos 3 segundos hasta que la información de los parques sea cargada
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Creamos las diferentes areas para la funcionalidad de Geofence
        for ( Variant _data: dataGeojson) {
            Log.i("Visitas", "Visitas: "+_data.getObjectElement("visitas").toString());
            addStylPoint(Integer.parseInt(_data.getObjectElement("visitas").toString()), _data);
        }
        Log.i("DIM-LISTA", "DIM-LISTA: "+dataGeojson.size());
    }

    /**
     * Cargamos los parques de la base de datos GEOJSON , haciendo uso de las librerias de Carto.js
     */
    private void loadDataParque() {
        _projection = _mapView.getOptions().getBaseProjection();

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                GeoJSONGeometryReader reader = new GeoJSONGeometryReader();
                reader.setTargetProjection(_projection);
                String fileName = "parque.geojson"; // base de datos
                String json;

                try {
                    InputStream is = getAssets().open(fileName);
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();

                    json = new String(buffer, "UTF-8");

                } catch (IOException ex) {
                    Log.i("ERROR-FILE", "ERROR-FILE: "+ex.getMessage());
                    return;
                }

                FeatureCollection features = reader.readFeatureCollection(json);
                // SOLO VAMOS A MOSTRAR 15 PARQUES
                for (int i = 0; i < 15; i++) {
                    if (features.getFeature(i).getGeometry() instanceof PointGeometry) {
                        Variant _pro = features.getFeature(i).getProperties(); // Obtenemos las propiedades de la base de datos
                        dataGeojson.add(_pro);
                    }else{
                        Log.i("SIN-GEOMETRIA", "SIN-GEOMETRIA: "+features.getFeature(i).getGeometry());
                    }
                }
            }
        });
        thread.start(); // TODO: should serialize execution
    }

    /**
     * Función que crear el area Geofence para cada parque.
     * Se usa la tecnologia FireBase
     * Se le pasa los datos de los parques y tambien un estilo de color en base a al numero de visistas
     * @param _pro
     * @param red
     * @param green
     * @param blue
     */
    private void addAreaGeofence(Variant _pro, int red, int green, int blue)
    {
        // capturamos el nombre del parque
        final String _nameParque = _pro.getObjectElement("foaf_name").toString().replace("\"","");

        // Obtenemos la latitud y longitud del parque
        Log.i("LA", "LT: "+_pro.getObjectElement("geo_lat").toString().replace("\"",""));
        Log.i("LG", "LG: "+_pro.getObjectElement("geo_long").toString().replace("\"",""));

        // Creamos la ubicacion
        LatLng _area = new LatLng(Double.parseDouble(_pro.getObjectElement("geo_lat").toString().replace("\"","")),
                Double.parseDouble(_pro.getObjectElement("geo_long").toString().replace("\"","")));

        _mMap.addCircle(new CircleOptions()
                .center(_area)
                .radius(100) // 100 metros
                .strokeColor(android.graphics.Color.argb(250, 185, 80, 115))
                .fillColor(android.graphics.Color.argb(80, red, green, blue))
                .strokeWidth(5.0f));

        // Se crea la funcionalidad de Geofence haciendo uso de los servicios de FireBase
        //0.1f --> 100 metros
        GeoQuery _geoQuery = _geoFire.queryAtLocation(new GeoLocation(_area.latitude,_area.longitude),0.1f);
        _geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                // Invocamos el envio de notificaciones
                sendNotification("Park Animals", String.format("Estas dentro del area: %s",_nameParque));
            }

            @Override
            public void onKeyExited(String key) {
                //sendNotification("Park Animals:", String.format("has salio del area: %s",key));
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                Log.d("onKeyMoved","onKeyMoved");
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                Log.e("ERROR","onGeoQueryError");
            }
        });
    }

    /**
     * Función que asigna los estilos a cada distrito. Para esto hay que conocer el código de cada distrito de madrid
     * Para este ejemplo se ha definido un archivo geojson ya conocido
     * @param _pro
     * @param numVisitas
     */
    public void addStylPoint(int numVisitas, Variant _pro)
    {
        switch (numVisitas) {
            case 500:
                addAreaGeofence(_pro,255,198,196);
                break;
            case 1000:
                addAreaGeofence(_pro,242,156,163);
                break;
            case 1500:
                addAreaGeofence(_pro,218,116,137);
                break;
            case 2000:
                addAreaGeofence(_pro, 185,80,115);
                break;
            case 2500:
                addAreaGeofence(_pro, 147,52,93);
                break;
            case 3000:
                addAreaGeofence(_pro,103,32,68);
                break;
            default:
                break;
        }
    }

    /**
     * Función que se encarga de enviar las notificaciones
     * @param title
     * @param content
     */
    private void sendNotification(String title, String content)
    {
        Toast.makeText(this,content,Toast.LENGTH_LONG).show();
        Log.i("NOTIFICACION","MSN: "+title+" "+content);

        // Envio de Notificación: Pero no me ha funcionado
        NotificationCompat.Builder _builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(content);
        NotificationManager _manager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent _intent = new Intent(this, GeofenceActivity.class);
        PendingIntent _contentIntent = PendingIntent.getActivity(this,0,_intent,PendingIntent.FLAG_IMMUTABLE);
        _builder.setContentIntent(_contentIntent);
        Notification _notification = _builder.build();

        _manager.notify(1,_notification);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if( ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;
        LocationServices.FusedLocationApi.requestLocationUpdates(_mGoogleApiClient,_mLocationRequest,this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        _mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        _mLastLocation = location;
        displayLocation();
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
}
