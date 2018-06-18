package es.upm.geo.appparqueanimales;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RelativeLayout rellay1;

    Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            rellay1.setVisibility(View.VISIBLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rellay1 = (RelativeLayout) findViewById(R.id.rellay1);

        // Efecto de pantalla de inicio
        handler.postDelayed(runnable, 2000); //2000 is the timeout for the splash
    }

    /**
     * Carga los puntos de los parques en un mapa de Carto
     * @param view
     */
    public void loadPark(View view)
    {
        try {
            Intent intent = new Intent(view.getContext(),ShowParkActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Carga la pantalla de Geofence en donde hace muestra un mensaje indicandole que se encuentra dentro de una parque
     * Los Parques son una base de datos de Carto
     * Para mostrar el area de los Parques se hace uso de la tecnologia Firebase, Carto y Google Maps
     * @param view
     */
    public void locatePark(View view)
    {
        try {
            //Intent intent = new Intent(view.getContext(),LocalizarParkActivity.class);
            Intent intent = new Intent(view.getContext(),GeofenceActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Función que cargar la pantalla de Ubicación haciendo uso de las librerias de CARTO
     * @param view
     */
    public void myUbicacion(View view)
    {
        try {
            //Intent intent = new Intent(view.getContext(),LocalizarParkActivity.class);
            Intent intent = new Intent(view.getContext(),LocalizarParkActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
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