package com.skycoin.wallet.onboarding;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.skycoin.wallet.BaseActivity;
import com.skycoin.wallet.R;
import com.skycoin.wallet.encryption.EncryptionManager;
import com.skycoin.wallet.preferences.PreferenceStore;

public class PinFragment extends Fragment {

    private static final String TAG = PinFragment.class.getName();

    private static final String ARG_INTERROGATE = "com.skycon.arg_interrogate_pin";

    private String mEnteredPin;
    private String mSavedPin;
    private boolean mConfirming = false;
    private boolean mSixDigits = false;
    private Spinner mPinTypeSelector;

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


    public PinFragment() {
        // Required empty public constructor
    }

    public static PinFragment newInstance() {
        PinFragment fragment = new PinFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.pin_fragment, container, false);

        mDot0 = v.findViewById(R.id.dot_0);
        mDot1 = v.findViewById(R.id.dot_1);
        mDot2 = v.findViewById(R.id.dot_2);
        mDot3 = v.findViewById(R.id.dot_3);
        mDot4 = v.findViewById(R.id.dot_4);
        mDot5 = v.findViewById(R.id.dot_5);

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
                }catch (Exception ex) {
                    // ignore, no update
                }
                if (intTag < 0 || intTag > 9) { // delete button
                    if (!TextUtils.isEmpty(mEnteredPin)) {
                        mEnteredPin = mEnteredPin.substring(0,mEnteredPin.length() -1);
                    }
                } else {
                    if (TextUtils.isEmpty(mEnteredPin)) {
                        mEnteredPin = ""+intTag;
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

        mPinTypeSelector = v.findViewById(R.id.pin_type_selector);
        mPinTypeSelector.setAdapter(new PinTypeAdapter());
        mPinTypeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG,"selected pin type "+position);
                if (position == 0) {
                    mSixDigits = false;
                } else if (position == 1) {
                    mSixDigits = true;
                }
                mEnteredPin = "";
                updateKeypad();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSixDigits = false;
                updateKeypad();
            }
        });

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
        // FIXME last dot wont be visible very long
        if (mConfirming) {
            Log.d(TAG,"first:"+mSavedPin+", conf:"+mEnteredPin);
            if (mSavedPin.equalsIgnoreCase(mEnteredPin)) {
                Log.d(TAG,"PINs match!");
                try {
                    String encryptedPin = EncryptionManager.encrypt(mEnteredPin.getBytes("UTF-8"));
                    PreferenceStore.setPinString(getActivity(), encryptedPin);
                    if (mSixDigits) {
                        PreferenceStore.setPinType(getActivity(), PreferenceStore.PIN_TYPE_6);
                    } else {
                        PreferenceStore.setPinType(getActivity(), PreferenceStore.PIN_TYPE_4);
                    }
                    if (getActivity() instanceof OnboardingActivity) {
                        ((OnboardingActivity)getActivity()).userSelectedPin();
                    } else {
                        getFragmentManager().popBackStack();
                    }
                } catch (Exception ex) { // shitload of exceptions
                    Log.e(TAG,"could not encrypt user PIN",ex);
                    // TODO: show popup and die. Not safe to continue
                }
            } else {
                Log.d(TAG,"PINs mismatch!");
                AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
                b.setCancelable(true);
                b.setTitle(R.string.pin_mismatch_heading);
                b.setMessage(R.string.pin_mismatch_message);
                b.setPositiveButton(R.string.pin_mismatch_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                b.show();
                mConfirming = false;
                mPinTypeSelector.setVisibility(View.VISIBLE);
                mSavedPin = null;
                mEnteredPin = null;
                updateKeypad();
                updatePinTexts();
            }
        } else {
            // all set, clear and request confirmation
            mSavedPin = mEnteredPin;
            mEnteredPin = null;
            mConfirming = true;
            mPinTypeSelector.setVisibility(View.INVISIBLE);
            updateKeypad(); // wont recurse since we cleared mEnteredPin
        }
    }

    private void clearAllDots() {
        mDot0.setImageDrawable(getActivity().getDrawable(R.drawable.black_dot));
        mDot1.setImageDrawable(getActivity().getDrawable(R.drawable.black_dot));
        mDot2.setImageDrawable(getActivity().getDrawable(R.drawable.black_dot));
        mDot3.setImageDrawable(getActivity().getDrawable(R.drawable.black_dot));
        mDot4.setImageDrawable(getActivity().getDrawable(R.drawable.black_dot));
        mDot5.setImageDrawable(getActivity().getDrawable(R.drawable.black_dot));
        if (mSixDigits) {
            mDot4.setVisibility(View.VISIBLE);
            mDot5.setVisibility(View.VISIBLE);
        } else {
            mDot4.setVisibility(View.GONE);
            mDot5.setVisibility(View.GONE);
        }
    }

    private void updatePinTexts() {
        if (mConfirming) {
            mHeading.setText(R.string.pin_heading_confirm);
            mMessage.setText(R.string.pin_confirm_message);
        } else {
            mHeading.setText(R.string.pin_heading_choose);
            mMessage.setText(R.string.pin_choose_message);
        }
    }

    private class PinTypeAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Object getItem(int i) {
            return new Integer(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            // Create a LinearLayout to contain image an text.
            if (view == null) {
                float fact = getResources().getDisplayMetrics().density;
                view = new TextView(getActivity());
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,ActionBar.LayoutParams.WRAP_CONTENT);
                view.setPadding((int)(16 * fact),(int)(8 * fact),(int)(16 * fact),(int)(8*fact));
                view.setLayoutParams(lp);

                ((TextView)view).setBackgroundColor(Color.parseColor("#00ffffff"));
                ((TextView)view).setTextColor(getResources().getColor(R.color.darkText));
                ((TextView)view).setTextSize(TypedValue.COMPLEX_UNIT_SP,10);
            }

            if (i == 0) {
                ((TextView) view).setText(R.string.pin_type_4);
            } else if (i == 1) {
                ((TextView) view).setText(R.string.pin_type_6);
            } else {
                Log.w(TAG,"unknown pin type");
            }

            return view;
        }

    }

}
