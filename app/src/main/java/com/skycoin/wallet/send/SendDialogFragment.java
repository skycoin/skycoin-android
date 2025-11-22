package com.skycoin.wallet.send;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.skycoin.wallet.BaseActivity;
import com.skycoin.wallet.Bip21Utils;
import com.skycoin.wallet.R;
import com.skycoin.wallet.SkycoinApplication;
import com.skycoin.wallet.Utils;
import com.skycoin.wallet.home.HomeActivity;
import com.skycoin.wallet.home.PinDialogFragment;
import com.skycoin.wallet.nodebackend.NodeUtils;
import com.skycoin.wallet.preferences.PreferenceStore;
import com.skycoin.wallet.wallet.Wallet;
import com.skycoin.wallet.wallet.WalletManager;
import com.skycoin.wallet.wallet.WalletUtils;

import java.awt.font.TextAttribute;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormatSymbols;

import static android.app.Activity.RESULT_OK;

public class SendDialogFragment extends DialogFragment {

    private static final String ARG_USE_WALLET = "com.skycoin.arg_send_use_wallet";
    private static final String ARG_SEND_REQUEST = "com.skycoin.arg_send_request";

    public static final int PERMISSION_REQ_CAMERA = 45600;

    private static final String TAG = SendDialogFragment.class.getName();

    private String mUseWalletId;
    private Spinner mWalletSelector;

    private Button mSendButton;
    private Button mCancelButton;
    private Button mAdvancedButton;
    private EditText mAddressText;
    private EditText mAmountText;
    private EditText mNoteText;
    private TextView mFiatPreview;

    public SendDialogFragment() {
    }

    public static SendDialogFragment newInstanceWithRequest(Bip21Utils.Bip21Data request) {
        SendDialogFragment fragment = SendDialogFragment.newInstance(null);
        Bundle args = new Bundle();
        args.putSerializable(ARG_SEND_REQUEST,request);
        fragment.setArguments(args);

        return fragment;
    }

    public static SendDialogFragment newInstance(Wallet sendFromWallet) {
        SendDialogFragment fragment = new SendDialogFragment();

        Bundle b = new Bundle();

        if (sendFromWallet != null) {
            b.putString(ARG_USE_WALLET, sendFromWallet.getId());
        }

        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.DialogFragmentAnimationTheme);

