package com.freshcheck.app.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class DateUtils {
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private DateUtils() {}

    public static SimpleDateFormat getDateFormat() {
        return new SimpleDateFormat(DATE_FORMAT, Locale.US);
    }

    public static String getTodayString() {
        return getDateFormat().format(new Date());
    }

    public static String getFutureDateString(int daysFromNow) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, daysFromNow);
        return getDateFormat().format(calendar.getTime());
    }

    public static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return new Date();
        }
        try {
            return getDateFormat().parse(dateStr);
        } catch (ParseException e) {
            return new Date();
        }
    }

    public static String formatDate(Date date) {
        if (date == null) return getTodayString();
        return getDateFormat().format(date);
    }

    /**
     * Calculates days remaining until the given expiry date.
     * Compares date midnight to date midnight to ensure accurate day counts.
     */
    public static long getDaysUntil(String expiryDateStr) {
        if (expiryDateStr == null || expiryDateStr.trim().isEmpty()) {
            return 0;
        }

        try {
            SimpleDateFormat sdf = getDateFormat();
            Date expiryDate = sdf.parse(expiryDateStr);

            Calendar todayCal = Calendar.getInstance();
            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0);

            Calendar expiryCal = Calendar.getInstance();
            expiryCal.setTime(expiryDate);
            expiryCal.set(Calendar.HOUR_OF_DAY, 0);
            expiryCal.set(Calendar.MINUTE, 0);
            expiryCal.set(Calendar.SECOND, 0);
            expiryCal.set(Calendar.MILLISECOND, 0);

            long diffMillis = expiryCal.getTimeInMillis() - todayCal.getTimeInMillis();
            return TimeUnit.MILLISECONDS.toDays(diffMillis);
        } catch (ParseException e) {
            return 0;
        }
    }

    public static String getRelativeExpiryText(String expiryDateStr) {
        long days = getDaysUntil(expiryDateStr);
        if (days < 0) {
            long absDays = Math.abs(days);
            return absDays == 1 ? "Expired yesterday" : "Expired " + absDays + " days ago";
        } else if (days == 0) {
            return "Expires today";
        } else if (days == 1) {
            return "Expires tomorrow";
        } else {
            return "Expires in " + days + " days";
        }
    }
}
