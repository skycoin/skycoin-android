package com.skycoin.wallet.home;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.skycoin.wallet.BaseActivity;
import com.skycoin.wallet.Bip21Utils;
import com.skycoin.wallet.R;
import com.skycoin.wallet.Utils;
import com.skycoin.wallet.db.DatabaseHelper;
import com.skycoin.wallet.nodebackend.BalanceRes;
import com.skycoin.wallet.nodebackend.CMCApi;
import com.skycoin.wallet.nodebackend.CMCRes;
import com.skycoin.wallet.nodebackend.CPApi;
import com.skycoin.wallet.nodebackend.CPRes;
import com.skycoin.wallet.nodebackend.NodeUtils;
import com.skycoin.wallet.nodebackend.SkycoinService;
import com.skycoin.wallet.preferences.PreferenceStore;
import com.skycoin.wallet.send.SendDialogFragment;
import com.skycoin.wallet.wallet.Address;
import com.skycoin.wallet.wallet.Wallet;
import com.skycoin.wallet.wallet.WalletManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static java.net.HttpURLConnection.HTTP_OK;

public class HomeActivity extends BaseActivity {

    private static final String TAG = HomeActivity.class.getName();

    public static final String ARG_SEND_REQUEST = "com.skycoin.arg.do_send_request";

