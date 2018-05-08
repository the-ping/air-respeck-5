package com.specknet.airrespeck.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

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

import java.util.Deque;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IndoorOutdoorPredictor {

    private Deque<Float> speedQueue = new LinkedList<>();
    private Deque<Float> pm25Queue = new LinkedList<>();

    private float avgSpeed = Float.NaN;
    private float varPM25 = Float.NaN;
    private float currentPlaceLikelihood = Float.NaN;
    private OpenWeatherData currentWeather;

    // Store the individual scores here. At the beginning, we are indifferent about each.
    private float previousIndoorLikelihood = 0.5f;
    private float gpsAccuracyScore = 0.5f;
    private float gpsSpeedScore = 0.5f;
    private float gpsGooglePlacesScore = 0.5f;
    private float luxLevelScore = 0.5f;
    private float temperatureScore = 0.5f;
    private float humidityScore = 0.5f;
    private float pmScore = 0.5f;

    private AirspeckData previousSensorReadings;

    private final static int EXPIRY_TIME = 1000 * 60 * 3;
    private final static int SPEED_QUEUE_LENGTH = 12;
    private final static int PM25_QUEUE_LENGTH = 12;
    private final static int UPDATE_INTERVAL_WEATHER = 1000 * 60 * 20; // 20 minutes
    private final static int UPDATE_INTERVAL_PLACES = 1000 * 80; // 80 seconds. There is a limit on Google Place SDK
    // of 150000 per project per month, which will be reached quickly if set much lower.

    // Weights of all the features. Has to sum up to 1
    private final float WEIGHT_PREVIOUS_LIKELIHOOD = 0.2f;
    private final float WEIGHT_GPS_ACCURACY = 0.28f;
    private final float WEIGHT_GPS_SPEED = 0.08f;
    private final float WEIGHT_GPS_PLACES = 0.08f;
    private final float WEIGHT_LUX_LEVEL = 0.16f;
    private final float WEIGHT_TEMPERATURE = 0.1f;
    private final float WEIGHT_HUMIDITY = 0.06f;
    private final float WEIGHT_PM = 0.04f;

    public IndoorOutdoorPredictor(final Context context) {
        // Start tasks updating weather and place data
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateCurrentPlaceLikelihood(context);
            }
        }, 0, UPDATE_INTERVAL_PLACES);

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateWeather();
            }
        }, 0, UPDATE_INTERVAL_WEATHER);

    }

    public void updateScores(AirspeckData currentSensorReadings) {
        // Before updating the scores, save the current combined score
        previousIndoorLikelihood = getIndoorLikelihood();

        // If there is no previous reading, or it was too far in the past, empty the queues.
        if (previousSensorReadings == null ||
                currentSensorReadings.getPhoneTimestamp() - previousSensorReadings.getPhoneTimestamp() > EXPIRY_TIME) {
            speedQueue.clear();
            pm25Queue.clear();

            avgSpeed = Float.NaN;
            varPM25 = Float.NaN;
        } else {
            // Update queue-based values
            Log.i("Predictor", "update queues");
            updateAvgSpeed(currentSensorReadings);
            updateVarPM25(currentSensorReadings);
        }

        // GPS accuracy
        if (currentSensorReadings.getLocation().getAccuracy() > 20) {
            // We are confident we are indoors. Add full weight to score.
            gpsAccuracyScore = 1.0f;
        } else if (currentSensorReadings.getLocation().getAccuracy() > 10) {
            // We are more likely indoor than outdoor. Add 60% of weight.
            gpsAccuracyScore = 0.6f;
        } else {
            gpsAccuracyScore = 0.0f;
        }

        // GPS speed
        if (!Float.isNaN(avgSpeed) && avgSpeed > 5) {
            gpsSpeedScore = 1.0f;
        } else {
            // If avg speed is nan or too low, we are indifferent
            gpsSpeedScore = 0.5f;
        }

        // GPS Google Places
        if (!Float.isNaN(currentPlaceLikelihood)) {
            // Determine logic of this. Which place ID counts as indoor? For now: likelihood of most likely place
            gpsGooglePlacesScore = currentPlaceLikelihood;
        } else {
            // Indifferent
            gpsGooglePlacesScore = 0.5f;
        }

        // Lux level
        if (currentWeather != null) {
            if (isDayMeasuredBySunset()) {
                if (currentSensorReadings.getLux() < 40) {
                    // It's day but not bright -> probably indoor
                    luxLevelScore = 1.0f;
                } else {
                    luxLevelScore = 0.0f;
                }
            } else {
                if (currentSensorReadings.getLux() > 5) {
                    // It's night but bright -> probably indoor
                    luxLevelScore = 1.0f;
                } else {
                    luxLevelScore = 0.0f;
                }
            }
        } else {
            luxLevelScore = 0.5f;
        }

        // Temperature
        if (currentWeather != null) {
            if (currentSensorReadings.getTemperature() >= 17 && currentSensorReadings.getTemperature() <= 23 &&
                    (currentWeather.getTemperature() < 17 || currentWeather.getTemperature() > 23)) {
                // Sensor reports temperature in range 17-23°, but outside temperature is not in this range -> indoor!
                temperatureScore = 1.0f;
            } else if (currentSensorReadings.getTemperature() - currentWeather.getTemperature() > 5) {
                // Temperature from sensor disagrees with outside temperature by more than 5 -> indoor!
                temperatureScore = 1.0f;
            } else {
                temperatureScore = 0.0f;
            }
        } else {
            temperatureScore = 0.5f;
        }

        // Humidity
        if (currentSensorReadings.getHumidity() - currentWeather.getHumidity() > 8) {
            // Humidity reported by sensor is more than 8% apart from humidity reported by weather API
            humidityScore = 1.0f;
        } else {
            // We can't be sure
            humidityScore = 0.5f;
        }

        // PM 2.5
        Log.i("Predictor", "Variance PM 2.5: " + varPM25);
        if (!Float.isNaN(varPM25) && varPM25 < 15) {
            pmScore = 1.0f;
        } else {
            pmScore = 0.5f;
        }

        // Store current sensor reading as previous one
        previousSensorReadings = currentSensorReadings;
    }

    public float getIndoorLikelihood() {
        // Calculate the predictions. 1 is indoor, 0 is outdoor
        float combinedScore = 0;

        combinedScore += previousIndoorLikelihood * WEIGHT_PREVIOUS_LIKELIHOOD;
        combinedScore += gpsAccuracyScore * WEIGHT_GPS_ACCURACY;
        combinedScore += gpsSpeedScore * WEIGHT_GPS_SPEED;
        combinedScore += gpsGooglePlacesScore * WEIGHT_GPS_PLACES;
        combinedScore += luxLevelScore * WEIGHT_LUX_LEVEL;
        combinedScore += temperatureScore * WEIGHT_TEMPERATURE;
        combinedScore += humidityScore * WEIGHT_HUMIDITY;
        combinedScore += pmScore * WEIGHT_PM;

        return combinedScore;
    }

    private boolean isDayMeasuredBySunset() {
        long now = Utils.getUnixTimestamp() / 1000;
        // Are we currently between sunrise and sunset?
        return (currentWeather.getSun().getSunrise() < now && now < currentWeather.getSun().getSunset());
    }

    private void updateAvgSpeed(AirspeckData currentSensorReadings) {
        speedQueue.addLast(calculateSpeed(previousSensorReadings.getLocation(), currentSensorReadings.getLocation(),
                (int) ((currentSensorReadings.getPhoneTimestamp() -
                        previousSensorReadings.getPhoneTimestamp()) / 1000.)));

        if (speedQueue.size() < SPEED_QUEUE_LENGTH) {
            avgSpeed = Float.NaN;
        } else {
            // Truncate queue
            while (speedQueue.size() > SPEED_QUEUE_LENGTH) {
                speedQueue.removeFirst();
            }

            // Calculate mean
            avgSpeed = Utils.mean(speedQueue.toArray(new Float[SPEED_QUEUE_LENGTH]));
        }
    }

    private void updateVarPM25(AirspeckData currentSensorReadings) {
        pm25Queue.addLast(currentSensorReadings.getPm2_5());

        if (pm25Queue.size() < PM25_QUEUE_LENGTH) {
            varPM25 = Float.NaN;
        } else {
            // Truncate queue
            while (pm25Queue.size() > PM25_QUEUE_LENGTH) {
                pm25Queue.removeFirst();
            }

            // Calculate variance
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
        PlaceDetectionClient client = Places.getPlaceDetectionClient(context);
        Task<PlaceLikelihoodBufferResponse> placeResult = client.getCurrentPlace(null);

        placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();
                currentPlaceLikelihood = likelyPlaces.iterator().next().getLikelihood();

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

    @Override
    public String toString() {
        return String.format(Locale.UK, "Current indoor likelihood: %.1f\nPrevious indoor likelihood: " +
                        "%.1f\nGPS Accuracy: %.1f\nGPS Speed: %.1f\nGoogle places: %.1f\nLux level: %.1f\nTemperature: " +
                        "%.1f\nHumidity: %.1f\nPM 2.5: %.1f", getIndoorLikelihood(), getPreviousIndoorLikelihood(),
                getGpsAccuracyScore(), getGpsSpeedScore(), getGpsGooglePlacesScore(), getLuxLevelScore(),
                getTemperatureScore(), getHumidityScore(), getPmScore());
    }

    public float getPreviousIndoorLikelihood() {
        return previousIndoorLikelihood;
    }

    public float getGpsAccuracyScore() {
        return gpsAccuracyScore;
    }

    public float getGpsSpeedScore() {
        return gpsSpeedScore;
    }

    public float getGpsGooglePlacesScore() {
        return gpsGooglePlacesScore;
    }

    public float getLuxLevelScore() {
        return luxLevelScore;
    }

    public float getTemperatureScore() {
        return temperatureScore;
    }

    public float getHumidityScore() {
        return humidityScore;
    }

    public float getPmScore() {
        return pmScore;
    }
}
