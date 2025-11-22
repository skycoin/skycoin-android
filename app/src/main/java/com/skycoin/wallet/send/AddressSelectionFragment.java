package com.skycoin.wallet.send;

import android.os.Bundle;
import android.provider.Telephony;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.skycoin.wallet.BaseActivity;
import com.skycoin.wallet.R;
import com.skycoin.wallet.Utils;
import com.skycoin.wallet.home.WalletsListFragment;
import com.skycoin.wallet.nodebackend.BalanceRes;
import com.skycoin.wallet.nodebackend.NodeUtils;
import com.skycoin.wallet.nodebackend.SkycoinService;
import com.skycoin.wallet.wallet.Address;
import com.skycoin.wallet.wallet.Wallet;
import com.skycoin.wallet.wallet.WalletManager;
import com.skycoin.wallet.wallet.WalletUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static java.net.HttpURLConnection.HTTP_OK;

public class AddressSelectionFragment extends Fragment {

    private static final String TAG = AddressSelectionFragment.class.getName();

    private Spinner mWalletSelector;
    private RecyclerView mAddressList;

    private TextView mPoolBalance;
    private TextView mPoolHours;

    private Button mNextButton;
    private Button mCancelButton;

    public AddressSelectionFragment() {

    }

    public void update() {
        selectWallet(mWalletSelector.getSelectedItemPosition());
    }

