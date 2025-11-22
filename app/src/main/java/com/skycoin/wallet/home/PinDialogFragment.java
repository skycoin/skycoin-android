package com.skycoin.wallet.home;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.skycoin.wallet.R;
import com.skycoin.wallet.encryption.EncryptionManager;
import com.skycoin.wallet.onboarding.OnboardingActivity;
import com.skycoin.wallet.onboarding.PinFragment;
import com.skycoin.wallet.preferences.PreferenceStore;

public class PinDialogFragment extends DialogFragment {

    private static final String TAG = PinFragment.class.getName();

    private String mEnteredPin;
    private String mSavedPin;

    private TextView mHeading;
    private TextView mMessage;

    private TextView mButton1;
    private TextView mButton2;
    private TextView mButton3;
    private TextView mButton4;
    private TextView mButton5;
    private TextView mButton6;
    private TextView mButton7;
    private TextView mButton8;
    private TextView mButton9;
    private TextView mButton0;
    private TextView mButtonDelete;

    private ImageView mDot0;
    private ImageView mDot1;
    private ImageView mDot2;
    private ImageView mDot3;
    private ImageView mDot4;
    private ImageView mDot5;

    private PinCallback mCallback;
    private String mOptionalMessage;
    private boolean mSixDigits = false;
    private Spinner mPinTypeSelector;


    public PinDialogFragment() {
        // Required empty public constructor
    }

    public static String getFragmentTag() {
        return PinDialogFragment.class.getName();
    }

