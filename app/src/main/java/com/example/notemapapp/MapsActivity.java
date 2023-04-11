package com.example.notemapapp;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.notemapapp.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.PointOfInterest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.Locale;

import io.paperdb.Paper;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, com.google.android.gms.location.LocationListener, GoogleApiClient.ConnectionCallbacks,
        SensorEventListener{

    private GoogleMap mMap;
    Location lastLocation;
    LocationRequest locationRequest;
    GoogleApiClient googleApiClient;

    Dialog dialog;
    ConstraintLayout layout;

    TextView tvMarkerAdded;

    ImageButton darkMode;
    boolean isDark = false;
    boolean auto = false;
    ImageButton btnAuto;

    SensorManager sensorManager;
    private Sensor sensorLight;
    private TextView textSensorLight;

    EditText etTitle;
    EditText etDesc;
    Button btnSave;

    FirebaseDatabase database;
    DatabaseReference reference;

    Notes notes;

    private Button compassButton;
    private boolean isFragmentDisplayed = true;
    Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        darkMode = findViewById(R.id.btn_dark_mode);

//        compassButton = findViewById(R.id.btn_compass);
//        compassButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(!isFragmentDisplayed){
//                    compassButton.setText("Compass");
//                    fragment = MapFragment.newInstance();
//                }
//                else{
//                    compassButton.setText("Map");
//                    fragment = MapFragment2.newInstance();
//                }
//                displayFragment(fragment);
//                isFragmentDisplayed = !isFragmentDisplayed;
//            }
//        });
//        displayFragment(MapFragment.newInstance());

//        btnAuto.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                auto = true;
//            }
//        });

        Paper.init(this);

        tvMarkerAdded = findViewById(R.id.marker_added);

        layout = new ConstraintLayout(this);
//
//        binding = ActivityMapsBinding.inflate(getLayoutInflater());
//        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        textSensorLight = findViewById(R.id.label_light);

        String sensor_error = "No sensor";
        if(sensorLight == null){
            textSensorLight.setText(sensor_error);
        }
    }

    private void displayFragment(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment).addToBackStack(null).commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (sensorLight != null){
            sensorManager.registerListener(this, sensorLight,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //ambil data dari sensor eventnya, ambil perubahan dimana
        int sensorType = event.sensor.getType();
        float currentValue = event.values[0];
        switch (sensorType) {
            case Sensor.TYPE_LIGHT:
                textSensorLight.setText(String.format("Light sensor : %1$.2f", currentValue));
                changeBgColor(currentValue);
            default:
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void changeBgColor(float value){
        if (value > 0 && value <= 10){
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(MapsActivity.this, R.raw.style_dark));
            darkMode.setImageResource(R.drawable.dark);
            isDark = true;
        }
        else if (value > 10 && value <= 30000){
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(MapsActivity.this, R.raw.style_light));
            darkMode.setImageResource(R.drawable.light);
            isDark = false;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        darkMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isDark){
                    mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(MapsActivity.this, R.raw.style_dark));
                    darkMode.setImageResource(R.drawable.dark);
                    isDark = true;
                }
                else{
                    mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(MapsActivity.this, R.raw.style_light));
                    darkMode.setImageResource(R.drawable.light);
                    isDark = false;
                }

            }
        });

//        try {
//            // Customise the styling of the base map using a JSON object defined
//            // in a raw resource file.
//            boolean success = mMap.setMapStyle(
//                    MapStyleOptions.loadRawResourceStyle(
//                            MapsActivity.this, R.raw.style_json));
//
//            if (!success) {
//                Log.e(TAG, "Style parsing failed.");
//            }
//        } catch (Resources.NotFoundException e) {
//            Log.e(TAG, "Can't find style.", e);
//        }

        setMapOnClick(mMap);
        setPoiClicked(mMap);
