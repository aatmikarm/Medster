package com.aatmik.medster;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class productStatus extends AppCompatActivity implements OnMapReadyCallback {

    private TextView productStatus_deliveryPersonName_tv, productStatus_total_amount_tv, productStatus_product_name, productStaus_arrivalTime, userProductOTP_tv, productStatusView_tv, paymentType_tv;
    private ImageView productStatus_deliveryPerson_iv, productStatus_back_iv, productStatus_image_iv;
    private CardView productStatus_deliveryPersonCall;
    private FragmentManager ratingfragmentManager;
    private String currentUserUid, seller, sellerId, sellerPhone, productId, productOrderId, distanceBetweenUserAndSeller;
    String userDefaultImageUrl = "https://firebasestorage.googleapis.com/v0/b/test1photographer.appspot.com/o/default%2FuserDefault.png?alt=media&token=0f495f89-caa3-4bcb-b278-97548eb77490";

    private FirebaseFirestore mDb;
    private FirebaseAuth firebaseAuth;
    private StorageReference mStorageRef;

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private GeoPoint sellerGeoPoint, userGeoPoint;
    private Handler handler1;
    private Runnable runnable1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_status);


        firebaseAuth = FirebaseAuth.getInstance();
        mDb = FirebaseFirestore.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        currentUserUid = firebaseAuth.getUid();

        if (getIntent().getExtras() != null) {
            this.productId = (String) getIntent().getExtras().get("productId");
            this.productOrderId = (String) getIntent().getExtras().get("productOrderId");
            this.sellerId = (String) getIntent().getExtras().get("sellerId");
            this.seller = (String) getIntent().getExtras().get("seller");
        }

        productStatus_deliveryPersonName_tv = findViewById(R.id.productStatus_deliveryPersonName_tv);
        productStaus_arrivalTime = findViewById(R.id.productStaus_arrivalTime);
        productStatus_deliveryPerson_iv = findViewById(R.id.productStatus_deliveryPerson_iv);
        productStatus_back_iv = findViewById(R.id.productStatus_back_iv);
        productStatus_image_iv = findViewById(R.id.productStatus_image_iv);
        productStatus_deliveryPersonCall = findViewById(R.id.productStatus_deliveryPersonCall);
        userProductOTP_tv = findViewById(R.id.userProductOTP_tv);
        productStatus_product_name = findViewById(R.id.productStatus_product_name);
        productStatus_total_amount_tv = findViewById(R.id.productStatus_total_amount_tv);
        productStatusView_tv = findViewById(R.id.productStatusView_tv);
        paymentType_tv = findViewById(R.id.paymentType_tv);

        if (savedInstanceState == null) {
            ratingFragment ratingFragment = new ratingFragment();
            Bundle bundle = new Bundle();
            bundle.putString("productId", productId);
            ratingFragment.setArguments(bundle);
            ratingfragmentManager = getSupportFragmentManager();
            ratingfragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .add(R.id.productStatus_rating_fcv, ratingFragment.class, bundle)
                    .commit();
        }

        productStatus_deliveryPersonCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", sellerPhone, null));
                startActivity(intent);
            }
        });

        productStatus_back_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.productStatusMap);
        mapFragment.getMapAsync(this);
        getSellerDetails();
        handler1 = new Handler();
        handler1.postDelayed(runnable1 = new Runnable() {
            @Override
            public void run() {
                if (userGeoPoint != null && sellerGeoPoint != null) {
                    double distance = distanceBetweenUserAndSeller(userGeoPoint.getLatitude(), userGeoPoint.getLongitude(),
                            sellerGeoPoint.getLatitude(), sellerGeoPoint.getLongitude());
                    // 5 is 5meter per second speed and 60 is seconds conversion
                    double timeInSeconds = (distance / 5);
                    productStaus_arrivalTime.setText(convertSecondsToTime(timeInSeconds) + " Min");
                }
                handler1.postDelayed(this, 10000);
            }
        }, 5000);

        showOTP();

    }

    private void showOTP() {
        mDb.collection("users")
                .document(currentUserUid)
                .collection("orders")
                .document(productOrderId)
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        userProductOTP_tv.setText((String) document.get("otp").toString());
                        productStatusView_tv.setText((String) document.get("status").toString());
                        paymentType_tv.setText((String) document.get("paymentType").toString());

                        String totalProductPrice = String.valueOf(Float.parseFloat((String) document.get("price").toString()) *
                                Float.parseFloat((String) document.get("productQuantity").toString()));

                        productStatus_total_amount_tv.setText("Rs. " + (String) document.get("price").toString() + " * "
                                + (String) document.get("productQuantity").toString() + " = Rs. " + totalProductPrice);
                        productStatus_product_name.setText((String) document.get("name").toString());

                        Glide.with(productStatus.this).load((String) document.get("imageUrl").toString()).into(productStatus_image_iv);

                    } else {
                        Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "failed ", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private String convertSecondsToTime(double timeInSeconds) {
        int hours = (int) (timeInSeconds / 3600);
        int minutes = (int) ((timeInSeconds % 3600) / 60);
        int seconds = (int) (timeInSeconds % 60);
        String timeString = String.format("%02d:%02d", hours, minutes);
        return timeString;
    }

    private double distanceBetweenUserAndSeller(double lat1, double lon1, double lat2, double lon2) {
        Location startPoint = new Location("locationA");
        startPoint.setLatitude(lat1);
        startPoint.setLongitude(lon1);
        Location endPoint = new Location("locationA");
        endPoint.setLatitude(lat2);
        endPoint.setLongitude(lon2);
        double distance = startPoint.distanceTo(endPoint);
        return distance;
    }

    private void getSellerDetails() {
        mDb.collection("sellers")
                .document(sellerId)
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        seller = document.get("name").toString();
                        productStatus_deliveryPersonName_tv.setText(seller);
                        Glide.with(productStatus.this).load(document.get("imageUrl").toString()).into(productStatus_deliveryPerson_iv);
                        sellerPhone = document.get("phone").toString();
                    } else {
                        Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "failed ", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.map = map;
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @SuppressLint("MissingPermission")
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    Location location = task.getResult();
                    if (location != null) {
                        //Toast.makeText(productStatus.this, "location = " + location.getLatitude(), Toast.LENGTH_SHORT).show();
                        map.setMyLocationEnabled(true);
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16.0f));
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        Map<String, Object> updateUserLocation = new HashMap<>();
                        updateUserLocation.put("geo_point", geoPoint);
                        mDb.collection("users").document(currentUserUid).update(updateUserLocation);
                        mDb.collection("users").document(currentUserUid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        StorageReference ref = mStorageRef.child("images/" + currentUserUid).child("profilepic.jpg");
                                        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                Glide.with(productStatus.this)
                                                        .asBitmap()
                                                        .load(uri)
                                                        .into(new CustomTarget<Bitmap>() {
                                                            @Override
                                                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                                                Bitmap bitmap = getCircularBitmap(resource);
                                                                GeoPoint geoPoint = (GeoPoint) document.get("geo_point");
                                                                userGeoPoint = geoPoint;
                                                                double lat = geoPoint.getLatitude();
                                                                double lng = geoPoint.getLongitude();
                                                                LatLng latLng = new LatLng(lat, lng);
                                                                map.addMarker(new MarkerOptions()
                                                                        .position(latLng)
                                                                        .icon(BitmapDescriptorFactory
                                                                                .fromBitmap(createCustomMarkerForUser
                                                                                        (productStatus.this,
                                                                                                bitmap)))
                                                                        .title(String.valueOf(document.get("name")))
                                                                        .zIndex(1.0f));
                                                            }

                                                            @Override
                                                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                                            }
                                                        });
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Glide.with(productStatus.this)
                                                        .asBitmap()
                                                        .load(userDefaultImageUrl)
                                                        .into(new CustomTarget<Bitmap>() {
                                                            @Override
                                                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                                                                Bitmap bitmap = getCircularBitmap(resource);
                                                                GeoPoint geoPoint = (GeoPoint) document.get("geo_point");
                                                                userGeoPoint = geoPoint;
                                                                double lat = geoPoint.getLatitude();
                                                                double lng = geoPoint.getLongitude();
                                                                LatLng latLng = new LatLng(lat, lng);
                                                                map.addMarker(new MarkerOptions()
                                                                        .position(latLng)
                                                                        .icon(BitmapDescriptorFactory
                                                                                .fromBitmap(createCustomMarkerForUser
                                                                                        (productStatus.this,
                                                                                                bitmap)))
                                                                        .title("user")
                                                                        .zIndex(1.0f));
                                                            }

                                                            @Override
                                                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                                            }
                                                        });
                                            }
                                        });
                                    } else {
                                        Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(getApplicationContext(), "failed ", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                        mDb.collection("sellers").document(sellerId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        StorageReference ref = mStorageRef.child("images/" + sellerId).child("profilepic.jpg");
                                        ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                Glide.with(productStatus.this)
                                                        .asBitmap()
                                                        .load(uri)
                                                        .into(new CustomTarget<Bitmap>() {
                                                            @Override
                                                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                                                Bitmap bitmap = getCircularBitmap(resource);
                                                                GeoPoint geoPoint = (GeoPoint) document.get("geo_point");
                                                                sellerGeoPoint = geoPoint;
                                                                double lat = geoPoint.getLatitude();
                                                                double lng = geoPoint.getLongitude();
                                                                LatLng latLng = new LatLng(lat, lng);
                                                                map.addMarker(new MarkerOptions()
                                                                        .position(latLng)
                                                                        .icon(BitmapDescriptorFactory
                                                                                .fromBitmap(createCustomMarkerForUser
                                                                                        (productStatus.this,
                                                                                                bitmap)))
                                                                        .title(String.valueOf(document.get("name")))
                                                                        .zIndex(1.0f));
                                                            }

                                                            @Override
                                                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                                            }
                                                        });
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Glide.with(productStatus.this)
                                                        .asBitmap()
                                                        .load(userDefaultImageUrl)
                                                        .into(new CustomTarget<Bitmap>() {
                                                            @Override
                                                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                                                Bitmap bitmap = getCircularBitmap(resource);
                                                                GeoPoint geoPoint = (GeoPoint) document.get("geo_point");
                                                                sellerGeoPoint = geoPoint;
                                                                double lat = geoPoint.getLatitude();
                                                                double lng = geoPoint.getLongitude();
                                                                LatLng latLng = new LatLng(lat, lng);
                                                                map.addMarker(new MarkerOptions()
                                                                        .position(latLng)
                                                                        .icon(BitmapDescriptorFactory
                                                                                .fromBitmap(createCustomMarkerForUser
                                                                                        (productStatus.this,
                                                                                                bitmap)))
                                                                        .title("Seller")
                                                                        .zIndex(1.0f));
                                                            }

                                                            @Override
                                                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                                            }
                                                        });
                                            }
                                        });
                                    } else {
                                        Toast.makeText(getApplicationContext(), "error", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Toast.makeText(getApplicationContext(), "failed ", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    public static Bitmap getCircularBitmap(Bitmap bitmap) {
        Bitmap output;

        if (bitmap.getWidth() > bitmap.getHeight()) {
            output = Bitmap.createBitmap(bitmap.getHeight(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        } else {
            output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getWidth(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        float r = 0;

        if (bitmap.getWidth() > bitmap.getHeight()) {
            r = bitmap.getHeight() / 2;
        } else {
            r = bitmap.getWidth() / 2;
        }

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(r, r, r, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    public Bitmap createCustomMarkerForUser(Context context, Bitmap resource) {
        View marker = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.custom_user_marker_layout, null);
        final CircleImageView markerImage = (CircleImageView) marker.findViewById(R.id.user_dp);
        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), resource);
        roundedBitmapDrawable.setCornerRadius(50.0f);
        roundedBitmapDrawable.setAntiAlias(true);
        markerImage.setImageDrawable(roundedBitmapDrawable);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        marker.setLayoutParams(new ViewGroup.LayoutParams(52, ViewGroup.LayoutParams.WRAP_CONTENT));
        marker.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        marker.buildDrawingCache();
        Bitmap bitmap = Bitmap.createBitmap(marker.getMeasuredWidth(),
                marker.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        marker.draw(canvas);
        return bitmap;
    }

}