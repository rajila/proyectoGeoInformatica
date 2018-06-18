package es.upm.geo.appparqueanimales;

import java.util.Locale;

import android.util.Log;

import com.carto.core.MapPos;
import com.carto.core.ScreenPos;
//import com.carto.datasources.LocalVectorDataSource;
import com.carto.core.Variant;
import com.carto.datasources.LocalVectorDataSource;
import com.nutiteq.layers.UTFGridRasterTileLayer;
import com.carto.styles.BalloonPopupMargins;
import com.carto.styles.BalloonPopupStyleBuilder;
import com.carto.ui.ClickType;
import com.carto.ui.MapClickInfo;
import com.carto.ui.MapEventListener;
//import com.carto.ui.MapView;
import com.carto.ui.MapView;
import com.carto.ui.VectorElementClickInfo;
//import com.nutiteq.ui.VectorElementsClickInfo;
import com.carto.vectorelements.BalloonPopup;
import com.carto.vectorelements.Billboard;
import com.carto.vectorelements.VectorElement;
import com.nutiteq.wrappedcommons.StringMap;

import java.util.Locale;

public class MyMapEventListener extends MapEventListener {
    private MapView mapView;
    private LocalVectorDataSource vectorDataSource;
    final String LOG_TAG = this.getClass().toString();

    private BalloonPopup oldClickLabel;

    public MyMapEventListener(MapView mapView, LocalVectorDataSource vectorDataSource) {
        this.mapView = mapView;
        this.vectorDataSource = vectorDataSource;
    }

    @Override
    public void onMapMoved() {

        final MapPos topLeft = mapView.screenToMap(new ScreenPos(0, 0));
        final MapPos bottomRight = mapView.screenToMap(new ScreenPos(mapView.getWidth(), mapView.getHeight()));
        Log.d(LOG_TAG, mapView.getOptions().getBaseProjection().toWgs84(topLeft)
                + " " + mapView.getOptions().getBaseProjection().toWgs84(bottomRight));

    }

    @Override
    public void onMapClicked(MapClickInfo mapClickInfo) {
        Log.d(LOG_TAG, "Map click!");

        // Remove old click label
        if (oldClickLabel != null) {
            //vectorDataSource.remove(oldClickLabel);
            oldClickLabel = null;
        }

        BalloonPopupStyleBuilder styleBuilder = new BalloonPopupStyleBuilder();
        // Make sure this label is shown on top all other labels
        styleBuilder.setPlacementPriority(10);

        // Check the type of the click
        String clickMsg = null;
        if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_SINGLE) {
            clickMsg = "Single map click!";
        } else if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_LONG) {
            clickMsg = "Long map click!";
        } else if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_DOUBLE) {
            clickMsg = "Double map click!";
        } else if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_DUAL) {
            clickMsg ="Dual map click!";
        }

        MapPos clickPos = mapClickInfo.getClickPos();
        MapPos wgs84Clickpos = mapView.getOptions().getBaseProjection().toWgs84(clickPos);
        String msg = String.format(Locale.US, "%.4f, %.4f", wgs84Clickpos.getY(), wgs84Clickpos.getX());
        BalloonPopup clickPopup = new BalloonPopup(mapClickInfo.getClickPos(),
                styleBuilder.buildStyle(),
                clickMsg,
                msg);
        vectorDataSource.add(clickPopup);
        oldClickLabel = clickPopup;
    }
}