package com.skycoin.wallet;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.skycoin.wallet.home.HomeActivity;
import com.skycoin.wallet.onboarding.OnboardingActivity;
import com.skycoin.wallet.preferences.PreferenceStore;

/**
 * Start/Launcher Activity. Has the Splash theme.
 * Basically just checks for the correct screen to launch
 * depending on onboarding, PIN codes, etc.
 */
public class StartActivity extends Activity {

    private static final String TAG = StartActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        // uncomment to test splash more clearly
        /*try {
            Thread.sleep(5000);
        }catch(Exception e) {

        }*/

        // The duration of the Splash is highly dependent
        // on Device performance and can be as little as
        // a few millis. This activity layout has the same
        // look as the Splash so the transition should be
        // pretty ok
        //
        // revert theme to normal appTheme
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_activity);

    }

    protected void onStart() {
        super.onStart();

        checkArguments(getIntent());
    }

    public void onNewIntent(Intent in) {
        checkArguments(in);
    }

    public void checkArguments(Intent in) {
        // decide which activity to show
        if (!PreferenceStore.hasShownOnboarding(this) ||
                TextUtils.isEmpty(PreferenceStore.getPin(this)) ||
                !PreferenceStore.hasDoneFirstWallet(this)) {
            Intent newIn = new Intent(this, OnboardingActivity.class);
            startActivity(newIn); // dont finish() current so we can get back here
        } else {
            Intent newIn = new Intent(this, HomeActivity.class);
            if (in != null && Intent.ACTION_VIEW.equals(in.getAction())) {
                Uri uri = in.getData();
                in.setData(null);
                if (uri != null) {
                    String scheme = uri.getScheme();
                    if (!TextUtils.isEmpty(scheme) && scheme.equalsIgnoreCase(Bip21Utils.BIP21_SCHEME_ID)) {
                        Bip21Utils.Bip21Data request = Bip21Utils.parseSkycoinBip21Url(uri.toString());
                        newIn.putExtra(HomeActivity.ARG_SEND_REQUEST, request);
                    }
                }
            }

            startActivity(newIn);
            finish(); // end this activity so we cant back into it
        }
    }

}
