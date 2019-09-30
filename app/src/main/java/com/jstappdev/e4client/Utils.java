package com.jstappdev.e4client;

import android.content.Context;
import android.os.AsyncTask;
import android.text.format.DateFormat;
import android.widget.Toast;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {

    public static String getDate(final long time) {
        final Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(time * 1000);
        return DateFormat.format("dd. MMM. yyyy - HH:mm", cal).toString();
    }

    public static String getDuration(final long time) {
        final Calendar cal = Calendar.getInstance(Locale.getDefault());
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(time * 1000);
        return DateFormat.format("HH:mm:ss", cal).toString();
    }


    public static class DeleteSession extends AsyncTask<String, Void, Boolean> {

        private Context context;
        private OkHttpClient okHttpClient;

        DeleteSession(final Context context, final OkHttpClient okHttpClient) {
            this.context = context;
            this.okHttpClient = okHttpClient;
        }

        @Override
        protected Boolean doInBackground(String... ids) {
            final String sessionId = ids[0];
            final String url = "https://www.empatica.com/connect/connect.php/sessions/" + sessionId;

            final Request request = new Request.Builder().url(url).delete().build();

            try {
                return okHttpClient.newCall(request).execute().isSuccessful();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            final String s = success ? "Deleted session." : "FAILED to delete session.";
            Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
        }

    }

}
