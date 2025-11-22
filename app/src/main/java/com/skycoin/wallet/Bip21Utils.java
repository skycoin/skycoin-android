package com.skycoin.wallet;

/*

 example url with all options:

 skycoin:2hYbwYudg34AjkJJCRVRcMeqSWHUixjkfwY?amount=123.456&hours=70&label=friend&message=Birthday%20Gift

*/

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.io.Serializable;

public class Bip21Utils {

    private static final String TAG = Bip21Utils.class.getName();

    public static final String BIP21_SCHEME_ID = "skycoin";
    public static final String BIP21_SCHEME = "scheme";
    public static final String BIP21_ADDRESS = "address";
    public static final String BIP21_AMOUNT = "amount";
    public static final String BIP21_HOURS = "hours";
    public static final String BIP21_LABEL = "label";
    public static final String BIP21_MESSAGE = "message";

    private static final String SKYCOIN_CORRECT = BIP21_SCHEME_ID + ":";
    private static final String SKYCOIN_URI_PARSE_HACK = BIP21_SCHEME_ID + "://";

    public static class Bip21Data implements Serializable {
        public String scheme;
        public String address;
        public String amount;
        public String hours;
        public String message;
        public String label;
    }

    public static boolean isPossibleUri(@NonNull String uriStr) {
        return uriStr.contains(":"); // scheme always ends with ":" so if no ":" no uri
    }

    @Nullable
    public static Bip21Data parseSkycoinBip21Url(@NonNull String urlStr) {
        if (urlStr.startsWith(SKYCOIN_CORRECT) && !urlStr.startsWith(SKYCOIN_URI_PARSE_HACK)) {
            urlStr = urlStr.replace(SKYCOIN_CORRECT,SKYCOIN_URI_PARSE_HACK);
        }

        Uri uri = Uri.parse(urlStr);

        if (uri == null || uri.getScheme() == null || !uri.getScheme().equalsIgnoreCase(BIP21_SCHEME_ID)) {
            return null;
        }

        Bip21Data data = new Bip21Data();

        data.scheme = uri.getScheme();
        data.address = uri.getHost();
        data.amount = uri.getQueryParameter(BIP21_AMOUNT);
        data.hours = uri.getQueryParameter(BIP21_HOURS);
        data.message = uri.getQueryParameter(BIP21_MESSAGE);
        data.label = uri.getQueryParameter(BIP21_LABEL);

        return data;
    }

    @Nullable
    public static String buildSkycoinBip21Url(@NonNull String address, String amount,
                                              String hours, String label, String message) {
        if (TextUtils.isEmpty(address)) {
            return null;
        }

        Uri.Builder b = new Uri.Builder();
        b.scheme(BIP21_SCHEME_ID);

        b.authority(address);

        if (!TextUtils.isEmpty(amount)) {
            b.appendQueryParameter(BIP21_AMOUNT, amount);
        }
        if (!TextUtils.isEmpty(hours)) {
            b.appendQueryParameter(BIP21_HOURS, hours);
        }
        if (!TextUtils.isEmpty(label)) {
            b.appendQueryParameter(BIP21_LABEL, label);
        }
        if (!TextUtils.isEmpty(message)) {
            b.appendQueryParameter(BIP21_MESSAGE, message);
        }

        Uri uri = b.build();
        String done = uri.toString();
        done = done.replace("://",":");

        Log.d(TAG,"build bip21: "+done);
        return done;
    }


}