    public static PinDialogFragment newInstance(PinCallback cb, String optionalMessage) {
        PinDialogFragment fragment = new PinDialogFragment();
        fragment.mCallback = cb;
        fragment.mOptionalMessage = optionalMessage;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.DialogFragmentAnimationTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.pin_fragment, container, false);
        v.setBackground(getResources().getDrawable(R.drawable.gradient_blue_rounded));
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (mCallback != null) {
            getDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    // dismiss will only be called if
                    mCallback.onCallback(true,null,0);
                }
            });
        }

        mSixDigits = PreferenceStore.getPinType(getActivity()) == PreferenceStore.PIN_TYPE_6;
        mPinTypeSelector = v.findViewById(R.id.pin_type_selector);
        mPinTypeSelector.setEnabled(false);
        mPinTypeSelector.setVisibility(View.INVISIBLE);

        mDot0 = v.findViewById(R.id.dot_0);
        mDot1 = v.findViewById(R.id.dot_1);
        mDot2 = v.findViewById(R.id.dot_2);
        mDot3 = v.findViewById(R.id.dot_3);
        mDot4 = v.findViewById(R.id.dot_4);
        mDot5 = v.findViewById(R.id.dot_5);
        if (mSixDigits) {
            mDot4.setVisibility(View.VISIBLE);
            mDot5.setVisibility(View.VISIBLE);
        }

        mHeading = v.findViewById(R.id.pin_heading);
        mMessage = v.findViewById(R.id.pin_message);

        mButton0 = v.findViewById(R.id.button_0);
        mButton1 = v.findViewById(R.id.button_1);
        mButton2 = v.findViewById(R.id.button_2);
        mButton3 = v.findViewById(R.id.button_3);
        mButton4 = v.findViewById(R.id.button_4);
        mButton5 = v.findViewById(R.id.button_5);
        mButton6 = v.findViewById(R.id.button_6);
        mButton7 = v.findViewById(R.id.button_7);
        mButton8 = v.findViewById(R.id.button_8);
        mButton9 = v.findViewById(R.id.button_9);
        mButtonDelete = v.findViewById(R.id.button_delete);

        View.OnClickListener cl = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int intTag = -1;
                String tag = (String) v.getTag();
                try {
                    intTag = Integer.parseInt(tag);
                } catch (Exception ex) {
                    // ignore, no update
                }
                if (intTag < 0 || intTag > 9) { // delete button
                    if (!TextUtils.isEmpty(mEnteredPin)) {
                        mEnteredPin = mEnteredPin.substring(0, mEnteredPin.length() - 1);
                    }
                } else {
                    if (TextUtils.isEmpty(mEnteredPin)) {
                        mEnteredPin = "" + intTag;
                    } else if (mEnteredPin.length() < (mSixDigits ? 6 : 4)) {
                        mEnteredPin += intTag;
                    }
                }
                updateKeypad();
                updatePinTexts();
            }
        };

        mButton0.setOnClickListener(cl);
        mButton1.setOnClickListener(cl);
        mButton2.setOnClickListener(cl);
        mButton3.setOnClickListener(cl);
        mButton4.setOnClickListener(cl);
        mButton5.setOnClickListener(cl);
        mButton6.setOnClickListener(cl);
        mButton7.setOnClickListener(cl);
        mButton8.setOnClickListener(cl);
        mButton9.setOnClickListener(cl);
        mButtonDelete.setOnClickListener(cl);

        mSavedPin = PreferenceStore.getPin(getActivity());

        updatePinTexts();

        return v;
    }

    private void updateKeypad() {
        clearAllDots();

        if (TextUtils.isEmpty(mEnteredPin)) {
            mButtonDelete.setEnabled(false);
        } else {
            mButtonDelete.setEnabled(true);
            if (mEnteredPin.length() > 0) {
                mDot0.setImageDrawable(getActivity().getDrawable(R.drawable.orange_dot));
            }
            if (mEnteredPin.length() > 1) {
                mDot1.setImageDrawable(getActivity().getDrawable(R.drawable.orange_dot));
            }
            if (mEnteredPin.length() > 2) {
                mDot2.setImageDrawable(getActivity().getDrawable(R.drawable.orange_dot));
            }
            if (mEnteredPin.length() > 3) {
                mDot3.setImageDrawable(getActivity().getDrawable(R.drawable.orange_dot));
                if (!mSixDigits) {
                    processPin();
                    return;
                }
            }
            if (mEnteredPin.length() > 4) {
                mDot4.setImageDrawable(getActivity().getDrawable(R.drawable.orange_dot));
            }
            if (mEnteredPin.length() > 5) {
                mDot5.setImageDrawable(getActivity().getDrawable(R.drawable.orange_dot));
                processPin();
            }
        }
    }

    public void processPin() {
        String pin = null;
        try {
            pin = new String(EncryptionManager.decrypt(mSavedPin), "UTF-8");
        } catch (Exception ex) {
            Log.e(TAG,"could not decrypt PIN");
            // TODO: show user
            return;
        }

        Log.d(TAG, "saved:" + pin + ", entered:" + mEnteredPin);
        if (pin.equalsIgnoreCase(mEnteredPin)) {
            Log.d(TAG, "PINs match!");
            if (mCallback != null) {
                mCallback.onCallback(true,null,0);
            }
            PinDialogFragment.this.dismiss();
        } else {
            Log.d(TAG, "PINs mismatch!");
            AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
            b.setCancelable(true);
            b.setTitle(R.string.pin_mismatch_heading);
            b.setMessage(R.string.pin_mismatch_message);
            b.setPositiveButton(R.string.pin_mismatch_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    // TODO: make callback onCancel? not needed currently
                }
            });
            b.show();
            mEnteredPin = null;
            updateKeypad();
        }
    }


    private void clearAllDots() {
        mDot0.setImageDrawable(getActivity().getDrawable(R.drawable.black_dot));
        mDot1.setImageDrawable(getActivity().getDrawable(R.drawable.black_dot));
        mDot2.setImageDrawable(getActivity().getDrawable(R.drawable.black_dot));
        mDot3.setImageDrawable(getActivity().getDrawable(R.drawable.black_dot));
        mDot4.setImageDrawable(getActivity().getDrawable(R.drawable.black_dot));
        mDot5.setImageDrawable(getActivity().getDrawable(R.drawable.black_dot));
    }

    private void updatePinTexts() {
        mHeading.setText(R.string.pin_heading_confirm);
        mMessage.setText(R.string.pin_heading_interrogate);
        if (!TextUtils.isEmpty(mOptionalMessage)) {
            mMessage.setText(mOptionalMessage);
        }
    }


    public interface PinCallback {
        public void onCallback(boolean succeeded, String res, int code);
    }

}
