package com.skycoin.wallet.onboarding;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.skycoin.wallet.BaseActivity;
import com.skycoin.wallet.R;
import com.skycoin.wallet.home.HomeActivity;
import com.skycoin.wallet.preferences.PreferenceStore;
import com.skycoin.wallet.wallet.NewWalletDialogFragment;
import com.skycoin.wallet.wallet.WalletManager;

public class NewWalletFragment extends Fragment {

    private static final String TAG = NewWalletFragment.class.getName();

    private RelativeLayout mSwitch;
    private View mSliderIndicator;
    private TextView mNewLabel;
    private TextView mLoadLabel;

    private EditText mWalletName;
    private EditText mWalletSeed;
    private EditText mWalletSeedConfirm;
    private TextView mWalletSeedConfirmLabel;
    private Button mCreateButton;

    private boolean mNewMode = true;

    public NewWalletFragment() {
    }

    public static NewWalletFragment newInstance() {
        NewWalletFragment fragment = new NewWalletFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.new_wallet_fragment, container, false);
        mNewLabel = v.findViewById(R.id.new_label);
        mLoadLabel = v.findViewById(R.id.load_label);
        mSliderIndicator = v.findViewById(R.id.slider_background);

        mSwitch = v.findViewById(R.id.switcher);
        mSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNewMode = !mNewMode;

                if (mNewMode) {
                    // new seed mode
                    injectNewSeed();
                    mWalletSeedConfirm.setVisibility(View.VISIBLE);
                    mWalletSeedConfirmLabel.setVisibility(View.VISIBLE);
                    mCreateButton.setEnabled(false);
                    mNewLabel.setTextColor(getResources().getColor(R.color.lightGreyAlmostWhite));
                    mLoadLabel.setTextColor(getResources().getColor(R.color.lightGreyText));
                    mSliderIndicator.animate().translationX(0).setDuration(200).start();
                } else {
                    mWalletSeed.setText(null);
                    mWalletSeedConfirm.setVisibility(View.INVISIBLE);
                    mWalletSeedConfirmLabel.setVisibility(View.INVISIBLE);
                    mCreateButton.setEnabled(true);
                    mNewLabel.setTextColor(getResources().getColor(R.color.lightGreyText));
                    mLoadLabel.setTextColor(getResources().getColor(R.color.lightGreyAlmostWhite));
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mSliderIndicator.getLayoutParams();
                    mSliderIndicator.animate().translationX(mSwitch.getWidth() -
                            mSliderIndicator.getWidth() - lp.leftMargin - lp.rightMargin).setDuration(200).start();
                }
                mSwitch.invalidate();
                mSwitch.requestLayout();
            }
        });

        mWalletName = v.findViewById(R.id.wallet_name_text);
        mWalletSeed = v.findViewById(R.id.wallet_seed_text);
        mWalletSeedConfirmLabel = v.findViewById(R.id.wallet_confirm_seed_heading);
        mWalletSeedConfirm = v.findViewById(R.id.wallet_seed_confirm);
        mWalletSeedConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String seed = mWalletSeed.getText().toString();
                if (TextUtils.isEmpty(seed)) {
                    mCreateButton.setEnabled(false);
                    return;
                }
                String current = s.toString();
                if (seed.equals(current)) {
                    mCreateButton.setEnabled(true);
                } else {
                    mCreateButton.setEnabled(false);
                }
            }
        });


        mCreateButton = v.findViewById(R.id.create_button);
        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "clicked create button");
                String name = mWalletName.getText().toString().trim();
                String seed = mWalletSeed.getText().toString().trim();
                String confSeed = mWalletSeedConfirm.getText().toString().trim();

                if (TextUtils.isEmpty(seed)) {
                    ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.seed),
                            getResources().getString(R.string.no_seed), getResources().getString(R.string.ok), null);
                    return;
                }

                if (mNewMode) {
                    if (!seed.equals(confSeed)) {
                        ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.seed),
                                getResources().getString(R.string.seed_no_match), getResources().getString(R.string.ok), null);
                        return;
                    }
                }

                // seed seems ok
                ReminderDialogFragment df = ReminderDialogFragment.newInstance(seed, name);
                df.setCancelable(true);
                df.show(getFragmentManager(),null);

            }
        });

        // we are always in "new" mode onCreate so generate new seed
        injectNewSeed();

        return v;
    }

    private void injectNewSeed() {
        try {
            String seed = WalletManager.generateNewSeed();
            mWalletSeed.setText(seed);
        } catch (Exception ex) {
            Log.e(TAG,"could not generate seed",ex);
            // TODO: popup and warn
        }

    }


    public static class ReminderDialogFragment extends DialogFragment {

        private static String mSeed;
        private static String mName;
        private HandlerThread ht;

        static ReminderDialogFragment newInstance(String seed, String name) {
            ReminderDialogFragment f = new ReminderDialogFragment();
            mSeed = seed;
            mName = name;
            return f;
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            View v = inflater.inflate(R.layout.disclaimer_fragment_dialog, container, false);

            TextView heading = v.findViewById(R.id.title);
            heading.setTextColor(getResources().getColor(R.color.reminder_red));
            heading.setText(R.string.seed_reminder_heading);
            TextView message = v.findViewById(R.id.message);
            message.setText(R.string.seed_reminder_message);

            ht = new HandlerThread("saveseed thread"); // instantiate just once
            ht.start();

            // Watch for button clicks.
            final Button button = (Button)v.findViewById(R.id.continue_button);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    ((BaseActivity)getActivity()).showLoadingPopup(getResources().getString(R.string.scanning));
                    // save wallet encrypted seed | name
                    Handler h = new Handler(ht.getLooper());
                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            WalletManager.saveSeed(getActivity(), mSeed, mName, new WalletManager.CreateCallback() {
                                @Override
                                public void createComplete(boolean success, Exception error) {
                                    if (getActivity() == null || getActivity().isDestroyed()) {
                                        return; // user backed out
                                    }

                                    ((BaseActivity) getActivity()).hideLoadingPopup();

                                    if (!success) {
                                        ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                                                getResources().getString(R.string.error_scanning_wallet),
                                                getResources().getString(R.string.ok), null);
                                    }

                                    getActivity().finish(); // send us back to startActivity
                                    PreferenceStore.setHasShownOnboarding(getActivity(), true);
                                    PreferenceStore.setHasDoneFirstWallet(getActivity(), true);
                                    ReminderDialogFragment.this.dismiss();
                                }
                            });
                        }
                    });
                }
            });
            button.setEnabled(false);

            CheckBox cb = v.findViewById(R.id.checkbox);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    button.setEnabled(isChecked);
                }
            });
            cb.setText(R.string.seed_reminder_checkbox);

            return v;
        }


        public void onDestroy() {
            super.onDestroy();
            ht.quit();
        }

    }

}
