package com.elvis.cliente.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

import com.elvis.cliente.R;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.elvis.cliente.models.ClientBooking;
import com.elvis.cliente.models.DriverFound;
import com.elvis.cliente.models.FCMBody;
import com.elvis.cliente.models.FCMResponse;
import com.elvis.cliente.providers.AuthProvider;
import com.elvis.cliente.providers.ClientBookingProvider;
import com.elvis.cliente.providers.DriversFoundProvider;
import com.elvis.cliente.providers.GeofireProvider;
import com.elvis.cliente.providers.GoogleApiProvider;
import com.elvis.cliente.providers.NotificationProvider;
import com.elvis.cliente.providers.TokenProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestDriverActivity extends AppCompatActivity {

    private LottieAnimationView mAnimation;
    private TextView mTextViewLookingFor;
    private Button mButtonCancelRequest;
    private GeofireProvider mGeofireProvider;

    private String mExtraOrigin;
    private String mExtraDestination;
    private double mExtraOriginLat;
    private double mExtraOriginLng;
    private double mExtraDestinationLat;
    private double mExtraDestinationLng;
    private LatLng mOriginLatLng;
    private LatLng mDestinationLatLng;

    private double mRadius = 3;
    private boolean mDriverFound = false;
    private String  mIdDriverFound = "";
    private LatLng mDriverFoundLatLng;
    private NotificationProvider mNotificationProvider;
    private TokenProvider mTokenProvider;
    private ClientBookingProvider mClientBookingProvider;
    private AuthProvider mAuthProvider;
    private GoogleApiProvider mGoogleApiProvider;
    private DriversFoundProvider mDriversFoundProvider;

    private ValueEventListener mListener;

    private ArrayList<String> mDriversNotAccept = new ArrayList<>();

    private ArrayList<String> mDriversFoundList = new ArrayList<>();
    private List<String> mTokenList = new ArrayList<>();

    private int mTimeLimit = 0;
    private Handler mHandler = new Handler();
    private boolean mIsFinishSearch = false;
    private boolean mIsLookingFor = false;

    private int mCounter = 0;
    private int mCounterDriversAvailable = 0;

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (mTimeLimit < 35) {
                mTimeLimit++;
                mHandler.postDelayed(mRunnable, 1000);
            }
            else {
                deleteDriversFound();
                cancelRequest();
                mHandler.removeCallbacks(mRunnable);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_driver);

        mAnimation = findViewById(R.id.animation);
        mTextViewLookingFor = findViewById(R.id.textViewLookingFor);
        mButtonCancelRequest = findViewById(R.id.btnCancelRequest);

        mAnimation.playAnimation();

        mExtraOrigin = getIntent().getStringExtra("origin");
        mExtraDestination = getIntent().getStringExtra("destination");
        mExtraOriginLat = getIntent().getDoubleExtra("origin_lat", 0);
        mExtraOriginLng = getIntent().getDoubleExtra("origin_lng", 0);
        mExtraDestinationLat = getIntent().getDoubleExtra("destination_lat", 0);
        mExtraDestinationLng = getIntent().getDoubleExtra("destination_lng", 0);
        mOriginLatLng = new LatLng(mExtraOriginLat, mExtraOriginLng);
        mDestinationLatLng= new LatLng(mExtraDestinationLat, mExtraDestinationLng);

        mGeofireProvider = new GeofireProvider("active_drivers");
        mTokenProvider = new TokenProvider();
        mNotificationProvider = new NotificationProvider();
        mClientBookingProvider = new ClientBookingProvider();
        mAuthProvider = new AuthProvider();
        mGoogleApiProvider = new GoogleApiProvider(RequestDriverActivity.this);
        mDriversFoundProvider = new DriversFoundProvider();

        mButtonCancelRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteDriversFound();
                cancelRequest();
            }
        });

        getClosestDriver();
    }

    private void deleteDriversFound() {
        for (String idDriver: mDriversFoundList) {
            mDriversFoundProvider.delete(idDriver);
        }
    }

    private void cancelRequest() {

        mClientBookingProvider.delete(mAuthProvider.getId()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                sendNotificationCancel();
            }
        });

    }

    /**
     * RETORNAR SI EL ID DEL CODNDUCTOR ENCONTRADO YA CANCELO EL VIAJE
     * @param idDriver
     * @return
     */
    private boolean isDriverCancel(String idDriver) {
        for (String id: mDriversNotAccept) {
            if (id.equals(idDriver)) {
                return true;
            }
        }
        return false;
    }

    private void checkStatusClientBooking() {
        mListener = mClientBookingProvider.getClientBooking(mAuthProvider.getId()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String status = dataSnapshot.child("status").getValue().toString();
                    String idDriver = dataSnapshot.child("idDriver").getValue().toString();
                    if (status.equals("accept") && !idDriver.equals("")) {

                        sendNotificationCancelToDrivers(idDriver);

                        Intent intent = new Intent(RequestDriverActivity.this, MapClientBookingActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else if (status.equals("cancel")) {
                        /*
                        if (mIsLookingFor) {
                            restartRequest();
                        }

                         */

                        Toast.makeText(RequestDriverActivity.this, "El conductor no acepto el viaje", Toast.LENGTH_SHORT).show();
                        /*
                        Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
                        startActivity(intent);
                        finish();

                         */
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void restartRequest() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        mTimeLimit = 0;
        mIsLookingFor = false;
        mDriversNotAccept.add(mIdDriverFound);
        mDriverFound = false;
        mIdDriverFound = "";
        mRadius = 0.1f;
        mIsFinishSearch = false;
        mTextViewLookingFor.setText("BUSCANDO CONDUCTOR");

        getClosestDriver();
    }

    private void getClosestDriver() {
        mGeofireProvider.getActiveDrivers(mOriginLatLng, mRadius).addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(final String key, final GeoLocation location) {
                mTextViewLookingFor.setText("BUSCANDO CONDUCTOR...");
                mDriversFoundList.add(key);
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                // YA FINALIZA LA BUSQUEDA EN UN RADIO DE 3 KILOMETROS
                checkIfDriverIsAvailable();
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void getDriversToken() {

        if (mDriversFoundList.size() == 0) {
            getClosestDriver();
            return;
        }

        mTextViewLookingFor.setText("ESPERANDO RESPUESTA...");

        for (String id: mDriversFoundList) {
            mTokenProvider.getToken(id).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    mCounter = mCounter + 1;

                    if (snapshot.exists()) {
                        String token = snapshot.child("token").getValue().toString();
                        mTokenList.add(token);
                    }

                    if (mCounter == mDriversFoundList.size()) {
                        sendNotification("", "");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    private void sendNotification(final String time, final String km) {
        mTokenProvider.getToken(mIdDriverFound).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    //String token = dataSnapshot.child("token").getValue().toString();
                    Map<String, String> map = new HashMap<>();
                    map.put("title", "SOLICITUD DE SERVICIO A " + time + " DE TU POSICION");
                    map.put("body",
                            "Un cliente esta solicitando un servicio a una distancia de " + km + "\n" +
                                    "Recoger en: " + mExtraOrigin + "\n" +
                                    "Destino: " + mExtraDestination
                    );
                    map.put("idClient", mAuthProvider.getId());
                    map.put("origin", mExtraOrigin);
                    map.put("destination", mExtraDestination);
                    map.put("min", time);
                    map.put("distance", km);
                    map.put("searchById", "false");
                    FCMBody fcmBody = new FCMBody(mTokenList, "high", "4500s", map);
                    mNotificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                        @Override
                        public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                            ClientBooking clientBooking = new ClientBooking(
                                    mAuthProvider.getId(),
                                    "",
                                    mExtraDestination,
                                    mExtraOrigin,
                                    time,
                                    km,
                                    "create",
                                    mExtraOriginLat,
                                    mExtraOriginLng,
                                    mExtraDestinationLat,
                                    mExtraDestinationLng
                            );

                            // ESTAMOS RECORRIENDO LA LISTA DE LOS CONDUCTORES ENCONTRADOS PARA ALMACENARLOS EN FIREBASE
                            for (String idDriver: mDriversFoundList) {
                                DriverFound driverFound = new DriverFound(idDriver, mAuthProvider.getId());
                                mDriversFoundProvider.create(driverFound);
                            }

                            mClientBookingProvider.create(clientBooking).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mHandler.postDelayed(mRunnable, 1000);
                                    checkStatusClientBooking();
                                }
                            });

                        }

                        @Override
                        public void onFailure(Call<FCMResponse> call, Throwable t) {
                            Log.d("Error", "Error " + t.getMessage());
                        }
                    });
                }
                else {
                    Toast.makeText(RequestDriverActivity.this, "No se pudo enviar la notificacion porque el conductor no tiene un token de sesion", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void checkIfDriverIsAvailable () {
        for (String idDriver: mDriversFoundList) {
            mDriversFoundProvider.getDriverFoundByIdDriver(idDriver).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    mCounterDriversAvailable = mCounterDriversAvailable + 1;

                    for (DataSnapshot d: snapshot.getChildren()) {
                        if (d.exists()) {
                            String idDriver = d.child("idDriver").getValue().toString();
                            // ELIMINO DE LA LISTA DE CONDUCTORES ENCONTRADOS EL CONDUCTOR QUE YA EXISTE EN EL NODO
                            // DriversFound PARA NO ENVIARLE LA NOTIFICACION
                            mDriversFoundList.remove(idDriver);
                            mCounterDriversAvailable = mCounterDriversAvailable - 1;

                        }
                    }

                    // YA SABEMOS QUE LA CONSULTA TERMINO
                    // ASEGURAMOS DE NO ENVIARLE LA NOTIFICACION A LOS CONDUCTORES QUE YA ESTAN ACTUALMENTE RECIBIENDO LA
                    // NOTIFICACION
                    if (mCounterDriversAvailable == mDriversFoundList.size()) {
                        getDriversToken();
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    private void createClientBooking() {

        mGoogleApiProvider.getDirections(mOriginLatLng, mDriverFoundLatLng).enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                try {

                    JSONObject jsonObject = new JSONObject(response.body());
                    JSONArray jsonArray = jsonObject.getJSONArray("routes");
                    JSONObject route = jsonArray.getJSONObject(0);
                    JSONObject polylines = route.getJSONObject("overview_polyline");
                    String points = polylines.getString("points");
                    JSONArray legs =  route.getJSONArray("legs");
                    JSONObject leg = legs.getJSONObject(0);
                    JSONObject distance = leg.getJSONObject("distance");
                    JSONObject duration = leg.getJSONObject("duration");
                    String distanceText = distance.getString("text");
                    String durationText = duration.getString("text");
                    //sendNotification(durationText, distanceText);

                } catch(Exception e) {
                    Log.d("Error", "Error encontrado " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {

            }
        });


    }

    private void sendNotificationCancel() {

        if (mTokenList.size() > 0) {
            //String token = dataSnapshot.child("token").getValue().toString();
            Map<String, String> map = new HashMap<>();
            map.put("title", "VIAJE CANCELADO");
            map.put("body",
                    "El cliente cancelo la solicitud"
            );
            FCMBody fcmBody = new FCMBody(mTokenList, "high", "4500s", map);
            mNotificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                @Override
                public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                    Toast.makeText(RequestDriverActivity.this, "La solicitud se cancelo correctamente", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onFailure(Call<FCMResponse> call, Throwable t) {
                    Log.d("Error", "Error " + t.getMessage());
                }
            });
        }
        else {
            Toast.makeText(RequestDriverActivity.this, "La solicitud se cancelo correctamente", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(RequestDriverActivity.this, MapClientActivity.class);
            startActivity(intent);
            finish();
        }

    }

    private void sendNotificationCancelToDrivers(String idDriver) {

        if (mTokenList.size() > 0) {
            //String token = dataSnapshot.child("token").getValue().toString();
            Map<String, String> map = new HashMap<>();
            map.put("title", "VIAJE CANCELADO");
            map.put("body",
                    "El cliente cancelo la solicitud"
            );

            // ELIMINAR DE LA LISTA DE TOKEN
            // EL TOKEN DEL CONDUCTOR QUE ACEPTO EL VIAJE
            mTokenList.remove(idDriver);

            FCMBody fcmBody = new FCMBody(mTokenList, "high", "4500s", map);
            mNotificationProvider.sendNotification(fcmBody).enqueue(new Callback<FCMResponse>() {
                @Override
                public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {

                }

                @Override
                public void onFailure(Call<FCMResponse> call, Throwable t) {
                    Log.d("Error", "Error " + t.getMessage());
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mListener != null) {
            mClientBookingProvider.getStatus(mAuthProvider.getId()).removeEventListener(mListener);
        }

        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }

        mIsFinishSearch = true;
    }
}