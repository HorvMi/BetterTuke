package hu.krisz768.bettertuke;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentContainerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hu.krisz768.bettertuke.Database.BusJaratok;
import hu.krisz768.bettertuke.Database.BusLine;
import hu.krisz768.bettertuke.Database.BusPlaces;
import hu.krisz768.bettertuke.Database.BusStops;
import hu.krisz768.bettertuke.Database.DatabaseManager;
import hu.krisz768.bettertuke.IncomingBusFragment.BottomSheetIncomingBusFragment;
import hu.krisz768.bettertuke.SearchFragment.SearchViewFragment;
import hu.krisz768.bettertuke.TrackBusFragment.BottomSheetTrackBusFragment;
import hu.krisz768.bettertuke.UserDatabase.UserDatabase;
import hu.krisz768.bettertuke.models.BackStack;
import hu.krisz768.bettertuke.models.MarkerDescriptor;
import hu.krisz768.bettertuke.models.ScheduleBackStack;
import hu.krisz768.bettertuke.models.SearchResult;

public class MainActivity extends AppCompatActivity {

    FusedLocationProviderClient fusedLocationClient;

    View BottomSheet;
    BottomSheetBehavior bottomSheetBehavior;
    private BottomSheetBehavior.BottomSheetCallback BottomSheetCallback;

    private SearchView searchView;
    SearchViewFragment Svf;

    private Integer CurrentPlace = -1;
    private Integer CurrentStop = -1;
    private Integer CurrentBusTrack = -1;
    private BusJaratok busJarat;

    private GoogleMap googleMap;

    private BusStops[] busStops;
    private BusPlaces[] busPlaces;

    private List<BackStack> backStack = new ArrayList<>();

    private boolean smallMarkerMode = false;

    private boolean IsBackButtonCollapse = true;

    private Marker BusMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme();

        HelperProvider.RenderAllBitmap(this);

        setContentView(R.layout.activity_main);


