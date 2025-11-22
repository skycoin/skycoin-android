package com.skycoin.wallet;

import android.content.Context;
import android.text.TextUtils;

import com.skycoin.wallet.nodebackend.SkycoinService;
import com.skycoin.wallet.preferences.PreferenceStore;

public class Utils {

    public static String getSkycoinUrl(Context ctx) {
        String url = PreferenceStore.getUrl(ctx);
        if (TextUtils.isEmpty(url)) {
            return SkycoinService.BASE_URL;
        }
        return url;
    }

}
