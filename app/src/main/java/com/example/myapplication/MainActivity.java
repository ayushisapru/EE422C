package com.example.myapplication;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class MainActivity extends AppCompatActivity {

    private LinearLayout temperatureContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvWeatherForecast = findViewById(R.id.title);
        tvWeatherForecast.setText("Weather Forecast");

        TextView tvLocation = findViewById(R.id.location);
        tvLocation.setText("Austin, TX");

        // Cloud animation setup
        ImageView cloud2 = findViewById(R.id.cloud2);
        Animation cloud2Animation = AnimationUtils.loadAnimation(this, R.anim.clouds_move);
        cloud2.startAnimation(cloud2Animation);

        ImageView sunImageView = findViewById(R.id.sun); // Replace with your actual ImageView ID
        Animation rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.sun_rotate);
        sunImageView.startAnimation(rotateAnimation);

        temperatureContainer = findViewById(R.id.temperatureContainer);
        new FetchWeatherTask().execute("https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&hourly=temperature_2m,relative_humidity_2m,visibility&temperature_unit=fahrenheit&wind_speed_unit=kn&precipitation_unit=inch");


        LineGraphSeries<DataPoint> seriesTemperature = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
        });
        seriesTemperature.setTitle("Temperature");
        seriesTemperature.setColor(Color.RED);
        seriesTemperature.setDrawDataPoints(true);
        seriesTemperature.setDataPointsRadius(10);
        seriesTemperature.setThickness(8);



    }

    private class FetchWeatherTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            List<DataPoint> temperaturePoints = parseJsonForTemperature(response);

            LineGraphSeries<DataPoint> seriesTemp = new LineGraphSeries<>(
                    temperaturePoints.toArray(new DataPoint[0])
            );
            seriesTemp.setColor(Color.RED);
            seriesTemp.setTitle("Temperature");

            GraphView graph = findViewById(R.id.graph_temperature);
            graph.addSeries(seriesTemp);

            graph.getGridLabelRenderer().setVerticalAxisTitle("Temperature (°F)");
            graph.getGridLabelRenderer().setHorizontalAxisTitle("Hours (in 7 days)");

            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(24 * 7);
            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setScrollable(true);
            graph.getViewport().setScalable(true);

            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray times = jsonObject.getJSONObject("hourly").getJSONArray("time");
                JSONArray temperatures = jsonObject.getJSONObject("hourly").getJSONArray("temperature_2m");
                JSONArray humidities = jsonObject.getJSONObject("hourly").getJSONArray("relative_humidity_2m");
                JSONArray visibilities = jsonObject.getJSONObject("hourly").getJSONArray("visibility");

                List<String> nextSevenDays = getNextSevenDays();
                Map<String, Double[]> tempData = new LinkedHashMap<>();
                Map<String, Double[]> humidityData = new LinkedHashMap<>();
                Map<String, Double[]> visibilityData = new LinkedHashMap<>();

                for (String date : nextSevenDays) {
                    tempData.put(date, new Double[]{Double.MAX_VALUE, -Double.MAX_VALUE});
                    humidityData.put(date, new Double[]{Double.MAX_VALUE, -Double.MAX_VALUE});
                    visibilityData.put(date, new Double[]{Double.MAX_VALUE, -Double.MAX_VALUE});
                }

                for (int i = 0; i < times.length(); i++) {
                    String date = times.getString(i).substring(0, 10);
                    if (tempData.containsKey(date)) {
                        double temperature = temperatures.getDouble(i);
                        double humidity = humidities.getDouble(i);
                        double visibility = visibilities.getDouble(i);
                        Double[] minMaxTemps = tempData.get(date);
                        Double[] minMaxHumidity = humidityData.get(date);
                        Double[] minMaxVisibility = visibilityData.get(date);

                        minMaxTemps[0] = Math.min(minMaxTemps[0], temperature);
                        minMaxTemps[1] = Math.max(minMaxTemps[1], temperature);

                        minMaxHumidity[0] = Math.min(minMaxHumidity[0], humidity);
                        minMaxHumidity[1] = Math.max(minMaxHumidity[1], humidity);

                        minMaxVisibility[0] = Math.min(minMaxVisibility[0], visibility);
                        minMaxVisibility[1] = Math.max(minMaxVisibility[1], visibility);
                    }
                }

                for (String date : nextSevenDays) {
                    Double[] minMaxTemps = tempData.get(date);
                    Double[] minMaxHumidity = humidityData.get(date);
                    Double[] minMaxVisibility = visibilityData.get(date);
                    String displayText = String.format(Locale.US, "%s - Min Temp: %.2f°F, Max Temp: %.2f°F, Min Humidity: %.2f%%, Max Humidity: %.2f%%, Min Visibility: %.2f ft, Max Visibility: %.2f ft",
                            date, minMaxTemps[0], minMaxTemps[1], minMaxHumidity[0], minMaxHumidity[1], minMaxVisibility[0], minMaxVisibility[1]);

                    TextView weatherInfoView = new TextView(MainActivity.this);
                    weatherInfoView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    weatherInfoView.setText(displayText);
                    weatherInfoView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                    weatherInfoView.setTypeface(null, Typeface.BOLD);


                    temperatureContainer.addView(weatherInfoView);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        private List<DataPoint> parseJsonForTemperature(String response) {
            List<DataPoint> dataPoints = new ArrayList<>();
            try {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray times = jsonObject.getJSONObject("hourly").getJSONArray("time");
                JSONArray temperatures = jsonObject.getJSONObject("hourly").getJSONArray("temperature_2m");

                for (int i = 0; i < times.length(); i++) {
                    double temp = temperatures.getDouble(i);
                    dataPoints.add(new DataPoint(i, temp));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return dataPoints;
        }

        private void parseAndDisplayWeatherData(String response) {
            try {
                JSONObject jsonObject = new JSONObject(response);
                // Assume 'hourly' is the part of the JSON where your data is:
                JSONArray temperatures = jsonObject.getJSONObject("hourly").getJSONArray("temperature_2m");

                for (int i = 0; i < temperatures.length(); i++) {
                    // Just an example, make sure to parse according to your JSON structure
                    double temp = temperatures.getDouble(i);

                    TextView tempView = new TextView(MainActivity.this);
                    tempView.setText(String.format(Locale.US, "Hour %d: Temperature: %.2f°F", i, temp));
                    tempView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                    tempView.setTypeface(null, Typeface.BOLD);

                    temperatureContainer.addView(tempView);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        private List<String> getNextSevenDays() {
            List<String> dates = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();

            for (int i = 0; i < 7; i++) {
                dates.add(sdf.format(calendar.getTime()));
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            return dates;
        }
    }
}
