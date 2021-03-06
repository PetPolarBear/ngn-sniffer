package de.fhkoeln.ngn.fragment;

import android.app.Fragment;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import de.fhkoeln.ngn.R;
import de.fhkoeln.ngn.data.HeatMapDataProvider;
import de.fhkoeln.ngn.data.Measurement;
import de.fhkoeln.ngn.service.event.LocationChangedEvent;
import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;


public class MapsFragment extends Fragment implements GoogleMap.OnMyLocationChangeListener, GoogleMap.OnCameraChangeListener {
    private MapView mapView;
    private GoogleMap map;
    private HeatmapTileProvider heatMapTileProvider;
    private Collection<WeightedLatLng> heatMapPoints = new HashSet<>();
    private TileOverlay heatMapOverlay;
    private ArrayList<Circle> circleArrayList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.maps_fragment, container, false);
        initializeMapView(savedInstanceState, v);
        circleArrayList = new ArrayList<>();

        return v;
    }

    private void initializeMapView(Bundle savedInstanceState, View v) {
        initializeHeatMapTileProvider();
        mapView = (MapView) v.findViewById(R.id.mapview);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;
                map.getUiSettings().setMyLocationButtonEnabled(true);
                map.getUiSettings().setZoomControlsEnabled(true);
                map.getUiSettings().setTiltGesturesEnabled(false);
                map.setMyLocationEnabled(true);
                map.setOnCameraChangeListener(MapsFragment.this);
                map.setOnMyLocationChangeListener(MapsFragment.this);
                map.setOnMapClickListener(onMapClickListener);
                TileOverlayOptions options = new TileOverlayOptions().tileProvider(heatMapTileProvider);
                options.visible(true);
                heatMapOverlay = map.addTileOverlay(options);
                updateHeatMapData(5);
                MapsInitializer.initialize(getActivity());
            }
        });
    }

    private void initializeHeatMapTileProvider() {
        int[] colors = {Color.rgb(255, 0, 0), Color.rgb(102, 225, 0)};
        float[] startPoints = {0.1f, 1f};
        Gradient gradient = new Gradient(colors, startPoints);

        heatMapPoints.add(new WeightedLatLng(new LatLng(50.962238 + Math.random() - Math.random(), 7.000172 + Math.random() - Math.random()), 1.0));
        heatMapTileProvider = new HeatmapTileProvider.Builder()
                .weightedData(heatMapPoints)
                .radius(30)
                .opacity(.7)
                .gradient(gradient)
                .build();
    }
    List<WeightedLatLng> wLatLngList;
    private void updateHeatMapData(int r) {
        final int radius = r;
        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        HeatMapDataProvider.getHeatMapData(bounds, new Callback<List<WeightedLatLng>>() {

            @Override
            public void success(List<WeightedLatLng> weightedLatLngList, Response response) {
                if (weightedLatLngList.size() > 0) {
                    wLatLngList = weightedLatLngList;
                    heatMapTileProvider.setWeightedData(weightedLatLngList);
                    heatMapOverlay.clearTileCache();
                    //drawMeasurementPoints(weightedLatLngList, radius);
                }
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onMyLocationChange(Location location) {
        //TODO: Save measurement every 50 meters
        //EventBus.getDefault().post(new LocationChangedEvent(location));
        //HeatMapDataProvider.saveMeasurement(SmallDetailFragment.getMeasurement());
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        int radius = (int) (cameraPosition.zoom * 3);
        heatMapTileProvider.setRadius(radius);
        updateHeatMapData(radius);
        drawMeasurementPoints(wLatLngList, radius);
    }

    ArrayList<Measurement> measurementPoints;
    Measurement m;
    public void drawMeasurementPoints(List<WeightedLatLng> weightedLatLngList, int radius)
    {
        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        HeatMapDataProvider.getMeasurements(bounds, false, new Callback<List<Measurement>>() {

            @Override
            public void success(List<Measurement> measurements, Response response) {
                measurementPoints = new ArrayList<>(measurements);
                Log.d("MapsFragment", "drawMeasurementPoints: "+measurements.size());
                if(measurements.size() >0)
                {
                    for(Measurement m : measurements)
                    {
                        Log.d("MapsFragment", "drawMeasurementPoints: Lat="+m.getLat() + " Lng="+m.getLng() + " Signal DBm: "+m.getSignalDBm() + "Type: "+m.getType()+ "APs: "+m.getWifiAPs());
                    }
                }
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });

        HeatMapDataProvider.getHeatMapData(bounds, new Callback<List<WeightedLatLng>>() {

            @Override
            public void success(List<WeightedLatLng> weightedLatLngList, Response response) {
                if (weightedLatLngList.size() > 0) {
                    heatMapTileProvider.setWeightedData(weightedLatLngList);
                    heatMapOverlay.clearTileCache();
                }
            }

            @Override
            public void failure(RetrofitError error) {

            }
        });

        //TODO: Clear all circles and redraw with new radius; Circles must be saved in a List
        if(m != null)
        {
            //for(Measurement measurement : measurementPoints)
            {
                CircleOptions circleOptions = new CircleOptions()
                        .center(new LatLng(m.getLat(), m.getLng()))
                        .radius(5);
                //Log.d("MapsFragment", "drawMeasurementPoints: Lat="+m.getLat() + " Lng="+m.getLng());

                Circle circle = map.addCircle(circleOptions);
                circle.setFillColor(getResources().getColor(R.color.indigo_500));
                circle.setStrokeWidth(1);
                circleArrayList.add(circle);
            }
        }
    }

    public GoogleMap.OnMapClickListener onMapClickListener = new GoogleMap.OnMapClickListener()
    {
        @Override
        public void onMapClick(LatLng position)
        {
            for(Circle circle : circleArrayList)
            {
                LatLng center = circle.getCenter();
                double radius = circle.getRadius();
                float[] distance = new float[1];
                Location.distanceBetween(position.latitude, position.longitude, center.latitude, center.longitude, distance);
                if(distance[0] < radius)
                {
                    // TODO: Open info dialog
                }
            }
        }
    };
}
