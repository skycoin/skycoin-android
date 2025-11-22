package com.skycoin.wallet.nodebackend;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.Log;

import com.skycoin.wallet.R;
import com.skycoin.wallet.preferences.PreferenceStore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static java.net.HttpURLConnection.HTTP_OK;

public class NodeUtils {

    public static final String VERSION_24 = "0.24";
    public static final int DEFAULT_BURN = 2;
    public static final int DEFAULT_MAX_DEC = 3;

    private static final String TAG = NodeUtils.class.getName();

    public interface NodeRulesCallback {
        public void onNodeRules(boolean success, @Nullable Throwable error, int burnFactor, int maxDecimals);
    }

    @Nullable
    public static Retrofit getRetrofit(final String url) {
        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            return retrofit;
        } catch (Exception ex) {
            Log.e(TAG,"could not get retrofit instance",ex);
        }
        return null;
    }


    // try to update node conf
    public static void pingNodeSilently(final Context context, final String url) {
        Retrofit retrofit = getRetrofit(url);
        if (retrofit == null) {
            return; // silent fail
        }

        final SkycoinService api = retrofit.create(SkycoinService.class);

        api.getNodeHealth().enqueue(new Callback<NodeHealthRes>() {
            @Override
            public void onResponse(Call<NodeHealthRes> call, Response<NodeHealthRes> response) {

                if (response.code() != HTTP_OK || response.body() == null) {
                    Log.d(TAG, "failed to load from backed:" + response.code());
                    // keep last known values
                } else {
                    Log.d(TAG, "pinged node health coin: " + response.body().coinName);
                    if (response.body().coinName != null) {
                        PreferenceStore.setCoinName(context, response.body().coinName);
                    }
                    if (response.body().userVerifyTxRules != null) {
                        int burn = response.body().userVerifyTxRules.burnFactor;
                        Log.d(TAG, "pinged node health burn: " + burn);
                        if (burn > 1) {
                            PreferenceStore.setBurnFactor(context, burn);
                        }
                        int maxDecimals = response.body().userVerifyTxRules.maxDecimals;
                        Log.d(TAG, "pinged node health maxdecimals: " + maxDecimals);
                        PreferenceStore.setMaxDecimals(context, maxDecimals);
                    }

                }

            }

            @Override
            public void onFailure(Call<NodeHealthRes> call, Throwable t) {
                Log.e(TAG, "error getting node health", t);
                // keep last known values
            }
        });

    }

    // read node conf, update local stored rules, return values
    public static void getNodeRules(final Context context, final String url, final NodeRulesCallback callback) {
        Retrofit retrofit = NodeUtils.getRetrofit(url);
        if (retrofit == null) {
            callback.onNodeRules(false, new Exception(context.getResources().getString(R.string.error_retrofit)), 0,0);
            return;
        }

        final SkycoinService api = retrofit.create(SkycoinService.class);

        api.getNodeHealth().enqueue(new Callback<NodeHealthRes>() {
            @Override
            public void onResponse(Call<NodeHealthRes> call, Response<NodeHealthRes> response) {

                if (response.code() != HTTP_OK || response.body() == null) {
                    Log.d(TAG, "failed to load from backed:" + response.code());
                } else {
                    Log.d(TAG, "pinged node health coin: " + response.body().coinName);
                    if (response.body().coinName != null) {
                        PreferenceStore.setCoinName(context, response.body().coinName);
                    }
                    if (response.body().userVerifyTxRules != null) {
                        int burn = response.body().userVerifyTxRules.burnFactor;
                        Log.d(TAG, "pinged node health burn: " + burn);
                        if (burn > 1) {
                            PreferenceStore.setBurnFactor(context, burn);
                        }
                        int maxDecimals = response.body().userVerifyTxRules.maxDecimals;
                        Log.d(TAG, "pinged node health maxdecimals: " + maxDecimals);
                        PreferenceStore.setMaxDecimals(context, maxDecimals);
                        callback.onNodeRules(true, null, burn,maxDecimals);
                        return;
                    } else {
                        // 0.24 does not return any userVerifyTxRules so if all else is fine
                        // but we dont get any we assume 0.24 and default to burn=2 and maxdec = 3
                        if (response.body().versionInfo == null ||
                                response.body().versionInfo.getVersion() == null ||
                                response.body().versionInfo.getVersion().startsWith(NodeUtils.VERSION_24)) {
                            Log.d(TAG,"no info but node is v0.24 so assume defaults");
                            callback.onNodeRules(true, null, DEFAULT_BURN,DEFAULT_MAX_DEC);
                            return;
                        }

                    }
                }
                callback.onNodeRules(false, null, 0,0);
            }

            @Override
            public void onFailure(Call<NodeHealthRes> call, Throwable t) {
                Log.e(TAG, "error getting node health", t);
                callback.onNodeRules(false, t, 0,0);
            }
        });

    }

}
