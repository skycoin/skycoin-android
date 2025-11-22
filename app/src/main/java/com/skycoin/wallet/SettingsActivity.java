package com.skycoin.wallet;

import android.os.Bundle;
import androidx.fragment.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skycoin.wallet.db.DatabaseHelper;
import com.skycoin.wallet.home.PinDialogFragment;
import com.skycoin.wallet.home.TransactionsFragment;
import com.skycoin.wallet.nodebackend.NodeHealthRes;
import com.skycoin.wallet.nodebackend.NodeUtils;
import com.skycoin.wallet.nodebackend.SkycoinService;
import com.skycoin.wallet.onboarding.PinFragment;
import com.skycoin.wallet.preferences.PreferenceStore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static java.net.HttpURLConnection.HTTP_OK;

public class SettingsActivity extends BaseActivity {

    private static final String TAG = SettingsActivity.class.getName();

    private TextView mSupportedCoin;
    private TextView mNodeVersion;
    private TextView mCHBurn;

    private LinearLayout mLoadingContainer;
    private LinearLayout mInfoContainer;

    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        setContentView(R.layout.settings_activity);

        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        final EditText txt = findViewById(R.id.url_text);
        txt.setText(Utils.getSkycoinUrl(this));

        Button restore = findViewById(R.id.restore_button);
        restore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txt.setText(SkycoinService.BASE_URL);
            }
        });

        Button query = findViewById(R.id.query_button);
        query.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = txt.getText().toString().trim();
                if (!TextUtils.isEmpty(url) && !url.endsWith("/")) {
                    url += "/";
                }
                try {
                    queryNodeStatus(url);
                } catch (Exception ex) {
                    Log.d(TAG,"could not query node",ex);
                    showInfoPopup(getResources().getString(R.string.error),
                            getResources().getString(R.string.error_network),
                            getResources().getString(R.string.ok), null);
                }
            }
        });

        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = txt.getText().toString();
                if (!TextUtils.isEmpty(url) && !url.endsWith("/")) {
                    url += "/";
                }
                PreferenceStore.setUrl(SettingsActivity.this, url);
                NodeUtils.pingNodeSilently(SettingsActivity.this, Utils.getSkycoinUrl(SettingsActivity.this));
                finish();
            }
        });

        Button pinButton = findViewById(R.id.pin_button);
        pinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPin(new PinDialogFragment.PinCallback() {
                    @Override
                    public void onCallback(boolean succeeded, String res, int code) {
                        if (succeeded) {
                            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                            PinFragment pf = PinFragment.newInstance();
                            ft.setCustomAnimations(R.anim.slide_in_from_bottom, 0,
                                    0, R.anim.slide_out_to_bottom);
                            ft.replace(R.id.main_content, pf, TransactionsFragment.getFragmentTag());
                            ft.addToBackStack(null);
                            ft.commit();
                        }
                    }
                }, true, getResources().getString(R.string.pin_request_pin));

            }
        });


        TextView tv = findViewById(R.id.versionName);
        tv.setText("Version: "+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+") (" + (BuildConfig.BUILD_TYPE)+")");

        tv = findViewById(R.id.backendVersion);
        tv.setText("Backend node min. version: "+BuildConfig.MinimumBackendVersion);

        tv = findViewById(R.id.dbVersion);
        tv.setText("DB version: " + DatabaseHelper.DB_VERSION);

        mSupportedCoin = findViewById(R.id.coin_name);
        mNodeVersion = findViewById(R.id.node_version);
        mCHBurn = findViewById(R.id.ch_burn);

        mLoadingContainer = findViewById(R.id.node_loading_container);
        mInfoContainer = findViewById(R.id.node_info_container);

        queryNodeStatus(Utils.getSkycoinUrl(this));
    }

    public void queryNodeStatus(String url) {

        mLoadingContainer.setVisibility(View.VISIBLE);
        mInfoContainer.setVisibility(View.INVISIBLE);

        mSupportedCoin.setText("Coin: -");
        mNodeVersion.setText("Node Version: -");
        mCHBurn.setText("Coin Hour burn: -");

        Retrofit retrofit = NodeUtils.getRetrofit(url);
        if (retrofit == null) {
            hideLoadingPopup();
            showInfoPopup(getResources().getString(R.string.error),
                    getResources().getString(R.string.error_retrofit),
                    getResources().getString(R.string.ok), null);
            return;
        }

        final SkycoinService api = retrofit.create(SkycoinService.class);

        api.getNodeHealth().enqueue(new Callback<NodeHealthRes>() {
            @Override
            public void onResponse(Call<NodeHealthRes> call, Response<NodeHealthRes> response) {
                if (isDestroyed()) {
                    return;
                }

                mLoadingContainer.setVisibility(View.INVISIBLE);
                mInfoContainer.setVisibility(View.VISIBLE);

                if (response.code() != HTTP_OK || response.body() == null) {
                    Log.d(TAG, "failed to load from backed:" + response.code());
                    showInfoPopup(getResources().getString(R.string.error),
                            getResources().getString(R.string.error_network),
                            getResources().getString(R.string.ok), null);
                } else {
                    Log.d(TAG, "got node health: " + response.body().coinName + " uptime " + response.body().uptime);
                    if (response.body().coinName != null) {
                        mSupportedCoin.setText("Coin: " + response.body().coinName);
                    }
                    if (response.body().versionInfo != null) {
                        mNodeVersion.setText("Node Version: " + response.body().versionInfo.getVersion());
                    }
                    if (response.body().userVerifyTxRules != null) {
                        int burn = response.body().userVerifyTxRules.burnFactor;
                        mCHBurn.setText("Coin Hour burn: " + ((int) ((1.0f / burn) * 100)) + "%");
                    }

                }

            }

            @Override
            public void onFailure(Call<NodeHealthRes> call, Throwable t) {
                if (isDestroyed()) {
                    return;
                }

                mLoadingContainer.setVisibility(View.INVISIBLE);
                mInfoContainer.setVisibility(View.VISIBLE);

                Log.e(TAG, "error getting version", t);
                showInfoPopup(getResources().getString(R.string.error),
                        getResources().getString(R.string.error_network),
                        getResources().getString(R.string.ok), null);
                hideLoadingPopup();
            }
        });

    }


    public boolean shouldRequirePin() {
        return false;
    }

}