    public static AddressSelectionFragment newInstance() {
        return new AddressSelectionFragment();
    }

    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.address_select_send_fragment, container, false);

        mWalletSelector = v.findViewById(R.id.wallet_selector);
        mWalletSelector.setAdapter(new WalletDropDownAdapter());

        mWalletSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectWallet(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mAddressList = v.findViewById(R.id.address_list);
        mAddressList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAddressList.setAdapter(new AddressAdapter());
        mWalletSelector.setSelection(0);

        mPoolBalance = v.findViewById(R.id.pool_balance_text);
        mPoolHours = v.findViewById(R.id.pool_hours_text);

        mNextButton = v.findViewById(R.id.next_button);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveToNextFragment();
            }
        });

        mCancelButton = v.findViewById(R.id.back_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });


        calcPool();

        checkButtonStatus();

        return v;
    }

    private void checkButtonStatus() {
        AdvancedSendActivity adv = (AdvancedSendActivity) getActivity();
        if (adv != null && adv.mSelectedAddrs.size() > 0) {
            mNextButton.setEnabled(true);
        } else {
            mNextButton.setEnabled(false);
        }
    }

    private void moveToNextFragment() {
        ((AdvancedSendActivity)getActivity()).userCompletedStepOne();
    }

    private void calcPool() {
        double bal = 0;
        long hours = 0;

        if (getActivity() == null || getActivity().isDestroyed()) {
            return;
        }

        List<Wallet> localList = ((AdvancedSendActivity) getActivity()).getWallets();
        if (localList == null ||localList.size() <= mWalletSelector.getSelectedItemPosition()) {
            // lost state... bail out gracefully
            getActivity().finish();
            return;
        }

        Wallet w = ((AdvancedSendActivity) getActivity()).getWallets().get(mWalletSelector.getSelectedItemPosition());
        for (String addr : ((AdvancedSendActivity)getActivity()).mSelectedAddrs) {
            for (Address wAddr : w.getAddresses()) {
                if (addr.equals(wAddr.getAddress())) {
                    bal += wAddr.getBalance();
                    hours += wAddr.getHours();
                }
            }
        }
        mPoolBalance.setText(WalletUtils.formatCoinsToSuffix(bal, true) + " " + getResources().getString(R.string.currency_short));
        mPoolHours.setText(WalletUtils.formatHoursToSuffix(hours, true) + " " + getResources().getString(R.string.hours_name));
        checkButtonStatus();
    }

    private void selectWallet(int position) {
        final AdvancedSendActivity act = (AdvancedSendActivity) getActivity();
        act.mSelectedWallet = act.getWallets().get(position);
        ((AddressAdapter) mAddressList.getAdapter()).setAddresses(new ArrayList<Address>(act.mSelectedWallet.getAddresses()));
        mAddressList.getAdapter().notifyDataSetChanged();

        act.mSelectedAddrs.clear();
        calcPool();

        // address list is now updated but have no/old balances so we reload those from backend
        Retrofit retrofit = NodeUtils.getRetrofit(Utils.getSkycoinUrl(getActivity()));
        if (retrofit == null) {
            ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                    getResources().getString(R.string.error_retrofit),
                    getResources().getString(R.string.ok), null);
            return;
        }

        SkycoinService api = retrofit.create(SkycoinService.class);

        String addressList = "";
        for (Address ad : act.mSelectedWallet.getAddresses()) {
            addressList += ad.getAddress() + ",";
        }
        if (TextUtils.isEmpty(addressList)) {
            mAddressList.getAdapter().notifyDataSetChanged();
            return;
        }
        addressList = addressList.substring(0, addressList.length() - 1);

        ((BaseActivity) getActivity()).showLoadingPopup(null);
        api.getBalances(addressList).enqueue(new Callback<BalanceRes>() {
            @Override
            public void onResponse(Call<BalanceRes> call, Response<BalanceRes> response) {
                ((BaseActivity) getActivity()).hideLoadingPopup();
                BalanceRes br = response.body();
                if (response.code() != HTTP_OK || br == null || br.getAddresses() == null) {
                    Log.d(TAG, "failed to load from backed:" + response.code());
                    ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                            getResources().getString(R.string.error_network),
                            getResources().getString(R.string.ok), null);
                } else {
                    Log.d(TAG, "got full balance: " + br.getConfirmed().getCoins());
                    for (Address ad : act.mSelectedWallet.getAddresses()) {
                        BalanceRes.BalanceCollection bc = br.getAddresses().get(ad.getAddress());
                        if (bc != null) {
                            ad.setBalance(bc.getConfirmed().getCoins());
                            ad.setHours(bc.getConfirmed().getHours());
                            if (ad.getBalance() == 0) {
                                ((AddressAdapter) mAddressList.getAdapter()).getAddresses().remove(ad);
                            }
                        }
                    }

                    mAddressList.getAdapter().notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<BalanceRes> call, Throwable t) {
                ((BaseActivity) getActivity()).hideLoadingPopup();
                ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                        getResources().getString(R.string.error_network),
                        getResources().getString(R.string.ok), null);
                Log.e(TAG, "service error", t);
            }
        });

    }

    private class WalletDropDownAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            AdvancedSendActivity adv = (AdvancedSendActivity) getActivity();
            return adv != null && adv.getWallets() != null ? adv.getWallets().size() : 0;
        }

        @Override
        public Object getItem(int i) {
            AdvancedSendActivity adv = (AdvancedSendActivity) getActivity();
            return adv != null && adv.getWallets() != null ? adv.getWallets().get(i) : null;
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

    public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {

        private List<Address> mAddresses;

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView address;
            public TextView hours;
            public TextView balance;
            public View bottomDiv;

            public ViewHolder(View v) {
                super(v);
                address = v.findViewById(R.id.address);
                hours = v.findViewById(R.id.hours);
                balance = v.findViewById(R.id.balance);
                bottomDiv = v.findViewById(R.id.bottom_divider);
            }
        }

        public AddressAdapter() {

        }

        public List<Address> getAddresses() {
            return mAddresses;
        }

        public void setAddresses(List<Address> newList) {
            mAddresses = newList;
        }

        @Override
        public AddressAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.address_selection_cell, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {

            Address ad = mAddresses.get(position);
            holder.address.setText(ad.getAddress());
            holder.hours.setText(WalletUtils.formatHoursToSuffix(ad.getHours(), true) + " " + getResources().getString(R.string.hours_name));
            holder.balance.setText(WalletUtils.formatCoinsToSuffix(ad.getBalance(), true) + " " + getResources().getString(R.string.currency_short));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Address a = mAddresses.get(position);
                    String as = a.getAddress();
                    if (((AdvancedSendActivity)getActivity()).mSelectedAddrs.contains(as)) {
                        ((AdvancedSendActivity)getActivity()).mSelectedAddrs.remove(as);
                    } else {
                        ((AdvancedSendActivity)getActivity()).mSelectedAddrs.add(as);
                    }
                    calcPool();
                    mAddressList.getAdapter().notifyDataSetChanged(); // update selected backgrounds
                }
            });
            Address a = mAddresses.get(position);
            String as = a.getAddress();
            if (((AdvancedSendActivity)getActivity()).mSelectedAddrs.contains(as)) {
                if (getItemCount() == 1) {
                    holder.itemView.setBackground(getResources().getDrawable(R.drawable.grey_rounded_rect));
                } else {
                    if (position == 0) {
                        holder.itemView.setBackground(getResources().getDrawable(R.drawable.grey_rounded_top));
                    } else if (position == getItemCount() -1) {
                        holder.itemView.setBackground(getResources().getDrawable(R.drawable.grey_rounded_bottom));
                    } else {
                        holder.itemView.setBackgroundColor(getResources().getColor(R.color.blueTransparent));
                    }
                }
            } else {
                holder.itemView.setBackground(null);
            }

            if (position == mAddresses.size() -1) {
                holder.bottomDiv.setVisibility(View.GONE);
            } else {
                holder.bottomDiv.setVisibility(View.VISIBLE);
            }

        }

        @Override
        public int getItemCount() {
            return mAddresses != null ? mAddresses.size() : 0;
        }
    }

}
