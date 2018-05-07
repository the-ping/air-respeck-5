package com.specknet.airrespeck.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.specknet.airrespeck.models.AirspeckData;
import com.specknet.airrespeck.models.LocationData;
import com.specknet.airrespeck.remote.OpenWeatherAPIClient;
import com.specknet.airrespeck.remote.OpenWeatherAPIService;
import com.specknet.airrespeck.remote.OpenWeatherData;

import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IndoorOutdoorPredictor {

    private Deque<Float> speedQueue = new LinkedList<>();
    private Deque<Float> pm25Queue = new LinkedList<>();

    private float avgSpeed = Float.NaN;
    private float varPM25 = Float.NaN;
    private PlaceLikelihood currentPlaceLikelihood;
    private OpenWeatherData currentWeather;

    private AirspeckData previousSensorReadings;
    private float previousIndoorLikelihood = Float.NaN;

    private final static int EXPIRY_TIME = 1000 * 60 * 3;
    private final static int SPEED_QUEUE_LENGTH = 12;
    private final static int PM25_QUEUE_LENGTH = 30;
    private final static int UPDATE_INTERVAL_WEATHER = 1000 * 60 * 20; // 20 minutes
    private final static int UPDATE_INTERVAL_PlACES = 1000 * 80; // 80 seconds. There is a limit on Google Place SDK
    // of 150000 per project per month, which will be reached quickly if set much lower.

    public IndoorOutdoorPredictor() {
        // Start tasks updating weather and place data

    }

    public float getIndoorLikelihood(AirspeckData currentSensorReadings) {

        // If there is no previous reading, or it was too far in the past, empty the queues.
        if (previousSensorReadings == null ||
                previousSensorReadings.getPhoneTimestamp() - currentSensorReadings.getPhoneTimestamp() <= EXPIRY_TIME) {
            speedQueue.clear();
            pm25Queue.clear();

            avgSpeed = Float.NaN;
            varPM25 = Float.NaN;
        } else {
            // Update queue-based values
            updateAvgSpeed(currentSensorReadings);
            updateVarPM25(currentSensorReadings);
        }

        // Calculate the predictions. 1 is indoor, 0 is outdoor
        float finalScore = 0;

        // Weights of all the features. Has to sum up to 1
        final float weightPreviousLikelihood = 0.2f;
        final float weightGPSAccuracy = 0.28f;
        final float weightGPSSpeed = 0.08f;
        final float weightGPSGooglePlaces = 0.08f;
        final float weightLuxLevel = 0.16f;
        final float weightTemperature = 0.08f;
        final float weightHumidity = 0.08f;
        final float weightPM = 0.04f;

        // Previous likelihood
        if (!Float.isNaN(previousIndoorLikelihood)) {
            finalScore += previousIndoorLikelihood * weightPreviousLikelihood;
        } else {
            // Indifferent if no values
            finalScore += 0.5 * weightPreviousLikelihood;
        }

        // GPS accuracy
        if (currentSensorReadings.getLocation().getAccuracy() > 20) {
            // We are confident we are indoors. Add full weight to score.
            finalScore += weightGPSAccuracy;
        } else if (currentSensorReadings.getLocation().getAccuracy() > 10) {
            // We are more likely indoor than outdoor. Add 60% of weight.
            finalScore += 0.6 * weightGPSAccuracy;
        }

        // GPS speed
        if (!Float.isNaN(avgSpeed) && avgSpeed > 5) {
            finalScore += weightGPSSpeed;
        } else {
            // If avg speed is nan or too low, we are indifferent
            finalScore += 0.5 * weightGPSSpeed;
        }

        // GPS Google Places
        if (currentPlaceLikelihood != null) {
            // Determine logic of this. Which place ID counts as indoor? For now: indifferent.
            finalScore += 0.5 * weightGPSGooglePlaces;
        } else {
            // Indifferent
            finalScore += 0.5 * weightGPSGooglePlaces;
        }

        // Lux level
        if (isDayMeasuredBySunset()) {
            if (currentSensorReadings.getLux() < 40) {
                // It's day but not bright -> probably indoor
                finalScore += weightLuxLevel;
            }
        } else {
            if (currentSensorReadings.getLux() > 5) {
                // It's night but bright -> probably indoor
                finalScore += weightLuxLevel;
            }
        }

        // Temperature
        if (currentSensorReadings.getTemperature() >= 17 && currentSensorReadings.getTemperature() <= 23 &&
                (currentWeather.getTemperature() < 17 || currentWeather.getTemperature() > 23)) {
            // Sensor reports temperature in range 17-23°, but outside temperature is not in this range -> indoor!
            finalScore += weightTemperature;
        } else if (currentSensorReadings.getTemperature() - currentWeather.getTemperature() > 5) {
            // Temperature from sensor disagrees with outside temperature by more than 5 -> indoor!
            finalScore += weightTemperature;
        }

        // Humidity
        if (currentSensorReadings.getHumidity() - currentWeather.getHumidity() > 8) {
            // Humidity reported by sensor is more than 8% apart from humidity reported by weather API
            finalScore += weightHumidity;
        } else {
            // We can't be sure
            finalScore += 0.5 * weightHumidity;
        }

        // PM 2.5
        if (!Float.isNaN(varPM25) && varPM25 < 25) {
            finalScore += weightPM;
        } else {
            finalScore += 0.5 * weightPM;
        }

        // Store current sensor reading as previous one
        previousSensorReadings = currentSensorReadings;

        previousIndoorLikelihood = finalScore;
        return finalScore;
    }

    private boolean isDayMeasuredBySunset() {
        long now = Utils.getUnixTimestamp() / 1000;
        // Are we currently between sunrise and sunset?
        return (currentWeather.getSun().getSunrise() < now && now < currentWeather.getSun().getSunset());
    }

    private void updateAvgSpeed(AirspeckData currentSensorReadings) {
        if (speedQueue.size() < SPEED_QUEUE_LENGTH) {
            avgSpeed = Float.NaN;
        } else if (Float.isNaN(avgSpeed)) {
            // Calculate speed for the first time
            avgSpeed = Utils.mean(speedQueue.toArray(new Float[SPEED_QUEUE_LENGTH]));
        } else {
            // Update the list
            speedQueue.removeFirst();
            speedQueue.addLast(calculateSpeed(previousSensorReadings.getLocation(), currentSensorReadings.getLocation(),
                    (int) ((currentSensorReadings.getPhoneTimestamp() -
                            previousSensorReadings.getPhoneTimestamp()) / 1000.)));

            // Update the running avg
            avgSpeed -= speedQueue.getFirst() / SPEED_QUEUE_LENGTH;
            avgSpeed += speedQueue.getLast() / SPEED_QUEUE_LENGTH;
        }
    }

    private void updateVarPM25(AirspeckData currentSensorReadings) {
        if (pm25Queue.size() < PM25_QUEUE_LENGTH) {
            varPM25 = Float.NaN;
        } else {
            // Update queue
            pm25Queue.removeFirst();
            pm25Queue.addLast(currentSensorReadings.getPm2_5());

            // As the queues are quite small, just re-calculate the variance each step
            varPM25 = Utils.variance(pm25Queue.toArray(new Float[PM25_QUEUE_LENGTH]));
        }
    }

    private Float calculateSpeed(LocationData loc1, LocationData loc2, int timeDifferenceSeconds) {
        float distance = distanceBetweenLocations(loc1.getLatitude(), loc1.getLongitude(), loc2.getLatitude(),
                loc2.getLongitude());

        return (float) (distance / timeDifferenceSeconds * 3.6);
    }

    private static float distanceBetweenLocations(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (float) (earthRadius * c);
    }


    private void updateWeather() {
        Log.i("Weather", "Making request");
        OpenWeatherAPIService openWeatherAPIService = OpenWeatherAPIClient.getOpenWeatherAPIService();

        openWeatherAPIService.getWeather(49.801969, 9.947268).enqueue(
                new Callback<OpenWeatherData>() {
                    @Override
                    public void onResponse(Call<OpenWeatherData> call, Response<OpenWeatherData> response) {
                        if (response.isSuccessful()) {
                            currentWeather = response.body();
                            Log.i("Weather", "Response: " + response.body());
                        } else {
                            currentWeather = null;
                        }
                    }

                    @Override
                    public void onFailure(Call<OpenWeatherData> call, Throwable t) {
                        Log.e("Weather", "Unable to submit weather request: " + Log.getStackTraceString(t));
                        currentWeather = null;
                    }
                });
    }

    /* Updates the place the subject is most likely at currently via the Google places SDK.
    Note that this can only be updated about every 80 seconds as there is an API call limit per project
    of 150000 calls.
     */
    @SuppressLint("MissingPermission")
    private void updateCurrentPlaceLikelihood(Context context) {
        final GoogleApiClient mGoogleApiClient = new GoogleApiClient
                .Builder(context)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();

        PlaceDetectionClient client = Places.getPlaceDetectionClient(context);
        Task<PlaceLikelihoodBufferResponse> placeResult = client.getCurrentPlace(null);

        placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();
                currentPlaceLikelihood = likelyPlaces.iterator().next();

                /* Iterate through all likely places
                for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                    Log.i("Places",
                            String.format(Locale.UK, "Place '%s' has likelihood: %g",
                                    placeLikelihood.getPlace().getPlaceTypes(),
                                    placeLikelihood.getLikelihood()));

                }*/

                likelyPlaces.release();
            }
        });
    }
}
