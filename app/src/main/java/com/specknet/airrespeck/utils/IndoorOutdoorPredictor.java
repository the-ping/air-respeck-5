package com.specknet.airrespeck.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.os.Looper;
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
    private String currentPlaceIDs = "";
    private float gpsMaxSignalNoiseRatio = Float.NaN;
    private int gpsCountAbove30Snr = -1;
    private OpenWeatherData currentWeather;

    // Store the individual scores here. At the beginning, we are indifferent about each.
    private float previousIndoorLikelihood = 0.5f;
    private float gpsSignalStrengthScore = 0.5f;
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
    private final float WEIGHT_GPS_SIGNAL_STRENGTH = 0.23f;
    private final float WEIGHT_GPS_ACCURACY = 0.05f;
    private final float WEIGHT_GPS_SPEED = 0.1f;
    private final float WEIGHT_GPS_PLACES = 0.08f;
    private final float WEIGHT_LUX_LEVEL = 0.19f;
    private final float WEIGHT_TEMPERATURE = 0.15f;
    private final float WEIGHT_HUMIDITY = 0.0f;
    private final float WEIGHT_PM = 0.0f;

    @SuppressLint("MissingPermission")
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


        // GPS signal strength updates
        final LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        new Thread() {
            @Override
            public void run() {
                // The thread running here does not have a message queue by default. This message queue is needed,
                // however, to receive GpsStatus listening events. Therefore, create a message queue by calling Looper.prepare()
                // and later Looper.loop() to start looking for events.
                Looper.prepare();
                if (locationManager != null) {
                    locationManager.addGpsStatusListener(new GpsStatus.Listener() {
                        @Override
                        public void onGpsStatusChanged(int event) {
                            if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                                GpsStatus status = locationManager.getGpsStatus(null);
                                if (status != null) {
                                    float maxSnr = 0.0f;
                                    int countAbove30 = 0;
                                    for (GpsSatellite satellite : status.getSatellites()) {
                                        maxSnr = Math.max(satellite.getSnr(), maxSnr);
                                        if (satellite.getSnr() > 30) {
                                            countAbove30 += 1;
                                        }
                                    }
                                    // Log.i("Satellite", "Max snr: " + maxSnr);
                                    // Log.i("Satellite", "Count > 30: " + countAbove30);
                                    gpsMaxSignalNoiseRatio = maxSnr;
                                    gpsCountAbove30Snr = countAbove30;
                                }
                            }
                        }
                    });
                }
                Looper.loop();
            }
        }.start();
    }

    public void updateScores(AirspeckData currentSensorReadings, Context context) {
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
        // GPS signal strength
        if (gpsMaxSignalNoiseRatio <= 35 || gpsCountAbove30Snr < 3) {
            gpsSignalStrengthScore = 1.0f;
        } else {
            gpsSignalStrengthScore = 0.0f;
        }

        // GPS accuracy
        if (currentSensorReadings.getLocation().getAccuracy() > 20) {
            // We are confident we are indoors. Add full weight to score.
            gpsAccuracyScore = 1.0f;
        } else if (currentSensorReadings.getLocation().getAccuracy() > 10) {
            // We are more likely indoor than outdoor. Add 60% of weight.
            gpsAccuracyScore = 0.6f;
        } else {
            // We can still be indoors with a good accuracy when standing near window
            gpsAccuracyScore = 0.2f;
        }

        // GPS speed
        if (Float.isNaN(avgSpeed)) {
            gpsSpeedScore = 0.5f;
        } else if (avgSpeed <= 3) {
            gpsSpeedScore = 0.6f;
        } else if (avgSpeed <= 5) {
            // We are moving. Probably outdoor!
            gpsSpeedScore = 0.3f;
        } else {
            gpsSpeedScore = 0.0f;
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
                    luxLevelScore = 0.75f;
                } else {
                    // Brighter than 40 means most certainly sunlight
                    luxLevelScore = 0.0f;
                }
            } else {
                if (currentSensorReadings.getLux() > 5) {
                    // It's night but bright -> probably indoor
                    luxLevelScore = 0.95f;
                } else {
                    // If it's dark, that can be either indoor (sleeping) or outdoor
                    luxLevelScore = 0.5f;
                }
            }
        } else {
            // Indifferent, as we don't have sunset data to compare against
            luxLevelScore = 0.5f;
        }

        // Temperature
        // When starting up, temperature and humidity are 0. Only humidity will never reach 0 with real values.
        if (currentWeather != null && currentSensorReadings.getHumidity() != 0.0f) {
            if (currentSensorReadings.getTemperature() >= 17 && currentSensorReadings.getTemperature() <= 24
                    && (currentSensorReadings.getTemperature() - currentWeather.getTemperature() > 5)) {
                // Sensor reports temperature in range 17-23°, but outside temperature is not in this range -> indoor!
                temperatureScore = 1.0f;
            } else {
                temperatureScore = 0.5f;
            }
        } else {
            // Indifferent, as we don't have outside temperature data to compare against
            temperatureScore = 0.5f;
        }

        // Humidity
        if (currentWeather != null && currentSensorReadings.getHumidity() != 0.0f) {
            if (currentSensorReadings.getHumidity() - currentWeather.getHumidity() > 8) {
                // Humidity reported by sensor is more than 8% apart from humidity reported by weather API
                humidityScore = 1.0f;
            } else {
                // We can't be sure
                humidityScore = 0.5f;
            }
        } else {
            humidityScore = 0.5f;
        }

        // PM 2.5
        Log.i("Predictor", "Variance PM 2.5: " + varPM25);
        if (!Float.isNaN(varPM25) && varPM25 < 15) {
            pmScore = 0.8f;
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
        combinedScore += gpsSignalStrengthScore * WEIGHT_GPS_SIGNAL_STRENGTH;
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
        return (currentWeather.getSunrise() < now && now < currentWeather.getSunset());
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
                if (task.isSuccessful()) {

                    PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();
                    PlaceLikelihood place = likelyPlaces.iterator().next();
                    currentPlaceLikelihood = place.getLikelihood();
                    currentPlaceIDs = place.getPlace().getPlaceTypes().toString();

                    likelyPlaces.release();
                }
            }
        });
    }

    @Override
    public String toString() {
        String out = "";
        if (previousSensorReadings != null && currentWeather != null) {
            out = String.format(Locale.UK, "Current indoor likelihood: %.1f\nPrevious indoor likelihood: " +
                            "%.1f\nGPS signal strength: %.1f \n(max snr: %.1f, #>30: %d)\nGPS Accuracy: %.1f\nGPS Speed: " +
                            "%.1f (%.1f)\nGoogle places: " +
                            "%.1f\nLux level: %.1f (lux %d)\nTemperature: " +
                            "%.1f (s: %.1f, a: %.1f)\nHumidity: %.1f (s: %.1f, a: %.1f) \nPM 2.5: %.1f (var %.1f)",
                    getIndoorLikelihood(),
                    getPreviousIndoorLikelihood(),
                    getGpsSignalStrengthScore(), gpsMaxSignalNoiseRatio, gpsCountAbove30Snr,
                    getGpsAccuracyScore(), getGpsSpeedScore(), avgSpeed, getGpsGooglePlacesScore(), getLuxLevelScore(),
                    previousSensorReadings.getLux(), getTemperatureScore(), previousSensorReadings.getTemperature(),
                    currentWeather.getTemperature(), getHumidityScore(), previousSensorReadings.getHumidity(),
                    currentWeather.getHumidity(), getPmScore(), varPM25);
        } else if (previousSensorReadings != null) {
            out = String.format(Locale.UK, "Current indoor likelihood: %.1f\nPrevious indoor likelihood: " +
                            "%.1f\nGPS signal strength: %.1f \n(max snr: %.1f, #>30: %d)\nGPS Accuracy: %.1f\nGPS Speed: " +
                            "%.1f (%.1f)\nGoogle places: " +
                            "%.1f\nLux level: %.1f (lux %d)\nTemperature: " +
                            "%.1f (s: %.1f, a: %.1f)\nHumidity: %.1f (s: %.1f, a: %.1f) \nPM 2.5: %.1f (var %.1f)",
                    getIndoorLikelihood(),
                    getPreviousIndoorLikelihood(),
                    getGpsSignalStrengthScore(), gpsMaxSignalNoiseRatio, gpsCountAbove30Snr,
                    getGpsAccuracyScore(), getGpsSpeedScore(), avgSpeed, getGpsGooglePlacesScore(), getLuxLevelScore(),
                    previousSensorReadings.getLux(), getTemperatureScore(), previousSensorReadings.getTemperature(),
                    Float.NaN, getHumidityScore(), previousSensorReadings.getHumidity(),
                    Float.NaN, getPmScore(), varPM25);
        }
        return out;
    }

    public String toFileString() {
        if (previousSensorReadings != null && currentWeather != null) {
            return String.format(Locale.UK, "%.1f;%d;%.1f;%.1f;%.2f;%s;%d;%d;%d;%.1f;%.1f;%.1f;%.1f;%.1f;%.2f",
                    gpsMaxSignalNoiseRatio, gpsCountAbove30Snr, previousSensorReadings.getLocation().getAccuracy(),
                    avgSpeed, currentPlaceLikelihood, currentPlaceIDs, previousSensorReadings.getLux(),
                    currentWeather.getSunrise(), currentWeather.getSunset(), previousSensorReadings.getTemperature(),
                    currentWeather.getTemperature(), previousSensorReadings.getHumidity(),
                    currentWeather.getHumidity(), varPM25, getIndoorLikelihood());
        } else if (previousSensorReadings != null) {
            return String.format(Locale.UK, "%.1f;%d;%.1f;%.1f;%.2f;%s;%d;%s;%s;%.1f;%s;%.1f;%s;%.1f;%.2f",
                    gpsMaxSignalNoiseRatio, gpsCountAbove30Snr, previousSensorReadings.getLocation().getAccuracy(),
                    avgSpeed, currentPlaceLikelihood, currentPlaceIDs, previousSensorReadings.getLux(),
                    "", "", previousSensorReadings.getTemperature(),
                    "", previousSensorReadings.getHumidity(),
                    "", varPM25, getIndoorLikelihood());
        } else {
            return String.format(Locale.UK, "%.1f;%d;%s;%.1f;%.2f;%s;%s;%d;%d;%s;%.1f;%s;%.1f;%.1f;%.2f",
                    gpsMaxSignalNoiseRatio, gpsCountAbove30Snr, "",
                    avgSpeed, currentPlaceLikelihood, currentPlaceIDs, "",
                    currentWeather.getSunrise(), currentWeather.getSunset(), "",
                    currentWeather.getTemperature(), "",
                    currentWeather.getHumidity(), varPM25, getIndoorLikelihood());
        }
    }

    public float getPreviousIndoorLikelihood() {
        return previousIndoorLikelihood;
    }

    public float getGpsSignalStrengthScore() {
        return gpsSignalStrengthScore;
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
