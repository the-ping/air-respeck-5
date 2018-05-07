package com.specknet.airrespeck.remote;

import java.util.Locale;

public class OpenWeatherData {
    private Sun sys;
    private TempAndHumidity main;
    private Clouds clouds;

    /*
    {"coord":{"lon":139,"lat":35},
        "sys":{"country":"JP","sunrise":1369769524,"sunset":1369821049},
        "weather":[{"id":804,"main":"clouds","description":"overcast clouds","icon":"04n"}],
        "main":{"temp":289.5,"humidity":89,"pressure":1013,"temp_min":287.04,"temp_max":292.04},
        "wind":{"speed":7.31,"deg":187.002},
        "rain":{"3h":0},
        "clouds":{"all":92},
        "dt":1369824698,
            "id":1851632,
            "name":"Shuzenji",
            "cod":200}
      */

    public OpenWeatherData(Sun sys, TempAndHumidity main, Clouds clouds) {
        this.sys = sys;
        this.main = main;
        this.clouds = clouds;
    }

    public Sun getSun() {
        return sys;
    }

    public double getTemperature() {
        return main.getTemp();
    }

    public double getHumidity() {
        return main.getHumidity();
    }

    public Clouds getClouds() {
        return clouds;
    }

    public class Sun {
        String country;
        Long sunrise;
        Long sunset;

        public Sun(String country, Long sunrise, Long sunset) {
            this.country = country;
            this.sunrise = sunrise;
            this.sunset = sunset;
        }

        public String getCountry() {
            return country;
        }

        public Long getSunrise() {
            return sunrise;
        }

        public Long getSunset() {
            return sunset;
        }

        @Override
        public String toString() {
            return String.format(Locale.UK, "Country: %s, Sunrise: %d, Sunset: %d", country, sunrise, sunset);
        }
    }

    public class TempAndHumidity {
        Double temp;
        Double humidity;
        Double pressure;
        Double temp_min;
        Double temp_max;

        public TempAndHumidity(Double temp, Double humidity, Double pressure, Double temp_min, Double temp_max) {
            this.temp = temp;
            this.humidity = humidity;
            this.pressure = pressure;
            this.temp_min = temp_min;
            this.temp_max = temp_max;
        }

        public Double getTemp() {
            return temp;
        }

        public Double getHumidity() {
            return humidity;
        }

        public Double getPressure() {
            return pressure;
        }

        public Double getTemp_min() {
            return temp_min;
        }

        public Double getTemp_max() {
            return temp_max;
        }

        @Override
        public String toString() {
            return String.format(Locale.UK, "Temperature: %.1f, Humidity: %.1f", temp, humidity);
        }
    }

    public class Clouds {
        Double all;

        public Clouds(Double all) {
            this.all = all;
        }

        public Double getAll() {
            return all;
        }
    }
}
