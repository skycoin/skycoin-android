package com.skycoin.wallet.send;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.skycoin.wallet.BaseActivity;
import com.skycoin.wallet.Bip21Utils;
import com.skycoin.wallet.R;
import com.skycoin.wallet.SkycoinApplication;
import com.skycoin.wallet.Utils;
import com.skycoin.wallet.home.HomeActivity;
import com.skycoin.wallet.home.PinDialogFragment;
import com.skycoin.wallet.nodebackend.NodeUtils;
import com.skycoin.wallet.nodebackend.Utxo;
import com.skycoin.wallet.preferences.PreferenceStore;
import com.skycoin.wallet.wallet.Address;
import com.skycoin.wallet.wallet.Wallet;
import com.skycoin.wallet.wallet.WalletManager;
import com.skycoin.wallet.wallet.WalletUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.skycoin.wallet.send.SendDialogFragment.PERMISSION_REQ_CAMERA;

public class ConfigureOutputsFragment extends Fragment {

    private static final String TAG = ConfigureOutputsFragment.class.getName();

    private TextView mPoolBalance;
    private TextView mPoolHours;
    private EditText mAddressText;
    private EditText mAmountText;
    private EditText mHoursText;
    private EditText mNoteText;

    private Button mBackButton;
    private Button mSendButton;
    private Spinner mChangeDropdown;
    private ImageView mQrButton;
    private TextView mFiatPreview;

    private double maxCoins;
    private long maxHours;
    private String mDstAddress;

    public ConfigureOutputsFragment() {

    }

    public static ConfigureOutputsFragment newInstance(@Nullable String dstAddress) {

        ConfigureOutputsFragment fr = new ConfigureOutputsFragment();
        if (!TextUtils.isEmpty(dstAddress)) {
            Bundle b = new Bundle();
            b.putString(AdvancedSendActivity.ARG_DST_ADDRESS, dstAddress);
            fr.setArguments(b);
        }
        return fr;
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        //out.putStringArrayList(ARG_ALL_ADDRESSES, (ArrayList) mSelectedAddresses);
        super.onSaveInstanceState(out);
    }

    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        Bundle args = getArguments();
        if (args != null && args.containsKey(AdvancedSendActivity.ARG_DST_ADDRESS)) {
            mDstAddress = args.getString(AdvancedSendActivity.ARG_DST_ADDRESS);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.configure_outputs_fragment, container, false);

        mPoolBalance = v.findViewById(R.id.pool_balance_text);
        mPoolHours = v.findViewById(R.id.pool_hours_text);

