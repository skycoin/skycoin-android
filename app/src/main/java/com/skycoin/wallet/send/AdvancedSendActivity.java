package com.skycoin.wallet.send;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skycoin.wallet.BaseActivity;
import com.skycoin.wallet.R;
import com.skycoin.wallet.nodebackend.Utxo;
import com.skycoin.wallet.onboarding.NonSwipeableViewPager;
import com.skycoin.wallet.wallet.Wallet;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AdvancedSendActivity extends BaseActivity {

    private static final String TAG = AdvancedSendActivity.class.getName();

    public static final String ARG_WALLET_LIST = "com.skycoin.arg.wallet_list";
    public static final String ARG_DST_ADDRESS = "com.skycoin.arg.dst_address";

    private NonSwipeableViewPager mPager;
    private SendFragmentAdapter mAdapter;

    protected List<String> mSelectedAddrs = new ArrayList<>();
    protected List<Utxo> mSelectedUtxos = new ArrayList<>();

    private List<Wallet> mAllWallets;
    protected Wallet mSelectedWallet;
    protected String mDstAddress;

    public int mBurnFactor = 2; // avoid divide by zero if not loaded
    public int mMaxDecimals = 3;

    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.advanced_send_activity);

        Gson gson = new Gson();
        Type listType = new TypeToken<List<Wallet>>() {}.getType();
        if (savedState != null) {
            String wJson = savedState.getString(ARG_WALLET_LIST);
            mAllWallets = gson.fromJson(wJson, listType);
            mDstAddress = savedState.getString(ARG_DST_ADDRESS);
        } else {
            String wJson = getIntent().getStringExtra(ARG_WALLET_LIST);
            mAllWallets = gson.fromJson(wJson, listType);
            mDstAddress = getIntent().getStringExtra(ARG_DST_ADDRESS);
        }

        mPager = findViewById(R.id.send_pager);
        mAdapter = new SendFragmentAdapter(getSupportFragmentManager(), mDstAddress);
        mPager.setAdapter(mAdapter);
        mPager.setPagingEnabled(false); // user cant scroll manually
        mPager.setOffscreenPageLimit(3);
    }

    public void onSaveInstanceState(Bundle state) {
        Gson gson = new Gson();
        state.putString(ARG_WALLET_LIST, gson.toJson(mAllWallets));
        super.onSaveInstanceState(state);
    }

    public List<Wallet> getWallets() {
        return mAllWallets;
    }

    // we dont need pin here, it is explicitly asked if user tries to send
    public boolean shouldRequirePin() {
        return false;
    }

    public void userCompletedStepOne() {
        mSelectedUtxos.clear();
        mPager.setCurrentItem(1, true);
        UtxoSelectionFragment frag = (UtxoSelectionFragment) mPager.getAdapter().instantiateItem(mPager, 1);
        frag.update();
    }
    public void userBackedToStepOne() {
        mPager.setCurrentItem(0, true);
        AddressSelectionFragment frag = (AddressSelectionFragment) mPager.getAdapter().instantiateItem(mPager, 0);
        frag.update();
    }
    public void userCompletedStepTwo() {
        mPager.setCurrentItem(2, true);
        ConfigureOutputsFragment frag = (ConfigureOutputsFragment) mPager.getAdapter().instantiateItem(mPager, 2);
        frag.update();
    }
    public void userBackedToStepTwo() {
        mPager.setCurrentItem(1, true);
        UtxoSelectionFragment frag = (UtxoSelectionFragment) mPager.getAdapter().instantiateItem(mPager, 1);
        frag.update();
    }

    public static class SendFragmentAdapter extends FragmentStatePagerAdapter {

        protected String mDstAddress;

        public SendFragmentAdapter(FragmentManager fm, @Nullable String addr) {
            super(fm);
            mDstAddress = addr;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return AddressSelectionFragment.newInstance();
                case 1: return UtxoSelectionFragment.newInstance();
                case 2: return ConfigureOutputsFragment.newInstance(mDstAddress);
            }

            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }
    }


}
