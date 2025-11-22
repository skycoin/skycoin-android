package com.skycoin.wallet.wallet;

import android.content.Context;

import com.skycoin.wallet.R;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WalletUtils {

    // default localized date formatting
    private static DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
    private static DateFormat tf = DateFormat.getTimeInstance();
    private static NumberFormat numF = NumberFormat.getInstance();

    public static String formatCoinsToSuffix(double droplets, boolean useCompact) {
        String res;
        double bal = droplets / 1000000.0f;
        if (useCompact) {
            if (bal <= 1000) {
                res = String.format("%.3f", bal);
            } else if (bal < 1000 * 1000) {
                res = String.format("%.3fK", (bal / 1000.0));
            } else if (bal < 1000 * 1000 * 1000) {
                res = String.format("%.3fM", (bal / 1000000.0));
            } else {
                res = String.format("%.3fB", (bal / 1000000000.0));
            }
        } else {

            res = String.format("%.3f", bal);
        }

        return res;
    }

    public static String formatHoursToSuffix(long hours, boolean useCompact) {
        String res;
        if (useCompact) {
            if (hours < 1000) {
                res = "" + hours;
            } else if (hours < 1000 * 1000) {
                res = String.format("%.1fK", (hours / 1000.0));
            } else if (hours < 1000 * 1000 * 1000) {
                res = String.format("%.1fM", (hours / 1000000.0));
            } else {
                res = String.format("%.1fB", (hours / 1000000000.0));
            }
        } else {
            res = numF.format(hours);// String.format("%.0f", (hours / 1.0));
        }

        return res;
    }

    public static String formatUnixDateToReadable(Context ctx, long timestamp) {

        long diff = System.currentTimeMillis() - timestamp;

        // in the future
        /*if (diff < 0) {
            return ctx.getResources().getString(R.string.time_nice_later);
        }*/

        if (diff < TimeUnit.MINUTES.toMillis(2)) {
            return ctx.getResources().getString(R.string.time_nice_justnow);
        }

        if (diff < TimeUnit.HOURS.toMillis(24)) {
            return ctx.getResources().getString(R.string.time_nice_today);
        }

        if (diff < TimeUnit.DAYS.toMillis(2)) {
            return ctx.getResources().getString(R.string.time_nice_yesterday);
        }

        return df.format(new Date(timestamp));
    }

    public static String formatUnixDateToClockTime(Context ctx, long timestamp) {
        return tf.format(new Date(timestamp));
    }

    public static Wallet findWalletForAddress(List<Wallet> allWallets, String address) {
        for(Wallet w : allWallets) {
            for(Address a : w.getAddresses() ) {
                if (a.getAddress().equals(address)) {
                    return w;
                }
            }
        }

        return null;
    }


}
