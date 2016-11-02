package com.example.android.sunshine.app;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WeatherDataService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private GoogleApiClient mGoogleApiClient;
    public static String WEATHER_DATA = "sunshine_weather_data";
    public static final String WEATHER_PATH = "/weather-data";
    public static final String WEATHER_ID = "weather_id";
    public static final String WEATHER_HIGH = "weather_high";
    public static final String WEATHER_LOW = "weather_low";

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("Yes", "Yes");
        for(DataEvent dataEvent : dataEvents) {
            if(dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                String path = dataEvent.getDataItem().getUri().getPath();
                if(path.equals(WeatherDataService.WEATHER_PATH)) {
                    int weatherId = dataMap.getInt(WeatherDataService.WEATHER_ID);
                    String weatherHigh = dataMap.getString(WeatherDataService.WEATHER_HIGH);
                    String weatherLow = dataMap.getString(WeatherDataService.WEATHER_LOW);
                    updateWeather(weatherId, weatherHigh,weatherLow);

                }
            }
        }
    }

    private void updateWeather(int weatherId, String high, String low) {
        SharedPreferences sharedPref = this.getSharedPreferences(WeatherDataService.WEATHER_DATA, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(WeatherDataService.WEATHER_ID, weatherId);
        editor.putString(WeatherDataService.WEATHER_HIGH, high);
        editor.putString(WeatherDataService.WEATHER_LOW, low);
        editor.apply();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
