package com.example.taxi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.TwitterAuthCredential;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CustomersMapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener
{

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    Marker driverMarker, PickUpMarker;
    GeoQuery geoQuery;

    private Button customerLogoutBtn, settingsBtn;
    private Button callTaxiBtn;
    private String customerID;
    private LatLng customerPosition;
    private int radius = 1;
    private Boolean driverFound = false, requestType = false;
    private String driversFoundID;



    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference customerDBRef;
    private DatabaseReference driversAvailableRef;
    private DatabaseReference driversRef;
    private DatabaseReference driversLocationRef;

    private ValueEventListener DriverLocationRefListener;

    private TextView txtName, txtPhone, txtCarName;
    private CircleImageView driverPhoto;
    private RelativeLayout relativeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customers_maps);

        customerLogoutBtn = (Button)findViewById(R.id.customerLogout);
        settingsBtn = (Button)findViewById(R.id.customerSettings);
        callTaxiBtn = (Button)findViewById(R.id.customerOrder);

        txtName=(TextView)findViewById(R.id.driver_name);
        txtPhone=(TextView)findViewById(R.id.driver_phone);
        txtCarName=(TextView)findViewById(R.id.driver_car);
        driverPhoto=(CircleImageView)findViewById(R.id.driver_photo);
        relativeLayout = findViewById(R.id.rel1);

        relativeLayout.setVisibility(View.INVISIBLE);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        customerDBRef = FirebaseDatabase.getInstance().getReference().child("Customer Requests");
        driversAvailableRef = FirebaseDatabase.getInstance().getReference().child("Driver Available");
        driversLocationRef = FirebaseDatabase.getInstance().getReference().child("Driver Working");

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

            settingsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent settingsIntent = new Intent(CustomersMapsActivity.this, SettingsActivity2.class);
                    settingsIntent.putExtra("type", "Customers");
                    startActivity(settingsIntent);
                }
            });

        customerLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();

                LogoutCustomer();
            }
        });

        callTaxiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(requestType){
                    requestType = false;
                    geoQuery.removeAllListeners();
                    driversLocationRef.removeEventListener(DriverLocationRefListener);

                    if(driverFound != null){
                        driversRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driversFoundID).child("CustomerRideID");

                        driversRef.removeValue();
                        driversFoundID = null;
                    }
                    driverFound = false;
                    radius = 1;

                    GeoFire geoFire = new GeoFire(customerDBRef);
                    geoFire.removeLocation(customerID);

                    if(PickUpMarker != null){
                        PickUpMarker.remove();
                    }
                    if(driverMarker != null){
                        driverMarker.remove();
                    }
                    callTaxiBtn.setText("Вызвать такси");
                }
                else{
                    requestType = true;
                    GeoFire geoFire = new GeoFire(customerDBRef);
                    geoFire.setLocation(customerID, new GeoLocation(lastLocation.getLatitude(),lastLocation.getLongitude()));

                    customerPosition = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    PickUpMarker = mMap.addMarker(new MarkerOptions().position(customerPosition).title("Я здесь").icon(BitmapDescriptorFactory.fromResource(R.drawable.user)));

                    callTaxiBtn.setText("Поиск водителя");
                    getNearbyDrivers();
                }


            }
        });
    }




    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    private void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        lastLocation = location;

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

    }

    @Override
    protected void onStop() {
        super.onStop();

    }
    private void LogoutCustomer() {
        Intent welcomeIntent = new Intent(CustomersMapsActivity.this, WelcomeActivity.class);
        startActivity(welcomeIntent);
        finish();
    }
    private void getNearbyDrivers() {
        GeoFire geoFire = new GeoFire(driversAvailableRef);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(customerPosition.latitude,customerPosition.longitude),radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!driverFound && requestType){
                    driverFound = true;
                    driversFoundID = key;

                    driversRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driversFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideID",customerID);
                    driversRef.updateChildren(driverMap);

                    GetDriverLocation();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!driverFound){
                    radius = radius + 1;
                    getNearbyDrivers();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void GetDriverLocation() {

        DriverLocationRefListener = driversLocationRef.child(driversFoundID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists() && requestType){

                            List<Object> driverLocationMap = (List<Object>) snapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;

                            callTaxiBtn.setText("Водитель найден");
                            getDriverInformation();
                            relativeLayout.setVisibility(View.VISIBLE);

                            if(driverLocationMap.get(0) != null){
                                LocationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                            }
                            if(driverLocationMap.get(1) != null){
                                LocationLng = Double.parseDouble(driverLocationMap.get(1).toString());
                            }
                            LatLng DriverLatLng = new LatLng(LocationLat,LocationLng);

                            if(driverMarker != null){
                                driverMarker.remove();
                            }

                            Location location1 = new Location("");
                            location1.setLatitude(customerPosition.latitude);
                            location1.setLongitude(customerPosition.longitude);

                            Location location2 = new Location("");
                            location2.setLatitude(DriverLatLng.latitude);
                            location2.setLongitude(DriverLatLng.longitude);

                            float Distance = location1.distanceTo(location2);
                            if(Distance<100){
                                callTaxiBtn.setText("Ваше такси подъезжает");

                            }
                            else {
                                callTaxiBtn.setText("Расстояние до такси " + String.valueOf(Distance));

                            }


                            driverMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Ваш водитель тут").icon(BitmapDescriptorFactory.fromResource(R.drawable.car)));

                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }
    private void getDriverInformation(){
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driversFoundID);

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists() && snapshot.getChildrenCount() > 0){
                    String name = snapshot.child("name").getValue().toString();
                    String phone = snapshot.child("phone").getValue().toString();
                    String carname = snapshot.child("carname").getValue().toString();

                    txtName.setText(name);
                    txtPhone.setText(phone);
                    txtCarName.setText(carname);

                    if(snapshot.hasChild("image")){
                        String image = snapshot.child("image").getValue().toString();
                        Picasso.get().load(image).into(driverPhoto);
                    }



                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

}