        BottomSheet = findViewById(R.id.standard_bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(BottomSheet);

        SetupBottomSheet();

        SetupSearchView();

        findViewById(R.id.ShowScheduleButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShowSchedule(-1, null, null, null, false);
            }
        });

        busStops = BusStops.GetAllStops(this);
        busPlaces = BusPlaces.getAllBusPlaces(this);

        SetupGoogleMap();
    }

    private void setTheme() {
        if (Build.VERSION.SDK_INT < 31) {
            setTheme(R.style.DefaultPre12);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        SetupBottomSheet();

        final FragmentContainerView fragmentView = findViewById(R.id.fragmentContainerView2);

        ViewGroup.LayoutParams params = fragmentView.getLayoutParams();
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            params.height = bottomSheetBehavior.getPeekHeight();
        } else if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            params.height = bottomSheetBehavior.getMaxHeight();
        }

        fragmentView.setLayoutParams(params);
    }

    public void ChangeStop(int Id) {
        AddBackStack();

        CurrentStop = Id;
        ZoomToMarker();
        MarkerRenderer();
    }

    private void SetupGoogleMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        final Context ctx = this;

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap_) {
                googleMap = googleMap_;

                googleMap.getUiSettings().setZoomControlsEnabled(false);
                googleMap.getUiSettings().setMyLocationButtonEnabled(false);

                MapStyleOptions style = new MapStyleOptions(HelperProvider.GetMapTheme(ctx));
                googleMap.setMapStyle(style);


                googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(@NonNull Marker marker) {
                        MarkerClickListener(marker);
                        return true;
                    }
                });

                googleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                    @Override
                    public void onCameraMove() {
                        //Log.e("ZOOM", googleMap.getCameraPosition().zoom + "");
                        final float ZoomLevel = googleMap.getCameraPosition().zoom;
                        if (ZoomLevel > 13.7) {
                            if (!smallMarkerMode) {
                                smallMarkerMode = true;
                                MarkerRenderer();
                            }
                        } else {
                            if (smallMarkerMode) {
                                smallMarkerMode = false;
                                MarkerRenderer();
                            }
                        }
                    }
                });

                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

                    MarkerRenderer();
                } else {
                    MarkerRenderer();

                    googleMap.setMyLocationEnabled(true);
                    GetClosestStop();
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i = 0; i < permissions.length; i++) {
            //Log.e("TESZT", permissions[i] + " " + grantResults[i]);
            if (permissions[i].equals("android.permission.ACCESS_FINE_LOCATION")) {
                if (grantResults[i] != -1) {
                    googleMap.setMyLocationEnabled(true);
                    GetClosestStop();
                } else {
                    GPSErr();
                }
            }
        }

    }

    private void GPSErr() {
        final LatLng Pecs = new LatLng(46.0707, 18.2331);

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(Pecs).zoom(12).build();

        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        Toast.makeText(this, R.string.GPSHint, Toast.LENGTH_LONG).show();
    }

    private void MarkerClickListener(Marker marker) {
        MarkerDescriptor Md = (MarkerDescriptor) marker.getTag();

        if (Md.getType() == MarkerDescriptor.Types.Stop) {
            SelectStop(Md.getId());
        } else if (Md.getType() == MarkerDescriptor.Types.Place){
            SelectPlace(Md.getId());
        } else {
            ZoomTo(BusMarker.getPosition());
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }


    }

    private void SelectStop(int StopId) {
        AddBackStack();

        if (CurrentBusTrack != -1) {
            CurrentBusTrack = -1;
            busJarat = null;
        }

        CurrentStop = StopId;
        for (int i = 0; i < busStops.length; i++) {
            if (busStops[i].getId() == CurrentStop) {
                int FoldhelyId = busStops[i].getFoldhely();
                for (int j = 0; j < busPlaces.length; j++) {
                    if (busPlaces[j].getId() == FoldhelyId) {
                        CurrentPlace = FoldhelyId;
                        break;
                    }
                }
                break;
            }
        }

        ZoomToMarker();
        ShowBottomSheetIncommingBuses();
        MarkerRenderer();
    }

    private void SelectPlace(int PlaceId) {
        AddBackStack();

        if (CurrentBusTrack != -1) {
            CurrentBusTrack = -1;
            busJarat = null;
        }

        CurrentPlace = PlaceId;

        List<Integer> StopIds = new ArrayList<>();

        for (int i = 0; i < busStops.length; i++) {
            if (busStops[i].getFoldhely() == CurrentPlace) {
                StopIds.add(busStops[i].getId());
                //CurrentStop = busStops[i].getId();
                //break;
            }
        }

        UserDatabase userDatabase = new UserDatabase(this);

        int Favid = Integer.MAX_VALUE;

        for (int i = 0; i < StopIds.size(); i++) {
            if (userDatabase.IsFavorite(UserDatabase.FavoriteType.Stop, StopIds.get(i).toString())) {
                int tFavId = userDatabase.GetId(StopIds.get(i).toString(), UserDatabase.FavoriteType.Stop);
                if (Favid > tFavId) {
                    CurrentStop = StopIds.get(i);
                    Favid = tFavId;
                }
            }
        }

        if ( Favid == Integer.MAX_VALUE) {
            CurrentStop = StopIds.get(0);
        }

        ZoomToMarker();
        ShowBottomSheetIncommingBuses();
        MarkerRenderer();
    }

    private void ZoomToMarker() {
        if (CurrentStop == -1)
            return;

        for (int i = 0; i < busStops.length; i++) {
            if (busStops[i].getId() == CurrentStop) {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(busStops[i].getGpsY(), busStops[i].getGpsX())).zoom(17.5F).build();

                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
    }

    private void MarkerRenderer() {
        googleMap.clear();
        if (busJarat == null) {
            BusMarker = null;
        }
        if (BusMarker != null) {
            BitmapDescriptor BusBitmap = BitmapDescriptorFactory.fromBitmap(HelperProvider.getBitmap(HelperProvider.Bitmaps.MapBus));
            MarkerOptions BusMarkerOption = new MarkerOptions().position(new LatLng(BusMarker.getPosition().latitude, BusMarker.getPosition().longitude)).icon(BusBitmap);
            CreateBusMarker(BusMarkerOption);
        }

        BitmapDescriptor StopSelected = BitmapDescriptorFactory.fromBitmap(HelperProvider.getBitmap(HelperProvider.Bitmaps.MapStopSelected));
        BitmapDescriptor StopNotSelected = BitmapDescriptorFactory.fromBitmap(HelperProvider.getBitmap(HelperProvider.Bitmaps.MapStopNotSelected));
        BitmapDescriptor Place;
        if (smallMarkerMode) {
            Place = BitmapDescriptorFactory.fromBitmap(HelperProvider.getBitmap(HelperProvider.Bitmaps.MapSmallPlace));
        } else {
            Place = BitmapDescriptorFactory.fromBitmap(HelperProvider.getBitmap(HelperProvider.Bitmaps.MapPlace));
        }


        if (CurrentBusTrack == -1) {
            for (int i = 0; i < busPlaces.length; i++) {
                if (busPlaces[i].getId() == CurrentPlace) {
                    for (int j = 0; j < busStops.length; j++) {
                        if (busStops[j].getFoldhely() == CurrentPlace) {

                            if (busStops[j].getId() == CurrentStop) {
                                Marker marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(busStops[j].getGpsY(), busStops[j].getGpsX())).icon(StopSelected));
                                marker.setTag(new MarkerDescriptor(MarkerDescriptor.Types.Stop, busStops[j].getId()));
                            } else {
                                Marker marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(busStops[j].getGpsY(), busStops[j].getGpsX())).icon(StopNotSelected));
                                marker.setTag(new MarkerDescriptor(MarkerDescriptor.Types.Stop, busStops[j].getId()));
                            }

                        }
                    }
                } else {
                    Marker marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(busPlaces[i].getGpsY(), busPlaces[i].getGpsX())).icon(Place));
                    marker.setTag(new MarkerDescriptor(MarkerDescriptor.Types.Place, busPlaces[i].getId()));
                }
            }
        } else {
            for (int i = 0; i < busJarat.getMegallok().length; i++) {
                for (int j = 0; j < busStops.length; j++) {
                    if (busJarat.getMegallok()[i].getKocsiallasId() == busStops[j].getId()) {
                        if (CurrentStop == busStops[j].getId()) {
                            Marker marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(busStops[j].getGpsY(), busStops[j].getGpsX())).icon(StopSelected));
                            marker.setTag(new MarkerDescriptor(MarkerDescriptor.Types.Stop, busStops[j].getId()));
                            break;
                        } else {
                            Marker marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(busStops[j].getGpsY(), busStops[j].getGpsX())).icon(StopNotSelected));
                            marker.setTag(new MarkerDescriptor(MarkerDescriptor.Types.Stop, busStops[j].getId()));
                            break;
                        }
                    }
                }
            }

            PolylineOptions lineOptions = new PolylineOptions();
            ArrayList<LatLng> points = new ArrayList();

            for (int i = 0; i < busJarat.getNyomvonal().length; i++) {
                LatLng position = new LatLng(busJarat.getNyomvonal()[i].getGpsY(), busJarat.getNyomvonal()[i].getGpsX());
                points.add(position);
            }

            lineOptions.addAll(points);
            lineOptions.width(12);

            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
            ContextCompat.getColor(this, typedValue.resourceId);

            lineOptions.color(ContextCompat.getColor(this, typedValue.resourceId));
            lineOptions.geodesic(true);

            googleMap.addPolyline(lineOptions);
        }
    }

    private void GetClosestStop() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            //CurrentLocationRequest asd = new CurrentLocationRequest.Builder().


            final LatLng Pecs = new LatLng(46.0707, 18.2331);

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(Pecs).zoom(12).build();

            //googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationToken() {
                @NonNull
                @Override
                public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                    return null;
                }

                @Override
                public boolean isCancellationRequested() {
                    return false;
                }
            }).addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        GetClosestStopFromList(location);
                    } else {
                        GPSErr();
                    }

                }
            });
        } catch (Exception e) {
            Log.e("Main", e.toString());
        }
    }

    private void GetClosestStopFromList(Location location) {
        int Closest = -1;

        double ClosestDistance = Double.MAX_VALUE;

        if (CurrentPlace != -1) {
            return;
        }

        for (int i = 0; i < busStops.length; i++) {
            double x = location.getLongitude() - busStops[i].getGpsX();
            double y = location.getLatitude() - busStops[i].getGpsY();

            double Distance = Math.sqrt((x * x) + (y * y));

            if (Distance < ClosestDistance) {
                ClosestDistance = Distance;
                Closest = i;
            }
        }

        if (Closest != -1) {
            CurrentPlace = busStops[Closest].getFoldhely();
            CurrentStop = busStops[Closest].getId();
            BottomSheetSetNormalParams();
            ZoomClose(new LatLng(busStops[Closest].getGpsY(), busStops[Closest].getGpsX()), new LatLng(location.getLatitude(), location.getLongitude()));
        }

        MarkerRenderer();
        ShowBottomSheetIncommingBuses();
    }

    private void ZoomClose(LatLng location, LatLng location2) {
        try {
            LatLng First = new LatLng(location.latitude > location2.latitude ? location2.latitude : location.latitude, location.longitude > location2.longitude ? location2.longitude : location.longitude);
            LatLng Second = new LatLng(location.latitude > location2.latitude ? location.latitude : location2.latitude, location.longitude > location2.longitude ? location.longitude : location2.longitude);

            LatLngBounds Bounds = new LatLngBounds(First,Second);

            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            int dp20 = Math.round(TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    20,
                    displayMetrics
            ));

            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(Bounds, dp20*4));
        } catch (Exception e) {
            if (BusMarker != null) {
                ZoomTo(location2);
            } else {
                ZoomToMarker();
            }

        }
        //googleMap.animateCamera(CameraUpdateFactory.zoomTo(googleMap.getCameraPosition().zoom - 2.0f));
    }

    private void SetupBottomSheet() {

        final FragmentContainerView fragmentView = findViewById(R.id.fragmentContainerView2);
        final FloatingActionButton ScheduleButton = findViewById(R.id.ShowScheduleButton);

        ViewGroup.LayoutParams params = fragmentView.getLayoutParams();
        params.height = bottomSheetBehavior.getPeekHeight();
        fragmentView.setLayoutParams(params);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int dp20 = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                20,
                displayMetrics
        ));

        if (BottomSheetCallback == null) {
            BottomSheetCallback = new BottomSheetBehavior.BottomSheetCallback() {

                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    ViewGroup.LayoutParams params = fragmentView.getLayoutParams();
                    ConstraintLayout.LayoutParams params2 = (ConstraintLayout.LayoutParams) ScheduleButton.getLayoutParams();

                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        IsBackButtonCollapse = true;

                        params.height = bottomSheetBehavior.getPeekHeight();

                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            googleMap.setPadding(0, dp20*4, 0, 0);
                            params2.bottomMargin = dp20;
                        } else {
                            googleMap.setPadding(0, dp20*4, 0, params.height);
                            params2.bottomMargin = params.height + dp20;
                        }
                    } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        params.height = bottomSheet.getHeight();
                    } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        googleMap.setPadding(0, dp20*4, 0, 0);
                        params2.bottomMargin = dp20;
                    }else if (newState == BottomSheetBehavior.STATE_SETTLING) {
                    }

                    fragmentView.setLayoutParams(params);
                    ScheduleButton.setLayoutParams(params2);
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    ViewGroup.LayoutParams params = fragmentView.getLayoutParams();
                    ConstraintLayout.LayoutParams params2 = (ConstraintLayout.LayoutParams) ScheduleButton.getLayoutParams();

                    if (slideOffset > 0) {
                        params.height = Math.round(bottomSheetBehavior.getPeekHeight() + ((bottomSheet.getHeight() - bottomSheetBehavior.getPeekHeight()) * slideOffset));
                    } else if (slideOffset < 0) {
                        params.height = bottomSheetBehavior.getPeekHeight();
                        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            googleMap.setPadding(0, dp20*4, 0, 0);
                            params2.bottomMargin = dp20;
                        } else {
                            googleMap.setPadding(0, dp20*4, 0, Math.round(bottomSheetBehavior.getPeekHeight() + ((bottomSheetBehavior.getPeekHeight()) * slideOffset)));
                            params2.bottomMargin = Math.round(bottomSheetBehavior.getPeekHeight() + ((bottomSheetBehavior.getPeekHeight()) * slideOffset)) + dp20;
                        }
                    }

                    fragmentView.setLayoutParams(params);
                    ScheduleButton.setLayoutParams(params2);
                }
            };
            bottomSheetBehavior.addBottomSheetCallback(BottomSheetCallback);

            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            int height = displayMetrics.heightPixels / 10;
            bottomSheetBehavior.setPeekHeight(height);

            bottomSheetBehavior.setMaxHeight(height);
            bottomSheetBehavior.setHideable(false);

            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                ConstraintLayout.LayoutParams params2 = (ConstraintLayout.LayoutParams) ScheduleButton.getLayoutParams();
                params2.bottomMargin = dp20;
                ScheduleButton.setLayoutParams(params2);
            } else {
                ConstraintLayout.LayoutParams params2 = (ConstraintLayout.LayoutParams) ScheduleButton.getLayoutParams();
                params2.bottomMargin = height + dp20;
                ScheduleButton.setLayoutParams(params2);
            }


            GPSLoadFragment InBusFragment = GPSLoadFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView2, InBusFragment)
                    .commit();
        } else {
            int height = displayMetrics.heightPixels / 3;

            int MinHeight = (int) Math.ceil(180 * displayMetrics.density);
            if (MinHeight > height) {
                height = MinHeight;
            }

            bottomSheetBehavior.setPeekHeight(height);

            ConstraintLayout.LayoutParams params2 = (ConstraintLayout.LayoutParams) ScheduleButton.getLayoutParams();

            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_HIDDEN) {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    googleMap.setPadding(0, dp20*4, 0, 0);
                    params2.bottomMargin = dp20;
                } else {
                    googleMap.setPadding(0, dp20*4, 0, height);
                    params2.bottomMargin = height + dp20;
                }



                ScheduleButton.setLayoutParams(params2);
            }
        }
    }

    private void ShowBottomSheetIncommingBuses() {
        BottomSheetSetNormalParams();

        BottomSheetIncomingBusFragment InBusFragment = BottomSheetIncomingBusFragment.newInstance(CurrentPlace, CurrentStop, busPlaces, busStops);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView2, InBusFragment)
                .commit();
    }

    public void TrackBus(int Id, String Date) {
        AddBackStack();
        CurrentBusTrack = Id;
        busJarat = BusJaratok.BusJaratokByJaratid(Id, this);
        if (Date != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date date = new Date();
            if (!Date.equals(formatter.format(date))) {
                busJarat.setDate(Date);
            }
        }

        if (BusMarker != null) {
            BusMarker.remove();
            BusMarker = null;
        }

        MarkerRenderer();
        ShowBottomSheetTrackBus();
    }

    private void BottomSheetSetNormalParams() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            IsBackButtonCollapse = false;
        } else {
            IsBackButtonCollapse = true;
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int dp20 = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                20,
                displayMetrics
        ));
        int height = displayMetrics.heightPixels / 3;

        int MinHeight = (int) Math.ceil(180 * displayMetrics.density);
        if (MinHeight > height) {
            height = MinHeight;
        }

        bottomSheetBehavior.setPeekHeight(height);

        bottomSheetBehavior.setMaxHeight(-1);
        bottomSheetBehavior.setHideable(true);

        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        final View Fc = findViewById(R.id.fragmentContainerView2);
        final FloatingActionButton ScheduleButton = findViewById(R.id.ShowScheduleButton);

        ViewGroup.LayoutParams params = Fc.getLayoutParams();
        ConstraintLayout.LayoutParams params2 = (ConstraintLayout.LayoutParams) ScheduleButton.getLayoutParams();

        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            params.height = height;
        } else {
            params.height = getWindow().getDecorView().getHeight();
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            googleMap.setPadding(0, dp20*4, 0, 0);
            params2.bottomMargin = dp20;
        } else {
            googleMap.setPadding(0, dp20*4, 0, height);
            params2.bottomMargin = height + dp20;
        }
        Fc.setLayoutParams(params);
        ScheduleButton.setLayoutParams(params2);
    }

    private void ShowBottomSheetTrackBus() {
        BottomSheetSetNormalParams();

        BottomSheetTrackBusFragment TrackBusFragment = BottomSheetTrackBusFragment.newInstance(CurrentPlace, CurrentStop, CurrentBusTrack, busPlaces, busStops, busJarat);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainerView2, TrackBusFragment)
                .commit();
    }

    public void BuspositionMarker(LatLng BusPosition) {
        if(busJarat != null) {
            if (BusPosition != null) {
                BitmapDescriptor BusBitmap = BitmapDescriptorFactory.fromBitmap(HelperProvider.getBitmap(HelperProvider.Bitmaps.MapBus));
                if (BusMarker == null) {
                    MarkerOptions BusMarkerOption = new MarkerOptions().position(new LatLng(BusPosition.latitude, BusPosition.longitude)).icon(BusBitmap);
                    CreateBusMarker(BusMarkerOption);

                    for (int i = 0; i < busStops.length; i++) {
                        if (busStops[i].getId() == CurrentStop) {
                            ZoomClose(new LatLng(busStops[i].getGpsY(), busStops[i].getGpsX()), new LatLng(BusPosition.latitude, BusPosition.longitude));
                        }
                    }
                } else {
                    BusMarker.setPosition(BusPosition);
                }
            } else {
                if (BusMarker != null) {
                    BusMarker.remove();
                    BusMarker = null;
                }
            }
        } else {
            if (BusMarker != null) {
                BusMarker.remove();
                BusMarker = null;
            }
        }

    }

    public void ZoomTo(LatLng Position) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(Position).zoom(15).build();

        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void CreateBusMarker(MarkerOptions option) {
        BusMarker = googleMap.addMarker(option);
        BusMarker.setTag(new MarkerDescriptor(MarkerDescriptor.Types.Bus, -1));
        BusMarker.setZIndex(Float.MAX_VALUE);
    }

    public boolean IsBottomSheetCollapsed() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (searchView.isShowing()) {
            searchView.hide();
            return;
        }

        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return;
        }

        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED && IsBackButtonCollapse) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return;
        }

        if (backStack.size() == 0) {
            /*if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                return;
            }*/
            finish();
        } else {
            RestorePrevState();
        }
    }

    private void RestorePrevState() {
        BackStack PrevState = backStack.get(backStack.size()-1);

        ScheduleBackStack scheduleBackStack = PrevState.getScheduleBackStack();
        if (scheduleBackStack != null) {
            ShowSchedule(scheduleBackStack.getStopId(), scheduleBackStack.getLineNum(), scheduleBackStack.getDirection(), scheduleBackStack.getDate(), scheduleBackStack.isPreSelected());

            backStack.remove(backStack.size()-1);

            RestorePrevState();
            return;
        }

        CurrentPlace = PrevState.getCurrentPlace();
        CurrentStop = PrevState.getCurrentStop();
        CurrentBusTrack = PrevState.getCurrentBusTrack();
        busJarat = PrevState.getBusJarat();

        backStack.remove(backStack.size()-1);

        if (BusMarker != null) {
            BusMarker.remove();
            BusMarker = null;
        }

        switch (DetermineMode()) {
            case IncBus:
                ShowBottomSheetIncommingBuses();
                ZoomToMarker();
                break;
            case TrackBus:
                ShowBottomSheetTrackBus();

                break;
        }

        IsBackButtonCollapse = PrevState.isBackButtonCollapse();

        MarkerRenderer();
    }

    private Mode DetermineMode() {
        if (CurrentStop == -1) {
            return Mode.None;
        } else if (CurrentBusTrack == -1) {
            return Mode.IncBus;
        } else {
            return Mode.TrackBus;
        }
    }

    private enum Mode {
        None,
        IncBus,
        TrackBus
    }

    private void AddBackStack() {
        Log.e("asd", IsBackButtonCollapse+ "");
        backStack.add(new BackStack(CurrentPlace, CurrentStop, CurrentBusTrack, busJarat, null, IsBackButtonCollapse));
    }

    public void ShowSchedule(int StopId, String LineNum, String Direction, String Date, boolean PreSelected) {
        Intent scheduleIntent = new Intent(this, ScheduleActivity.class);
        scheduleIntent.putExtra("StopId", StopId);
        scheduleIntent.putExtra("LineNum", LineNum);
        scheduleIntent.putExtra("Direction", Direction);
        scheduleIntent.putExtra("Date", Date);
        scheduleIntent.putExtra("PreSelected", PreSelected);
        scheduleResultLaunch.launch(scheduleIntent);
    }

    ActivityResultLauncher<Intent> scheduleResultLaunch = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        TrackBus(result.getData().getExtras().getInt("ScheduleId"), result.getData().getExtras().getString("ScheduleDate"));

                        backStack.add(new BackStack(null, null, null, null, new ScheduleBackStack(result.getData().getExtras().getString("LineNum"), result.getData().getExtras().getString("Direction"), result.getData().getExtras().getString("ScheduleDate"),result.getData().getExtras().getInt("StopId"),result.getData().getExtras().getBoolean("PreSelected")), false));
                    }
                }
    });

    private void SetupSearchView() {
        searchView = findViewById(R.id.search_view);
        SearchBar searchBar = findViewById(R.id.search_bar);

        searchView.setupWithSearchBar(searchBar);



        searchView.addTransitionListener(
        (searchView, previousState, newState) -> {
            if (newState == SearchView.TransitionState.SHOWING) {
                List<SearchResult> AllItemList = new ArrayList<>();

                for (int i = 0; i < busPlaces.length; i++) {
                    AllItemList.add(new SearchResult(SearchResult.SearchType.Stop, busPlaces[i].getName(), busPlaces[i]));
                }

                DatabaseManager Dm = new DatabaseManager(this);

                BusLine[] BusLines = Dm.GetActiveBusLines();

                for (int i = 0; i < BusLines.length; i++) {
                    AllItemList.add(new SearchResult(SearchResult.SearchType.Line, BusLines[i].getLineName(), BusLines[i]));
                }

                SearchResult[] AllItem = new SearchResult[AllItemList.size()];

                AllItemList.toArray(AllItem);

                Svf = SearchViewFragment.newInstance(AllItem);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.SerchViewFragmentContainer, Svf)
                        .commit();
            }
        });

        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Svf.OnSearchTextChanged(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    public void OnSearchResultClick(SearchResult searchResult) {
        searchView.hide();

        if (searchResult.getType() == SearchResult.SearchType.Line) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            ShowSchedule(-1, ((BusLine)searchResult.getData()).getLineName(), "O", formatter.format(date), true);
        } else if (searchResult.getType() == SearchResult.SearchType.FavStop) {
            SelectStop((int)searchResult.getData());
        } else if (searchResult.getType() == SearchResult.SearchType.Stop) {
            SelectPlace(((BusPlaces)searchResult.getData()).getId());
        }
    }
}