//        enableMyLocation();
    }

    void buildGoogleApiClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        lastLocation = location;
        LatLng latiLong = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latiLong, 16));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        //trigger refresh location
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_option, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.normal_map:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                return true;
            case R.id.satelite_map:
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                return true;
            case R.id.terrain_map:
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                return true;
            case R.id.hybrid_map:
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                return true;
            case R.id.action_language:
                Intent languageIntent = new Intent(Settings.ACTION_LOCALE_SETTINGS);
                startActivity(languageIntent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public int markerAdded = 0;
    private void setMapOnClick(final GoogleMap map){
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull LatLng latLng) {
                String text = String.format(Locale.getDefault(),
                        "Lat : %1$.5f, Long : %2$.5f",
                        latLng.latitude,
                        latLng.longitude);
                map.addMarker(new MarkerOptions()
                        .position(latLng)
                        .snippet(text)
                        .title("Dropped pin")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.hengkermap)));
                markerAdded++;
                tvMarkerAdded.setText(String.valueOf(markerAdded));

                Paper.book().write("markerAmount", String.valueOf(markerAdded)).toString();

            }
        });
    }

    private void setPoiClicked(final GoogleMap map){
        map.setOnPoiClickListener(new GoogleMap.OnPoiClickListener() {
            @Override
            public void onPoiClick(@NonNull PointOfInterest pointOfInterest) {
                Marker poiMarker = mMap.addMarker(new MarkerOptions()
                        .position(pointOfInterest.latLng)
                        .title(pointOfInterest.name));
                poiMarker.showInfoWindow();

                createNotePopUp();
            }
        });
    }
//
//    private void enableMyLocation(){
//        if (ContextCompat.checkSelfPermission(this,
//                android.Manifest.permission.ACCESS_FINE_LOCATION)
//                == PackageManager.PERMISSION_GRANTED){
//            mMap.setMyLocationEnabled(true);
//        }
//
//        else{
//            ActivityCompat.requestPermissions(this, new String[]{
//                    Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        switch (requestCode){
//            case 1:
//                if (grantResults.length>0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                    enableMyLocation();
//                    break;
//                }
//        }
//    }

    private void createNotePopUp(){
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popUpNote = inflater.inflate(R.layout.note_popup, null);

        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        boolean focusable = true;
        PopupWindow popupWindow = new PopupWindow(popUpNote, width, height, focusable);
        popupWindow.showAtLocation(layout, Gravity.TOP, 0, 0);

        etTitle = popUpNote.findViewById(R.id.et_title);
        etDesc = popUpNote.findViewById(R.id.et_desc);
        btnSave = popUpNote.findViewById(R.id.btn_save);

        database = FirebaseDatabase.getInstance();
        reference = database.getReference("Notes");
        notes = new Notes();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = "note" + new Date().getTime();
                String title = etTitle.getText().toString();
                String desc = etDesc.getText().toString();

//                addDataToFirebase(id, title, desc);
////                    reference.child("Notes").child(id).setValue(new Notes(id, title, desc));
//                Toast.makeText(MapsActivity.this, "Note Added", Toast.LENGTH_SHORT).show();

                if(TextUtils.isEmpty(title) && TextUtils.isEmpty(desc)){
                    Toast.makeText(MapsActivity.this, "Please add some data.", Toast.LENGTH_SHORT).show();
                }
                else{
                    addDataToFirebase(id, title, desc);
//                    reference.child("Notes").child(id).setValue(new Notes(id, title, desc));
                    Toast.makeText(MapsActivity.this, "Note Added", Toast.LENGTH_SHORT).show();
                }
                readData();
            }
        });
    }

    private void readData(){
        reference.child("Notes").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot dataSnapshot : snapshot.getChildren()){
                    Notes notes = dataSnapshot.getValue(Notes.class);
                    etTitle.setText(notes.noteTitle);
                    etDesc.setText(notes.noteDesc);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void addDataToFirebase(String id, String title, String desc){
        notes.setNoteID(id);
        notes.setNoteTitle(title);
        notes.setNoteDesc(desc);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reference.setValue(notes);
                Toast.makeText(MapsActivity.this, "Note Added", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MapsActivity.this, "Fail to Add Note " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }



    @Override
    public void onConnectionSuspended(int i) {

    }


}