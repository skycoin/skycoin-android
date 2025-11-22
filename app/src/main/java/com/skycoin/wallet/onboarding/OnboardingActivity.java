package com.skycoin.wallet.onboarding;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.skycoin.wallet.BaseActivity;
import com.skycoin.wallet.R;
import com.skycoin.wallet.preferences.PreferenceStore;


public class OnboardingActivity extends BaseActivity {

    private static final String TAG = OnboardingActivity.class.getName();

    private NonSwipeableViewPager mPager;
    private OnboardingFragmentAdapter mFragmentAdapter;

    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        Log.d(TAG,"onCreate, launching onboarding activity");
        setContentView(R.layout.onboarding_activity);

        mPager = findViewById(R.id.pager);
        mFragmentAdapter = new OnboardingFragmentAdapter(getSupportFragmentManager());
        mPager.setAdapter(mFragmentAdapter);
        mPager.setPagingEnabled(false); // user cant scroll manually

        // we always show the popup
        DisclaimerDialogFragment df = DisclaimerDialogFragment.newInstance();
        df.setCancelable(false);
        df.show(getSupportFragmentManager(),null);

        if (!TextUtils.isEmpty(PreferenceStore.getPin(this)) &&  !PreferenceStore.hasDoneFirstWallet(this)) {
            mPager.setCurrentItem(1); // swipe to wallet
        }
    }



    public static class OnboardingFragmentAdapter extends FragmentStatePagerAdapter {

        public OnboardingFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return PinFragment.newInstance();
                case 1: return NewWalletFragment.newInstance();
            }

            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    @Override
    public boolean shouldRequirePin() {
        return false;
    }

    public static class DisclaimerDialogFragment extends DialogFragment {

        static DisclaimerDialogFragment newInstance() {
            DisclaimerDialogFragment f = new DisclaimerDialogFragment();
            return f;
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            View v = inflater.inflate(R.layout.disclaimer_fragment_dialog, container, false);

            // Watch for button clicks.
            final Button button = (Button)v.findViewById(R.id.continue_button);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    DisclaimerDialogFragment.this.dismiss();
                }
            });
            button.setEnabled(false);

            CheckBox cb = v.findViewById(R.id.checkbox);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.d(TAG,"i agree: "+isChecked);
                    PreferenceStore.setHasShownOnboarding(DisclaimerDialogFragment.this.getActivity(), isChecked);
                    button.setEnabled(isChecked);
                }
            });

            return v;
        }
    }

    public void userSelectedPin() {
        Log.d(TAG,"user selected pin, swipe to create wallet");
        mPager.setCurrentItem(1); // swipe to wallet
    }

    @Override
    public void onBackPressed() {
        // back is disabled during onboarding
        return;
    }

}