        Bundle args = getArguments();
        if (savedInstanceState != null) {
            args = savedInstanceState;
        }
        mUseWalletId = args.getString(ARG_USE_WALLET);

    }

    private void populateRequest(Bip21Utils.Bip21Data request) {
        mAddressText.setText(request.address);
        mAmountText.setText(request.amount);
        mNoteText.setText(request.message);
    }

    public void onResume() {
        super.onResume();
        int adjust = (int) (getResources().getDisplayMetrics().density * 40);
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels - adjust;
        getDialog().getWindow().setLayout(width, height);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().setGravity(Gravity.BOTTOM);

        View v = inflater.inflate(R.layout.send_fragment, container, false);

        mWalletSelector = v.findViewById(R.id.wallet_selector);
        mWalletSelector.setAdapter(new WalletDropDownAdapter());
        mWalletSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "selected " + i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ImageView qrImage = v.findViewById(R.id.qr_button);
        qrImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "clicked QR button");

                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.CAMERA},
                            PERMISSION_REQ_CAMERA);
                } else {
                    Intent in = new Intent(getActivity(), OcrCaptureActivity.class);
                    startActivityForResult(in, OcrCaptureActivity.REQ_QR_CODE);
                    ((HomeActivity) getActivity()).temporarilySupressPin(); // just once
                }
            }
        });

        mNoteText = v.findViewById(R.id.note_text);

        mCancelButton = v.findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendDialogFragment.this.dismiss();
            }
        });
        mSendButton = v.findViewById(R.id.send_button);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "SEND pressed");

                ((BaseActivity) getActivity()).requestPin(new PinDialogFragment.PinCallback() {
                    @Override
                    public void onCallback(boolean succeeded, String res, int code) {
                        if (succeeded) {
                            if (getActivity() == null) {
                                return; // user left
                            }
                            NodeUtils.getNodeRules(getActivity(), Utils.getSkycoinUrl(getActivity()), new NodeUtils.NodeRulesCallback() {
                                @Override
                                public void onNodeRules(boolean success, @Nullable Throwable error, int burnFactor, int maxDecimals) {
                                    if (getActivity() == null || getActivity().isDestroyed()) {
                                        return;
                                    }
                                    if (!success) {
                                        Log.e(TAG, "could not load node rules, exception:" + error);
                                        ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                                                getResources().getString(R.string.error_load_node_rules),
                                                getResources().getString(R.string.ok), null);
                                        return;
                                    }
                                    Wallet w = (Wallet) mWalletSelector.getSelectedItem();
                                    String to = mAddressText.getText().toString().trim();
                                    String amount = mAmountText.getText().toString().trim();
                                    DecimalFormatSymbols dfs = new DecimalFormatSymbols();
                                    char c = dfs.getDecimalSeparator();
                                    String sepStr = Character.toString(c);
                                    Log.d(TAG, "decimal separator " + sepStr + " amount string " + amount);
                                    if (amount.contains(sepStr)) {
                                        String parts = amount.substring(amount.indexOf(sepStr)+1);
                                        Log.d(TAG,"parts string: "+parts);
                                        if (!TextUtils.isEmpty(parts)) {
                                            if (parts.length() > maxDecimals) {
                                                Log.e(TAG, "too many decimals");
                                                ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                                                        getResources().getString(R.string.error_too_many_decimals, maxDecimals),
                                                        getResources().getString(R.string.ok), null);
                                                return;
                                            }
                                        }
                                    }

                                    try {
                                        BigDecimal biggie = new BigDecimal(amount);
                                        BigDecimal factor = new BigDecimal(1000000l);
                                        BigInteger droplets = biggie.multiply(factor).toBigInteger();
                                        ((BaseActivity) getActivity()).showLoadingPopup(getResources().getString(R.string.sending));
                                        WalletManager.send(getActivity(), w, to, droplets, burnFactor, new WalletManager.SendCallback() {
                                            @Override
                                            public void sendComplete(String res, Exception error, int code) {
                                                if (getActivity() == null || getActivity().isDestroyed()) {
                                                    return;
                                                }

                                                ((BaseActivity) getActivity()).hideLoadingPopup();
                                                if (code != WalletManager.ERROR_NO_ERROR) {
                                                    Log.e(TAG, "could not send", error);
                                                    ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                                                            getResources().getString(R.string.error_transaction_failed) + ": " + res,
                                                            getResources().getString(R.string.ok), null);
                                                } else {
                                                    ((SkycoinApplication) getActivity().getApplication()).getDb().insertTx(res, mNoteText.getText().toString(), (System.currentTimeMillis() / 1000));
                                                    // all good, dismiss dialog and reload wallets
                                                    ((BaseActivity) getActivity()).showTxSuccessPopup(getResources().getString(R.string.send_success),
                                                            res,
                                                            getResources().getString(R.string.ok), new BaseActivity.PopupCallback() {
                                                                @Override
                                                                public void onCallback(String res, int code) {
                                                                    if (getActivity() == null) {
                                                                        return; // user has left
                                                                    }
                                                                    ((HomeActivity) getActivity()).refreshWallets(true);
                                                                    SendDialogFragment.this.dismiss();
                                                                }
                                                            }, res);
                                                }
                                            }
                                        });
                                    } catch (Exception ex) {
                                        if (getActivity() == null || getActivity().isDestroyed()) {
                                            return;
                                        }

                                        ((BaseActivity) getActivity()).hideLoadingPopup();
                                        Log.e(TAG, "could not send", ex);
                                        ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                                                getResources().getString(R.string.error_transaction_failed) + ": " + ex.getMessage(),
                                                getResources().getString(R.string.ok), null);
                                    }

                                }
                            });

                        }
                    }
                }, true, getResources().getString(R.string.pin_request_pin_for_sending));

            }
        });

        mAdvancedButton = v.findViewById(R.id.advanced_button);
        mAdvancedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((HomeActivity) getActivity()).temporarilySupressPin(); // on launch
                Intent in = new Intent(getActivity(), AdvancedSendActivity.class);
                Gson gson = new Gson();
                in.putExtra(AdvancedSendActivity.ARG_WALLET_LIST, gson.toJson(((HomeActivity) getActivity()).getWallets()));
                String addr = mAddressText.getText().toString();
                if (!TextUtils.isEmpty(addr)) {
                    in.putExtra(AdvancedSendActivity.ARG_DST_ADDRESS, addr);
                }
                getActivity().startActivity(in);
                SendDialogFragment.this.dismiss();
            }
        });

        mAddressText = v.findViewById(R.id.address_text);
        mAmountText = v.findViewById(R.id.amount_text);

        TextWatcher tv = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(mAddressText.getText().toString())) {
                    mSendButton.setEnabled(false);
                } else {
                    try {
                        double am = Double.parseDouble(mAmountText.getText().toString());
                        if (am <= 0) {
                            mSendButton.setEnabled(false);
                        } else {
                            mSendButton.setEnabled(true);
                        }
                    } catch (Exception ex) {
                        // bad format
                        mSendButton.setEnabled(false);
                    }
                }
            }
        };

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
                                mFiatPreview.setText("$" + WalletUtils.formatCoinsToSuffix(am * ratio * 1000000, true));
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

        mAddressText.addTextChangedListener(tv);
        mAmountText.addTextChangedListener(tv);
        mAmountText.addTextChangedListener(amountTv);

        mSendButton.setEnabled(false);

        mFiatPreview = v.findViewById(R.id.fiat_preview);

        Bundle args = getArguments();
        Bip21Utils.Bip21Data request = (Bip21Utils.Bip21Data) args.getSerializable(ARG_SEND_REQUEST);
        if (request != null) {
            args.remove(ARG_SEND_REQUEST);
            populateRequest(request);
        }

        return v;
    }

    private class WalletDropDownAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            HomeActivity ha = (HomeActivity) getActivity();
            return ha != null && ha.getWallets() != null ? ha.getWallets().size() : 0;
        }

        @Override
        public Object getItem(int i) {
            HomeActivity ha = (HomeActivity) getActivity();
            return ha != null && ha.getWallets() != null ? ha.getWallets().get(i) : null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            // Create a LinearLayout to contain image an text.
            if (view == null) {
                view = getLayoutInflater().inflate(R.layout.wallet_dropdown_cell, null, false);
            }

            Wallet w = (Wallet) getItem(i);
            if (w == null) {
                return null;
            }

            TextView name = view.findViewById(R.id.wallet_name);
            TextView balance = view.findViewById(R.id.wallet_balance);

            name.setText(w.getName());
            balance.setText(WalletUtils.formatCoinsToSuffix(w.getBalance(), true));

            return view;
        }

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

                    Log.d(TAG, "parsed bip21 scheme: " + bip21.scheme);
                    Log.d(TAG, "parsed bip21 address: " + bip21.address);
                    Log.d(TAG, "parsed bip21 amount: " + bip21.amount);
                    Log.d(TAG, "parsed bip21 hours: " + bip21.hours);
                    Log.d(TAG, "parsed bip21 message: " + bip21.message);
                    Log.d(TAG, "parsed bip21 label: " + bip21.label);

                    mAddressText.setText(bip21.address);
                    mAmountText.setText(bip21.amount);
                    mNoteText.setText(bip21.message);
                }
            }
        }


    }

}
