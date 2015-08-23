package com.morristaedt.mirror.modules;

import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import com.morristaedt.mirror.R;
import com.morristaedt.mirror.requests.ForecastRequest;
import com.morristaedt.mirror.requests.ForecastResponse;

import java.util.Calendar;
import java.util.List;

import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

/**
 * Created by HannahMitt on 8/22/15.
 */
public class ForecastModule {

    public interface ForecastListener {
        void onWeatherToday(String weatherToday);

        void onShouldBike(boolean showToday, boolean shouldBike);
    }

    public static void getHourlyForecast(final Resources resources, final double lat, final double lon, final ForecastListener listener) {
        new AsyncTask<Void, Void, ForecastResponse>() {

            @Override
            protected ForecastResponse doInBackground(Void... params) {
                RestAdapter restAdapter = new RestAdapter.Builder()
                        .setEndpoint("https://api.forecast.io")
                        .setErrorHandler(new ErrorHandler() {
                            @Override
                            public Throwable handleError(RetrofitError cause) {
                                Log.w("mirror", "Forecast error: " + cause);
                                return null;
                            }
                        })
                        .build();

                ForecastRequest service = restAdapter.create(ForecastRequest.class);
                String excludes = "minutely,daily,flags";
                String units = "si";
                Log.d("mirror", "backgrounddd");
                return service.getHourlyForecast(resources.getString(R.string.dark_sky_api_key), lat, lon, excludes, units);
            }

            @Override
            protected void onPostExecute(ForecastResponse forecastResponse) {
                if (forecastResponse != null) {
                    if (forecastResponse.currently != null) {
                        listener.onWeatherToday(forecastResponse.currently.getDisplayTemperature() + " " + forecastResponse.currently.summary);
                    }

                    if (isWeekday() && forecastResponse.hourly != null && forecastResponse.hourly.data != null) {
                        listener.onShouldBike(true, shouldBikeToday(forecastResponse.hourly.data));
                    } else {
                        listener.onShouldBike(false, true);
                    }
                }
            }

            private boolean isWeekday() {
                int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
                return dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY;
            }

            private boolean shouldBikeToday(List<ForecastResponse.Hour> hours) {
                int dayOfMonthToday = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

                for (ForecastResponse.Hour hour : hours) {
                    Calendar hourCalendar = hour.getCalendar();

                    // Only check hourly forecast for today
                    if (hourCalendar.get(Calendar.DAY_OF_MONTH) == dayOfMonthToday) {
                        int hourOfDay = hourCalendar.get(Calendar.HOUR_OF_DAY);
                        Log.i("mirror", "Hour of day is " + hourOfDay + " with precipProb " + hour.precipProbability);
                        if (hourOfDay >= 7 && hourOfDay <= 11) {
                            if (hour.precipProbability >= 0.3) {
                                return false;
                            }
                        } else if (hourOfDay >= 17 && hourOfDay <= 19) {
                            if (hour.precipProbability >= 0.3) {
                                return false;
                            }
                        }
                    }
                }

                return true;
            }
        }.execute();

    }
}
