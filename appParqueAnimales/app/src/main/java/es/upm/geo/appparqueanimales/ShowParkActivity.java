package es.upm.geo.appparqueanimales;


import com.carto.geometry.GeoJSONGeometryReader;
import com.carto.geometry.LineGeometry;
import com.carto.geometry.MultiGeometry;
import com.carto.geometry.PolygonGeometry;
import com.carto.layers.Layer;
import com.carto.layers.VectorTileEventListener;
import com.carto.layers.VectorTileLayer;
import com.carto.styles.GeometryCollectionStyleBuilder;
import com.carto.styles.LineStyleBuilder;
import com.carto.styles.PolygonStyleBuilder;
import com.carto.vectorelements.GeometryCollection;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


import com.carto.core.MapPos;
import com.carto.core.MapRange;
import com.carto.core.Variant;
import com.carto.datasources.LocalVectorDataSource;
//import com.nutiteq.datasources.LocalVectorDataSource;
import com.carto.geometry.FeatureCollection;
import com.carto.geometry.PointGeometry;
import com.carto.graphics.Color;
import com.carto.layers.CartoBaseMapStyle;
import com.carto.layers.CartoOnlineVectorTileLayer;
import com.carto.layers.VectorLayer;
import com.carto.projections.Projection;
import com.carto.styles.BillboardOrientation;
import com.carto.styles.PointStyleBuilder;
import com.carto.styles.TextStyleBuilder;
import com.carto.ui.MapView;
import com.carto.vectorelements.Point;
import com.carto.vectorelements.Text;


import java.io.IOException;
import java.io.InputStream;

public class ShowParkActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_show_park);

        MapView.registerLicense(LICENSE, this);
        _mapView = (MapView) this.findViewById(R.id.mapView);

        // Create basemap layer with bundled style
        CartoOnlineVectorTileLayer baseLayer = new CartoOnlineVectorTileLayer(CartoBaseMapStyle.CARTO_BASEMAP_STYLE_VOYAGER);

        _mapView.getLayers().add(baseLayer);

        _projection = _mapView.getOptions().getBaseProjection();

        // 1. Initialize an local vector data source
        vectorDataSource1 = new LocalVectorDataSource(_projection);

        // 2. Initialize a vector layer with the previous data source
        VectorLayer vectorLayer1 = new VectorLayer(vectorDataSource1);

        // 3. Add the previous vector layer to the map
        _mapView.getLayers().add(vectorLayer1);

        // 4. Set limited visible zoom range for the vector layer (optional)
        vectorLayer1.setVisibleZoomRange(new MapRange(10, 24));

        // Verificamos que haya internet
        if( !verificarInternet() ) return;

        loadViaVisGeoJson();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Función que procesa un archivo GeoJson y en base a la geometria de la base de datos dibujamos en el mapa
     * con ayuda de las librerias de Carto
     */
    public void loadViaVisGeoJson()
    {
        MapPos _ubicacionCaceres = _projection.fromWgs84(new MapPos(-6.37, 39.47));
        // Initialize a local vector data source
        _projection = _mapView.getOptions().getBaseProjection();
        _source = new LocalVectorDataSource(_projection);

        VectorLayer _layer = new VectorLayer(_source);
        _mapView.getLayers().add(_layer);

        _mapView.setFocusPos(_ubicacionCaceres, 3);
        _mapView.setZoom(12, 3);


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run()
            {
                // Read GeoJSON, parse it using SDK GeoJSON parser
                GeoJSONGeometryReader reader = new GeoJSONGeometryReader();

                // Set target projection to base (mercator)
                reader.setTargetProjection(_projection);

                String fileName = "parque.geojson"; //parquecaceres
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

                // Read features from local asset
                FeatureCollection features = reader.readFeatureCollection(json);

                TextStyleBuilder textStyleBuilder = new TextStyleBuilder();
                textStyleBuilder.setColor(new Color(android.graphics.Color.argb(ALPHARGB, 103, 32, 68)));
                textStyleBuilder.setOrientationMode(BillboardOrientation.BILLBOARD_ORIENTATION_FACE_CAMERA);
                textStyleBuilder.setFontSize(12);

                // This enables higher resolution texts for retina devices, but consumes more memory and is slower
                textStyleBuilder.setScaleWithDPI(false);

                Log.i("DIM-DATA", "DIM-DATA: "+features.getFeatureCount());

                MapPos _position = null;
                for (int i = 0; i < features.getFeatureCount(); i++) {
                    if (features.getFeature(i).getGeometry() instanceof PointGeometry) {
                        Log.i("PointGeometry", "PointGeometry: "+features.getFeature(i).toString());
                        PointStyleBuilder _pointStyleBuilder = new PointStyleBuilder();
                        Variant _pro = features.getFeature(i).getProperties();
                        Log.i("Visitas", "Visitas: "+_pro.getObjectElement("visitas").toString());
                        addStylePoint(_pointStyleBuilder, Integer.parseInt(_pro.getObjectElement("visitas").toString()));
                        PointGeometry _geometry = (PointGeometry)features.getFeature(i).getGeometry();
                        _source.add(new Point(_geometry, _pointStyleBuilder.buildStyle()));
                        _position = _geometry.getCenterPos();

                        Text _textData = new Text(_position, textStyleBuilder.buildStyle(), _pro.getObjectElement("foaf_name").toString().replace("\"",""));
                        _source.add(_textData);
                    } else if (features.getFeature(i).getGeometry() instanceof LineGeometry) {
                        Log.i("LineGeometry", "LineGeometry: "+features.getFeature(i).toString());
                    }  else if (features.getFeature(i).getGeometry() instanceof PolygonGeometry) {
                        Log.i("PolygonGeometry", "PolygonGeometry: "+features.getFeature(i).toString());
                    } else if (features.getFeature(i).getGeometry() instanceof MultiGeometry) {
                    }else{
                        Log.i("SIN-GEOMETRIA", "SIN-GEOMETRIA: "+features.getFeature(i).getGeometry());
                    }
                }
            }
        });
        thread.start(); // TODO: should serialize execution
    }

    /**
     * Función que asigna los estilos a cada parque, para este caso pintamos cada parque en base al numero de visitas
     * Para este ejemplo se ha definido un archivo geojson ya conocido
     * @param pointStyle
     * @param numVisitas
     */
    public void addStylePoint(PointStyleBuilder pointStyle, int numVisitas)
    {
        switch (numVisitas) {
            case 500:
                pointStyle.setColor(new Color(android.graphics.Color.argb(ALPHARGB, 255, 198, 196)));
                pointStyle.setSize(10);
                break;
            case 1000:
                pointStyle.setColor(new Color(android.graphics.Color.argb(ALPHARGB, 242, 156, 163)));
                pointStyle.setSize(14);
                break;
            case 1500:
                pointStyle.setColor(new Color(android.graphics.Color.argb(ALPHARGB, 218, 116, 137)));
                pointStyle.setSize(18);
                break;
            case 2000:
                pointStyle.setColor(new Color(android.graphics.Color.argb(ALPHARGB, 185, 80, 115)));
                pointStyle.setSize(22);
                break;
            case 2500:
                pointStyle.setColor(new Color(android.graphics.Color.argb(ALPHARGB, 147, 52, 93)));
                pointStyle.setSize(26);
                break;
            case 3000:
                pointStyle.setColor(new Color(android.graphics.Color.argb(ALPHARGB, 103, 32, 68)));
                pointStyle.setSize(30);
                break;
            default:
                //poligonoStyle.setColor(new Color(android.graphics.Color.argb(ALPHARGB, GENRED, GENGREEN, GENBLUE)));
                break;
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
}
