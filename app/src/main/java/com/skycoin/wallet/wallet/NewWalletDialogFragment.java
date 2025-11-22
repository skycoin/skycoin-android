package com.skycoin.wallet.wallet;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.skycoin.wallet.BaseActivity;
import com.skycoin.wallet.R;
import com.skycoin.wallet.home.HomeActivity;
import com.skycoin.wallet.preferences.PreferenceStore;
import com.skycoin.wallet.wallet.WalletManager;

public class NewWalletDialogFragment extends DialogFragment {

    private static final String ARG_SHOULD_CREATE_NEW = "com.skycoin.arg_create_new_wallet";

    private static final String TAG = NewWalletDialogFragment.class.getName();

    private boolean mCreateNew;

    private EditText mWalletName;
    private EditText mWalletSeed;
    private EditText mWalletSeedConfirm;
    private TextView mConfirmHeading;
    private Button mCreateButton;
    private Button mCancelButton;
    private Button mNewSeedButton;
    private HandlerThread ht;

    public NewWalletDialogFragment() {
    }

    public static NewWalletDialogFragment newInstance(boolean createNew) {
        NewWalletDialogFragment fragment = new NewWalletDialogFragment();

        Bundle b = new Bundle();
        b.putBoolean(ARG_SHOULD_CREATE_NEW, createNew);
        fragment.setArguments(b);

        return fragment;
    }

    public void onResume() {
        super.onResume();
        int adjust = (int) (getResources().getDisplayMetrics().density * 40);
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels  - adjust;
        getDialog().getWindow().setLayout(width, height);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.DialogFragmentAnimationTheme);

        Bundle args = getArguments();
        if (savedInstanceState != null) {
            args = savedInstanceState;
        }
        mCreateNew = args.getBoolean(ARG_SHOULD_CREATE_NEW);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().setGravity(Gravity.BOTTOM);

        View v = inflater.inflate(R.layout.new_wallet_dialog_fragment, container, false);

        ht = new HandlerThread("saveseed thread");
        ht.start();

        mWalletName = v.findViewById(R.id.wallet_name_text);
        mWalletSeed = v.findViewById(R.id.wallet_seed_text);
        mWalletSeedConfirm = v.findViewById(R.id.wallet_seed_confirm);
        mConfirmHeading = v.findViewById(R.id.wallet_confirm_seed_heading);
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

                final String name = mWalletName.getText().toString().trim();
                final String seed = mWalletSeed.getText().toString().trim();
                final String confSeed = mWalletSeedConfirm.getText().toString().trim();

                if (TextUtils.isEmpty(seed)) {
                    ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.seed),
                            getResources().getString(R.string.no_seed), getResources().getString(R.string.ok), null);
                    return;
                }

                if (mCreateNew) {
                    if (!seed.equals(confSeed)) {
                        ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.seed),
                                getResources().getString(R.string.seed_no_match), getResources().getString(R.string.ok), null);
                        return;
                    }
                }

                ((BaseActivity)getActivity()).showLoadingPopup(getResources().getString(R.string.scanning));

                Handler h = new Handler(ht.getLooper());
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (WalletManager.doesSeedExist(getActivity(), seed)) {
                                ((BaseActivity)getActivity()).hideLoadingPopup();
                                ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.seed),
                                        getResources().getString(R.string.seed_duplicate),
                                        getResources().getString(R.string.ok), null);
                                return;
                            }
                        } catch (Exception ex) {
                            // maybe a major issue with decryption but this is just a utility method
                            // so we ignore it and hope for the best. Worst case is we get a duplicate wallet
                        }

                        // seed seems ok
                        // save wallet encrypted seed | name
                        WalletManager.saveSeed(getActivity(), seed, name, new WalletManager.CreateCallback() {
                            @Override
                            public void createComplete(boolean success, Exception error) {
                                if (getActivity() == null || getActivity().isDestroyed()) {
                                    // use backed out
                                    return;
                                }
                                ((BaseActivity)getActivity()).hideLoadingPopup();

                                if (!success) {
                                    ((BaseActivity)getActivity()).showInfoPopup(getResources().getString(R.string.error),
                                            getResources().getString(R.string.error_scanning_wallet),
                                            getResources().getString(R.string.ok), null);
                                }

                                ((HomeActivity) getActivity()).refreshWallets(true);

                                NewWalletDialogFragment.this.dismissAllowingStateLoss();
                            }
                        });
                    }
                });
            }
        });

        mCancelButton = v.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NewWalletDialogFragment.this.dismiss();
            }
        });

        mNewSeedButton = v.findViewById(R.id.new_seed_button);
        mNewSeedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                injectNewSeed();
            }
        });

        TextView heading = v.findViewById(R.id.new_wallet_heading);
        if (mCreateNew) {
            injectNewSeed();
            mWalletSeedConfirm.setVisibility(View.VISIBLE);
            mConfirmHeading.setVisibility(View.VISIBLE);
            mNewSeedButton.setVisibility(View.VISIBLE);
            heading.setText(R.string.wallet_heading_new);
        } else {
            mWalletSeedConfirm.setVisibility(View.GONE);
            mConfirmHeading.setVisibility(View.GONE);
            mNewSeedButton.setVisibility(View.GONE);
            heading.setText(R.string.wallet_heading_load);
        }

        if (!mCreateNew) {
            mCreateButton.setEnabled(true);
        }

        return v;
    }

    public void onDestroy() {
        super.onDestroy();
        ht.quit();
    }

    private void injectNewSeed() {
        try {
            String newSeed = WalletManager.generateNewSeed();
            mWalletSeed.setText(newSeed);
        } catch (Exception ex) {
            Log.e(TAG,"could not generate seed",ex);
            // TODO: popup and warn
        }
    }

}