    protected boolean mSupressPin = false;

    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.home_activity);

        WalletsContainerFragment fr = WalletsContainerFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.home_activity_fragment, fr, WalletsContainerFragment.getFragmentTag());
        transaction.commit();

        ImageView send = findViewById(R.id.send_button);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"clicked SEND button");
                DialogFragment sendFragment = SendDialogFragment.newInstance(null);
                sendFragment.show(getSupportFragmentManager(), null);

            }
        });
        final ImageView wallet = findViewById(R.id.wallets_button);
        final ImageView tx = findViewById(R.id.transactions_button);
        wallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"clicked WALLETS button");
                WalletsContainerFragment fr = WalletsContainerFragment.newInstance();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.slide_in_from_left, R.anim.slide_out_to_right,
                        R.anim.slide_in_from_right, R.anim.slide_out_to_left);
                transaction.replace(R.id.home_activity_fragment, fr, WalletsContainerFragment.getFragmentTag());
                transaction.commit();
                wallet.setEnabled(false);
                tx.setEnabled(true);
            }
        });

        tx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"clicked TRANSACTIONS button");
                // show transactions for all wallets
                Fragment listFragment = TransactionsFragment.newInstance();
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.slide_in_from_right, R.anim.slide_out_to_left,
                        R.anim.slide_in_from_left, R.anim.slide_out_to_right);
                // in case we are viewing a childfrgament/wallet-detail we first pop that one so we are back to wallet list
                getSupportFragmentManager().popBackStack();
                transaction.replace(R.id.home_activity_fragment, listFragment, TransactionsFragment.getFragmentTag());
                transaction.commit();
                wallet.setEnabled(true);
                tx.setEnabled(false);
            }
        });
        wallet.setEnabled(false);
        tx.setEnabled(true);
    }

    public void onNewIntent(Intent in) {
        setIntent(in);
    }

    public void onPinSuccess() {
        Intent in = getIntent();
        if (in != null) {
            Bip21Utils.Bip21Data req = (Bip21Utils.Bip21Data) in.getSerializableExtra(ARG_SEND_REQUEST);
            in.removeExtra(ARG_SEND_REQUEST);
            if (req != null && !TextUtils.isEmpty(req.scheme) && req.scheme.equalsIgnoreCase(Bip21Utils.BIP21_SCHEME_ID)) {
                handleSendRequest(req);
            }
        }
    }

    private void handleSendRequest(Bip21Utils.Bip21Data request) {
        Log.d(TAG,"handling external send request");
        DialogFragment sendFragment = SendDialogFragment.newInstanceWithRequest(request);
        sendFragment.show(getSupportFragmentManager(), null);
    }

    public void loadUsdPrice() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(CPApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        final CPApi api = retrofit.create(CPApi.class);
        api.getSKYInfo().enqueue(new Callback<CPRes>() {
            @Override
            public void onResponse(Call<CPRes> call, Response<CPRes> response) {

                // sometimes kotlin would be nicer...
                if (response != null && response.body() != null &&  response.body().quotes != null &&
                        response.body().quotes.USD != null) {
                    Log.d(TAG,"got CP price USD:"+response.body().quotes.USD.price);
                    PreferenceStore.setUsdPrice(HomeActivity.this,(float) response.body().quotes.USD.price);
                } else {
                    Log.e(TAG,"could not load cmc price");
                }
            }

            @Override
            public void onFailure(Call<CPRes> call, Throwable t) {
                Log.e(TAG,"could not load cp price",t);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SendDialogFragment.PERMISSION_REQ_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    showInfoPopup(getResources().getString(R.string.info),
                            getResources().getString(R.string.retry_qr_scan),
                            getResources().getString(R.string.ok), null);

                } else {
                    // permission denied TODO: hide qr-icons
                    Toast.makeText(this, R.string.no_camera_access, Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    public void temporarilySupressPin() {
        mSupressPin = true;
    }

    @Override
    public boolean shouldRequirePin() {
        FragmentManager man = getSupportFragmentManager();

        Fragment pinFr = man.findFragmentByTag(PinDialogFragment.getFragmentTag());
        if (pinFr != null && pinFr.isVisible()) {
            return false; // dont show pin twice
        }

        if (mSupressPin) {
            mSupressPin = false;
            return false;
        }

        return true;
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshWallets(true);
    }

    public void refreshWallets(boolean loadFromBackend) {
        if (loadFromBackend) {
            NodeUtils.pingNodeSilently(this, Utils.getSkycoinUrl(this));
            loadUsdPrice();
            loadAllWallets(); // generate static crypto data, balances will be 0 after this
            loadBalances(); // async call, loads balances from backend
        } else { // dont load from backend, just update UI
            // if we have an existing wallets container fragment we tell it to update
            WalletsContainerFragment fr = (WalletsContainerFragment) getSupportFragmentManager().findFragmentByTag(WalletsContainerFragment.getFragmentTag());
            if (fr != null) {
                fr.refreshWallets();
            }
        }
    }
    public List<Wallet> getWallets() {
        return mAllWallets;
    }

    // loads balances for each wallet separately.
    private int callCounter; // not pretty but hey it works
    private void loadBalances() {
        Retrofit retrofit = NodeUtils.getRetrofit(Utils.getSkycoinUrl(this));
        if (retrofit == null) {
            showInfoPopup(getResources().getString(R.string.error),
                    getResources().getString(R.string.error_retrofit),
                    getResources().getString(R.string.ok), null);
            return;
        }

        SkycoinService api = retrofit.create(SkycoinService.class);
        callCounter = mAllWallets.size();
        if (callCounter > 0) {
            showLoadingPopup(null);
        }
        boolean hasLoadedAtleastOne = false;
        for (final Wallet w : mAllWallets) {

            if (w.getAddresses() == null || w.getAddresses().size() == 0) {
                callCounter--;
                continue;
            }
            hasLoadedAtleastOne = true;

            String addressList = "";
            for (Address ad : w.getAddresses()) {
                addressList += ad.getAddress() + ",";
            }

            addressList = addressList.substring(0, addressList.length() -1);
            // make 1 call for each wallet
            api.getBalances(addressList).enqueue(new Callback<BalanceRes>() {
                @Override
                public void onResponse(Call<BalanceRes> call, Response<BalanceRes> response) {
                    if (HomeActivity.this.isDestroyed()) {
                        // user has backed out, just skip
                        return;
                    }
                    callCounter--;
                    BalanceRes br = response.body();
                    if (response.code() != HTTP_OK || br == null || br.getConfirmed() == null) {
                        Log.d(TAG,"faile to load from backed:"+response.code());
                        showInfoPopup(getResources().getString(R.string.error),
                                getResources().getString(R.string.error_network),
                                getResources().getString(R.string.ok), null);
                    } else {
                        Log.d(TAG, "got balances: " + br.getConfirmed().getCoins());

                        w.setBalance(br.getConfirmed().getCoins());
                        w.setHours(br.getConfirmed().getHours());

                        // if we have an existing wallets container fragment we tell it to update
                        WalletsContainerFragment fr = (WalletsContainerFragment) getSupportFragmentManager().findFragmentByTag(WalletsContainerFragment.getFragmentTag());
                        if (fr != null) {
                            fr.refreshWallets();
                        }
                    }
                    Log.d(TAG,"callcounter "+callCounter);
                    if (callCounter <= 0) {
                        hideLoadingPopup();
                    }
                }

                @Override
                public void onFailure(Call<BalanceRes> call, Throwable t) {
                    callCounter--;
                    if (HomeActivity.this.isDestroyed()) {
                        // user has backed out, just skip
                        return;
                    }
                    Log.e(TAG, "service error", t);
                    showInfoPopup(getResources().getString(R.string.error),
                            getResources().getString(R.string.error_network),
                            getResources().getString(R.string.ok), null);
                    if (callCounter <= 0) {
                        hideLoadingPopup();
                    }
                }
            });
        }

        if (!hasLoadedAtleastOne) {
            hideLoadingPopup();
        }

    }

    @Override
    public void onBackPressed() {
        FragmentManager man = getSupportFragmentManager();

        Fragment trFr = man.findFragmentByTag(TransactionsFragment.getFragmentTag());
        if (trFr != null && trFr.isAdded()) { // base tx-view should exit on back
            finish();
        }

        Fragment fr = man.findFragmentByTag(AddressListFragment.getFragmentTag());
        if (fr != null && fr.isAdded()) { // pop back if we are watching individual wallet. Will go back to global wallet list
            man.popBackStackImmediate(); // go back to wallet list
            WalletsContainerFragment cfr = (WalletsContainerFragment) man.findFragmentByTag(WalletsContainerFragment.getFragmentTag());
            if (cfr != null) {
                cfr.refreshWallets();
            }
        } else {
            // otherwise call super()
            super.onBackPressed();
        }

    }

    public void onBackPressed(boolean forceReload) {
        FragmentManager man = getSupportFragmentManager();

        Fragment fr = man.findFragmentByTag(AddressListFragment.getFragmentTag());
        if (fr != null && fr.isAdded()) { // pop back if we are watching individual wallet. Will go back to global wallet list
            man.popBackStackImmediate(); // go back to wallet list
            refreshWallets(forceReload);
        } else {
            // otherwise call super()
            super.onBackPressed();
        }

    }

}
