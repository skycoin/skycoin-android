package com.skycoin.wallet.home;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.skycoin.wallet.BaseActivity;
import com.skycoin.wallet.R;
import com.skycoin.wallet.SettingsActivity;
import com.skycoin.wallet.encryption.CryptoException;
import com.skycoin.wallet.encryption.EncryptionManager;
import com.skycoin.wallet.nodebackend.BalanceRes;
import com.skycoin.wallet.nodebackend.SkycoinService;
import com.skycoin.wallet.preferences.PreferenceStore;
import com.skycoin.wallet.wallet.Address;
import com.skycoin.wallet.wallet.Wallet;
import com.skycoin.wallet.wallet.WalletManager;
import com.skycoin.wallet.wallet.WalletUtils;

import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WalletsContainerFragment extends Fragment {

    private static final String TAG = WalletsContainerFragment.class.getName();

    private TextView mBalanceSum;
    private TextView mHoursSum;
    private TextView mFiatSum;
    private TextView mHeading;

    private ImageView mSettingsImage;
    private ImageView mRefreshImage;
    private ImageView mShowSeedImage;
    private ImageView mDeleteWalletImage;

    private boolean mShowFullBalance = true; // default to true

    public WalletsContainerFragment() {
        // Required empty public constructor
    }

    public static String getFragmentTag() {
        return WalletsContainerFragment.class.getName();
    }

    public static WalletsContainerFragment newInstance() {
        WalletsContainerFragment fragment = new WalletsContainerFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.wallets_container_fragment, container, false);

        // starting fragment is always wallets-list
        Fragment listFragment = new WalletsListFragment();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, listFragment, WalletsListFragment.getFragmentTag());
        transaction.commit();

        mBalanceSum = v.findViewById(R.id.wallet_balance_num);
        mBalanceSum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"clicked!");
                mShowFullBalance = !mShowFullBalance;
                refreshWallets();
            }
        });

        mFiatSum = v.findViewById(R.id.wallet_balance_fiat);
        mHoursSum = v.findViewById(R.id.wallet_balance_hours);
        mHeading = v.findViewById(R.id.wallet_heading);

        mSettingsImage = v.findViewById(R.id.settings_button);

        mRefreshImage = v.findViewById(R.id.refresh_button);
        mRefreshImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "refresh clicked");
                if (getActivity() != null) {
                    ((HomeActivity) getActivity()).refreshWallets(true);
                }
            }
        });

        mShowSeedImage = v.findViewById(R.id.show_seed_button);
        mShowSeedImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "show seed");
                if (getActivity() != null) {
                    ((BaseActivity) getActivity()).requestPin(new PinDialogFragment.PinCallback() {
                        @Override
                        public void onCallback(boolean succeeded, String res, int code) {
                            if (succeeded) {
                                AddressListFragment adfr = (AddressListFragment) getFragmentManager().findFragmentByTag(AddressListFragment.getFragmentTag());
                                if (adfr != null && adfr.isAdded()) {
                                    String[] seedAndName = WalletManager.getSeedAndNameForWallet(getActivity(),adfr.getWalletId());
                                    if (seedAndName != null && seedAndName.length > 0) {
                                        try {
                                            String decryptedSeed = new String(EncryptionManager.decrypt(seedAndName[0]), WalletManager.USE_STRING_ENCODING);
                                            ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.wallet_seed_heading),
                                                    decryptedSeed, getResources().getString(R.string.ok), null);
                                        } catch (CryptoException | UnsupportedEncodingException ex) {
                                            Log.e(TAG,"could not decrypt seed");
                                            ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                                                    getResources().getString(R.string.error_could_not_decrypt_seed),
                                                    getResources().getString(R.string.ok), null);
                                        }
                                    }
                                }
                            }
                        }
                    }, true, getResources().getString(R.string.wallets_show_seed));
                }
            }
        });

        mDeleteWalletImage  = v.findViewById(R.id.delete_wallet_button);
        mDeleteWalletImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "delete wallet");
                if (getActivity() != null) {
                    AlertDialog.Builder b = new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
                    b.setCancelable(true);

                    ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.RED);
                    String titleText = getString(R.string.warning);

                    SpannableStringBuilder ssBuilder = new SpannableStringBuilder(titleText);
                    ssBuilder.setSpan(
                            foregroundColorSpan,
                            0,
                            titleText.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    );
                    b.setTitle(ssBuilder);
                    b.setMessage(R.string.delete_warning);
                    b.setNegativeButton(R.string.delete_it, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.d(TAG,"delete it");
                            if (getActivity() != null) {
                                ((BaseActivity)getActivity()).requestPin(new PinDialogFragment.PinCallback() {
                                    @Override
                                    public void onCallback(boolean succeeded, String res, int code) {
                                        Log.d(TAG,"delete pin check: "+succeeded);
                                        if (!succeeded) {
                                            return;
                                        }

                                        // delete wallet and go back to start screen
                                        AddressListFragment adfr = (AddressListFragment) getFragmentManager().findFragmentByTag(AddressListFragment.getFragmentTag());
                                        if (adfr != null && adfr.isAdded()) {
                                            WalletManager.deleteWallet(getActivity(), adfr.getWalletId());
                                            ((HomeActivity)getActivity()).onBackPressed(true); // fake back press
                                            refreshWallets();
                                        }
                                    }
                                }, true, "Optional message");
                            }
                        }
                    });
                    b.setPositiveButton(R.string.keep_it, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.d(TAG,"keep it");
                        }
                    });
                    b.create().show();
                }
            }
        });

        updateHeaderInfo(((HomeActivity) getActivity()).getWallets(), false); // at this point we have no wallets so values will be 0

        return v;
    }

    // assumes all data exists, only updates UI
    public void refreshWallets() {
        if (getActivity() == null) {
            return; // invalid context
        }

        Log.d(TAG, "updating container fragment");

        WalletsListFragment fr = (WalletsListFragment) getFragmentManager().findFragmentByTag(WalletsListFragment.getFragmentTag());
        if (fr != null && fr.isAdded()) {
            updateHeaderInfo(((HomeActivity) getActivity()).getWallets(), false);
            fr.updateWallets();
            return;
        }

        AddressListFragment adfr = (AddressListFragment) getFragmentManager().findFragmentByTag(AddressListFragment.getFragmentTag());
        if (adfr != null && adfr.isAdded()) {
            List<Wallet> wl = new ArrayList<>();
            for (Wallet wall : ((HomeActivity) getActivity()).getWallets()) {
                if (wall.getId().equals(adfr.getWalletId())) {
                    wl.add(wall);
                    break;
                }
            }

            updateHeaderInfo(wl, true);
            adfr.reloadWalletData();
        }
    }


    public void updateHeaderInfo(List<Wallet> useThese, boolean singleWallet) {
        if (getActivity() == null) {
            return; // invalid context
        }

        long bal = 0;
        long hours = 0;
        if (useThese != null) {
            for (Wallet w : useThese) {
                bal += w.getBalance();
                hours += w.getHours();
            }
        }

        if (useThese != null && useThese.size() == 1 && singleWallet) {
            mHeading.setText(useThese.get(0).getName());
            mSettingsImage.setImageDrawable(getResources().getDrawable(R.drawable.back_white));
            mSettingsImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "back clicked");
                    getActivity().onBackPressed();
                }
            });
            mShowSeedImage.setVisibility(View.VISIBLE);
            mDeleteWalletImage.setVisibility(View.VISIBLE);
        } else {
            mHeading.setText(R.string.wallets_list_heading);
            mSettingsImage.setImageDrawable(getResources().getDrawable(R.drawable.settings));
            mSettingsImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "settings clicked");
                    ((HomeActivity)getActivity()).temporarilySupressPin();
                    Intent in = new Intent(getActivity(), SettingsActivity.class);
                    startActivity(in);
                }
            });
            mShowSeedImage.setVisibility(View.INVISIBLE);
            mDeleteWalletImage.setVisibility(View.INVISIBLE);
        }


        mBalanceSum.setText(WalletUtils.formatCoinsToSuffix(bal, !mShowFullBalance));
        mHoursSum.setText(WalletUtils.formatHoursToSuffix(hours, !mShowFullBalance) + " " + getResources().getString(R.string.hours_name));

        float usd = PreferenceStore.getUsdPrice(getActivity());
        if (usd > 0) {
            NumberFormat df = DecimalFormat.getInstance();
            df.setMaximumFractionDigits(2);
            mFiatSum.setVisibility(View.VISIBLE);
            mFiatSum.setText("$"+df.format(usd * (bal / 1000000.0)) + " ($"+df.format(usd)+")");
        } else {
            mFiatSum.setVisibility(View.INVISIBLE);
        }
    }

}