        mBackButton = v.findViewById(R.id.prev_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((AdvancedSendActivity)getActivity()).userBackedToStepTwo();
            }
        });

        mSendButton = v.findViewById(R.id.send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                completeSend();
            }
        });

        mAddressText = v.findViewById(R.id.address_text);
        if (!TextUtils.isEmpty(mDstAddress)) {
            mAddressText.setText(mDstAddress);
            mDstAddress = null; // we only do this once to avoid overwriting a possible changed user input
        }
        mAmountText = v.findViewById(R.id.amount_text);
        mHoursText = v.findViewById(R.id.hours_text);
        mNoteText = v.findViewById(R.id.note_text);

        mChangeDropdown = v.findViewById(R.id.change_addr_selector);
        mChangeDropdown.setAdapter(new AddressDropDownAdapter());

        mQrButton = v.findViewById(R.id.qr_button);
        mQrButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doQr();
            }
        });

        mFiatPreview = v.findViewById(R.id.fiat_preview);
        TextWatcher amountTv = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                mFiatPreview.setText("");

                if (!TextUtils.isEmpty(mAmountText.getText().toString())) {
                    try {
                        double am = Double.parseDouble(mAmountText.getText().toString());
                        if (am >= 0) {
                            float ratio = PreferenceStore.getUsdPrice(getActivity());
                            if (ratio > 0) {
                                mFiatPreview.setText("$"+ WalletUtils.formatCoinsToSuffix(am * ratio * 1000000, true));
                            } else {
                                mFiatPreview.setText("?");
                            }
                        }
                    } catch (Exception ex) {
                        // bad format
                    }
                }
            }
        };
        mAmountText.addTextChangedListener(amountTv);

        update();

        return v;
    }

    public void completeSend() {
        final String address = mAddressText.getText().toString();
        final String coinStr = mAmountText.getText().toString();
        String hoursStr = mHoursText.getText().toString();

        if (TextUtils.isEmpty(address) || TextUtils.isEmpty(coinStr) || TextUtils.isEmpty(hoursStr)) {
            ((BaseActivity)getActivity()).showInfoPopup(getString(R.string.send),
                    getString(R.string.validation_advanced_send_missing_fields),
                    getString(R.string.ok), null);
            return;
        }

        // this is just to check the string is a valid numeric amount
        double coinsDontUse = 0;
        try {
            coinsDontUse = Double.parseDouble(coinStr);
            if (coinsDontUse <= 0 || coinsDontUse > maxCoins) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            ((BaseActivity)getActivity()).showInfoPopup(getString(R.string.send),
                    getString(R.string.validation_invalid_amount_coins, maxCoins),
                    getString(R.string.ok), null);
            return;
        }

        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        char c = dfs.getDecimalSeparator();
        String sepStr = Character.toString(c);
        Log.d(TAG, "decimal separator " + sepStr + " amount string " + sepStr);
        if (coinStr.contains(sepStr)) {
            String parts = coinStr.substring(coinStr.indexOf(sepStr)+1);
            Log.d(TAG,"parts string: "+parts);
            if (!TextUtils.isEmpty(parts)) {
                if (parts.length() > ((AdvancedSendActivity)getActivity()).mMaxDecimals) {
                    Log.e(TAG, "too many decimals");
                    ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                            getResources().getString(R.string.error_too_many_decimals, ((AdvancedSendActivity)getActivity()).mMaxDecimals),
                            getResources().getString(R.string.ok), null);
                    return;
                }
            }
        }

        long hours = 0;
        try {
            hours = Long.parseLong(hoursStr);
            if (hours < 0 || hours > maxHours) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            ((BaseActivity)getActivity()).showInfoPopup(getString(R.string.send),
                    getString(R.string.validation_invalid_amount_hours, maxHours),
                    getString(R.string.ok), null);
            return;
        }
        // all values provided and legal

        final long hoursToSend = hours;
        if (hours == 0 || hours == maxHours) {
            ((BaseActivity)getActivity()).showConfirmPopup(getString(R.string.send),
                    hours == 0 ? getString(R.string.send_no_hours_warning) : getString(R.string.keep_no_hours_warning),
                    getString(R.string.yes), getString(R.string.no), new BaseActivity.ConfirmCallback() {
                        @Override
                        public void onCallback(boolean confirmed, int code) {
                            Log.d(TAG,"confirmed: "+confirmed);
                            if (confirmed) {
                                ((BaseActivity)getActivity()).requestPin(new PinDialogFragment.PinCallback() {
                                    @Override
                                    public void onCallback(boolean succeeded, String res, int code) {
                                        if (succeeded) {
                                            executeSend(address, ((Address) mChangeDropdown.getSelectedItem()).getAddress(),
                                                    coinStr, hoursToSend, ((AdvancedSendActivity)getActivity()).mBurnFactor);
                                        }
                                    }
                                }, true, getResources().getString(R.string.pin_request_pin_for_sending));
                            }
                        }
                    });
        } else {
            ((BaseActivity)getActivity()).requestPin(new PinDialogFragment.PinCallback() {
                @Override
                public void onCallback(boolean succeeded, String res, int code) {
                    if (succeeded) {
                        executeSend(address, ((Address) mChangeDropdown.getSelectedItem()).getAddress(),
                                coinStr, hoursToSend, ((AdvancedSendActivity)getActivity()).mBurnFactor);
                    }
                }
            }, true, getResources().getString(R.string.pin_request_pin_for_sending));
        }
    }

    private void executeSend(String toAddress, String changeAddress, String skyAmount, long hoursToSend, int burnFactor) {
        Log.d(TAG,"all validated and confirmed, go ahead and transact!");
        AdvancedSendActivity adv = (AdvancedSendActivity)getActivity();

        BigDecimal biggie = new BigDecimal(skyAmount);
        BigDecimal factor = new BigDecimal(1000000l);
        BigInteger droplets = biggie.multiply(factor).toBigInteger();

        try {
            WalletManager.advancedSend(adv, adv.mSelectedWallet.getId(), adv.mSelectedUtxos, toAddress, changeAddress,
                    droplets, new WalletManager.SendCallback() {
                @Override
                public void sendComplete(String res, Exception error, int code) {
                    Log.d(TAG,"tx completed! res:"+res+" err:"+error+" code:"+code);
                    if (getActivity() == null || getActivity().isDestroyed()) {
                        return;
                    }

                    if (code != WalletManager.ERROR_NO_ERROR) {
                        Log.e(TAG, "could not send", error);
                        ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                                getResources().getString(R.string.error_transaction_failed) + ": " + error.getMessage(),
                                getResources().getString(R.string.ok), null);
                    } else {
                        ((SkycoinApplication) getActivity().getApplication()).getDb().insertTx(res, mNoteText.getText().toString(), (System.currentTimeMillis() / 1000));
                        // all good, dismiss dialog and reload wallets
                        ((BaseActivity) getActivity()).showTxSuccessPopup(getResources().getString(R.string.send_success),
                                res,
                                getResources().getString(R.string.ok), new BaseActivity.PopupCallback() {
                                    @Override
                                    public void onCallback(String res, int code) {
                                        getActivity().finish();
                                    }
                                }, res);
                    }
                }
            }, hoursToSend, burnFactor);
        } catch (Exception ex) {
            Log.e(TAG,"Transaction failed: "+ex);
            if (getActivity() == null || getActivity().isDestroyed()) {
                return;
            }
            ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                    getResources().getString(R.string.error_transaction_failed) + ": " + ex.getMessage(),
                    getResources().getString(R.string.ok), null);
        }
    }

    public void doQr() {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQ_CAMERA);
        } else {
            Intent in = new Intent(getActivity(), OcrCaptureActivity.class);
            startActivityForResult(in, OcrCaptureActivity.REQ_QR_CODE);
        }
    }

    public void update() {
        maxCoins = 0;
        maxHours = 0;
        for (Utxo ux : ((AdvancedSendActivity)getActivity()).mSelectedUtxos) {
            maxCoins += Double.parseDouble(ux.getCoins());
            maxHours += ux.getCalculatedHours();
        }
        maxHours = maxHours - (long)Math.ceil(maxHours / (double) ((AdvancedSendActivity)getActivity()).mBurnFactor);
        mPoolBalance.setText(String.format("%."+PreferenceStore.getMaxDecimals(getActivity())+"f", maxCoins));
        mPoolHours.setText(maxHours + "");
        ((AddressDropDownAdapter)mChangeDropdown.getAdapter()).notifyDataSetChanged();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(TAG, "activity result from req " + requestCode);
        if (resultCode == RESULT_OK && requestCode == OcrCaptureActivity.REQ_QR_CODE) {

            String scanRes = data.getStringExtra(OcrCaptureActivity.KEY_QR_CODE);
            Log.d(TAG, "scanned:" + scanRes);
            if (!TextUtils.isEmpty(scanRes)) {
                if (!Bip21Utils.isPossibleUri(scanRes)) {
                    mAddressText.setText(scanRes); // not uri-format. Assume raw address
                } else {
                    Bip21Utils.Bip21Data bip21 = Bip21Utils.parseSkycoinBip21Url(scanRes);

                    if (bip21 == null) {
                        Log.d(TAG, "not 'skycoin:' url");
                        return;
                    }

                    mAddressText.setText(bip21.address);
                    mAmountText.setText(bip21.amount);
                    mNoteText.setText(bip21.message);
                    mHoursText.setText(bip21.hours);
                }
            }
        }

    }

    private class AddressDropDownAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            AdvancedSendActivity adv = (AdvancedSendActivity) getActivity();
            if (adv == null || adv.mSelectedWallet == null || adv.mSelectedWallet.getAddresses() == null) {
                return 0;
            }

            return adv.mSelectedWallet.getAddresses().size();
        }

        @Override
        public Object getItem(int i) {
            AdvancedSendActivity adv = (AdvancedSendActivity) getActivity();
            return adv.mSelectedWallet.getAddresses().get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            // Create a LinearLayout to contain image an text.
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.address_selection_cell, null, false);
            }

            Address adr = (Address) getItem(i);
            if (adr == null) {
                return null;
            }

            TextView addr = view.findViewById(R.id.address);
            TextView balance = view.findViewById(R.id.balance);
            TextView hours = view.findViewById(R.id.hours);

            addr.setText(adr.getAddress());
            balance.setText(WalletUtils.formatCoinsToSuffix(adr.getBalance(), true) + " " + getResources().getString(R.string.currency_short));
            hours.setText(WalletUtils.formatHoursToSuffix(adr.getHours(), true) + " " + getResources().getString(R.string.hours_name));

            View v = view.findViewById(R.id.bottom_divider);
            v.setVisibility(View.GONE);

            return view;
        }

    }


}