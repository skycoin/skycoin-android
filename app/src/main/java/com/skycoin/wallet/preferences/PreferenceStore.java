package com.skycoin.wallet.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import android.util.Log;

import com.skycoin.wallet.nodebackend.SkycoinService;

/**
 * Simple utility class for storing and retreiving user prefs and
 * other small values
 */
public class PreferenceStore
{

    private static final String TAG = PreferenceStore.class.getName();

    private static final String FILE_NAME = "skycoin.wallet.prefs";

    // begin various keys
    private static final String KEY_SHOWN_ONBOARDING = "skycoin.prefs.show_onboarding";
    private static final String KEY_PIN = "skycoin.prefs.pin";
    private static final String KEY_PIN_TYPE = "skycoin.prefs.pin_type";
    private static final String KEY_FIRST_WALLET = "skycoin.prefs.first_wallet";
    private static final String KEY_WALLET_KEY_LIST = "skycoin.wallet.all_wallets";
    private static final String KEY_USD_PRICE = "skycoin.prefs.usd_price";
    private static final String KEY_BACKEND_URL = "skycoin.prefs.backend_url";
    private static final String KEY_BACKEND_COIN_NAME = "skycoin.prefs.backend_coin_name";
    private static final String KEY_BACKEND_MAX_DECIMALS = "skycoin.prefs.backend_max_decimals";
    private static final String KEY_BACKEND_BURN_FACTOR = "skycoin.prefs.backend_burn_factor";

    public static final int PIN_TYPE_4 = 0;
    public static final int PIN_TYPE_6 = 1;

    private static final String DEFAULT_COIN_NAME = "Skycoin";
    private static final int DEFAULT_MAX_DECIMALS = 3;
    private static final int DEFAULT_BURN_FACTOR = 2; // divisor, so burn will be NumHours/2 (round the burn up to nearest integer)

    // end keys

    private static SharedPreferences getPrefs(final @NonNull Context ctx) {
        return ctx.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }

    public static boolean hasShownOnboarding(final @NonNull Context ctx) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        return pref.getBoolean(KEY_SHOWN_ONBOARDING, false);
    }
    public static void setHasShownOnboarding(final @NonNull Context ctx, boolean b) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        pref.edit().putBoolean(KEY_SHOWN_ONBOARDING, b).apply();
    }

    public static boolean hasDoneFirstWallet(final @NonNull Context ctx) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        return pref.getBoolean(KEY_FIRST_WALLET, false);
    }
    public static void setHasDoneFirstWallet(final @NonNull Context ctx, boolean b) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        pref.edit().putBoolean(KEY_FIRST_WALLET, b).apply();
    }

    public static void setPinString(final @NonNull Context ctx, String pin) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        pref.edit().putString(KEY_PIN, pin).apply();
    }
    public static String getPin(final @NonNull Context ctx) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        return pref.getString(KEY_PIN, null);
    }

    public static void setPinType(final @NonNull Context ctx, int pintype) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        pref.edit().putInt(KEY_PIN_TYPE, pintype).apply();
    }
    public static int getPinType(final @NonNull Context ctx) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        return pref.getInt(KEY_PIN_TYPE, PIN_TYPE_4);
    }

    public static void setUrl(final @NonNull Context ctx, String url) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        pref.edit().putString(KEY_BACKEND_URL, url).apply();
    }
    public static String getUrl(final @NonNull Context ctx) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        return pref.getString(KEY_BACKEND_URL, null);
    }

    public static void setUsdPrice(final @NonNull Context ctx, float price) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        pref.edit().putFloat(KEY_USD_PRICE, price).apply();
    }
    public static float getUsdPrice(final @NonNull Context ctx) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        return pref.getFloat(KEY_USD_PRICE,0);
    }

    public static void setCoinName(final @NonNull Context ctx, String name) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        pref.edit().putString(KEY_BACKEND_COIN_NAME, name).apply();
    }
    public static String getCoinName(final @NonNull Context ctx) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        return pref.getString(KEY_BACKEND_COIN_NAME, DEFAULT_COIN_NAME);
    }

    public static void setMaxDecimals(final @NonNull Context ctx, int max) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        pref.edit().putInt(KEY_BACKEND_MAX_DECIMALS, max).apply();
    }
    public static int getMaxDecimals(final @NonNull Context ctx) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        return pref.getInt(KEY_BACKEND_MAX_DECIMALS, DEFAULT_MAX_DECIMALS);
    }

    public static void setBurnFactor(final @NonNull Context ctx, int burn) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        pref.edit().putInt(KEY_BACKEND_BURN_FACTOR, burn).apply();
    }
    public static int getBurnFactor(final @NonNull Context ctx) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        return pref.getInt(KEY_BACKEND_BURN_FACTOR, DEFAULT_BURN_FACTOR);
    }

    /**
     * Sets the comma separated list of wallet keys
     *
     * @param ctx calling context
     * @param wallets comma separated list of preference keys
     */
    public static void setWalletKeyList(final @NonNull Context ctx, String wallets) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        pref.edit().putString(KEY_WALLET_KEY_LIST, wallets).apply();
    }

    /**
     * Gets the list of wallet pref keys
     *
     * @param ctx
     * @return comma separated list of pref keys. Each key stores wallet data that can be retrieved from the preferences
     */
    public static String getWalletKeyList(final @NonNull Context ctx) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        return pref.getString(KEY_WALLET_KEY_LIST, null);
    }

    public static boolean deleteWalletData(@NonNull Context ctx, @NonNull String walletIdKey) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        boolean res = pref.edit().remove(walletIdKey).commit();
        Log.d(TAG,"deleted data for wallet "+walletIdKey+", success:"+res);
        return res;
    }

    public static void storeWalletData(@NonNull Context ctx, @NonNull String base64Data, @NonNull String key) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        pref.edit().putString(key, base64Data).commit();
    }
    public static String getWalletData(@NonNull Context ctx, @NonNull String walletKey) {
        SharedPreferences pref = PreferenceStore.getPrefs(ctx);
        return pref.getString(walletKey, null);
    }

}
