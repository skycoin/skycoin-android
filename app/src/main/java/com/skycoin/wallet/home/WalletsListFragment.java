package com.skycoin.wallet.home;


import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skycoin.wallet.R;
import com.skycoin.wallet.nodebackend.BalanceRes;
import com.skycoin.wallet.nodebackend.SkycoinService;
import com.skycoin.wallet.wallet.Address;
import com.skycoin.wallet.wallet.NewWalletDialogFragment;
import com.skycoin.wallet.wallet.Wallet;
import com.skycoin.wallet.wallet.WalletManager;
import com.skycoin.wallet.wallet.WalletUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class WalletsListFragment extends Fragment {

    private static final String TAG = WalletsListFragment.class.getName();

    private RecyclerView mWalletRecycler;
    private WalletAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;


    public WalletsListFragment() {
        // Required empty public constructor
    }

    public static String getFragmentTag() {
        return WalletsListFragment.class.getName();
    }

    public static WalletsListFragment newInstance() {
        WalletsListFragment fragment = new WalletsListFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.wallets_list_fragment, container, false);
        mWalletRecycler = v.findViewById(R.id.wallet_list);
        mWalletRecycler.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mWalletRecycler.setLayoutManager(mLayoutManager);


        mAdapter = new WalletAdapter();
        mWalletRecycler.setAdapter(mAdapter);

        return v;
    }

    public void onResume() {
        super.onResume();
        updateWallets();
    }

    // assumes all data is loaded, only updates UI
    public void updateWallets() {
        Log.d(TAG, "updating wallet UI");
        HomeActivity ha = (HomeActivity) getActivity();
        mAdapter.setWallets(ha.getWallets());
        mAdapter.notifyDataSetChanged();
    }


    public class WalletAdapter extends RecyclerView.Adapter<WalletAdapter.ViewHolder> {

        private List<Wallet> mWallets;

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView walletName;
            public TextView walletHours;
            public TextView walletBalance;
            public View bottomDivider;

            public LinearLayout buttonsContainer;
            public TextView newWalletButton;
            public TextView loadWalletButton;

            public ViewHolder(View v) {
                super(v);

                walletName = v.findViewById(R.id.wallet_name);
                walletHours = v.findViewById(R.id.wallet_hours);
                walletBalance = v.findViewById(R.id.wallet_balance);

                buttonsContainer = v.findViewById(R.id.buttons_container);
                newWalletButton = v.findViewById(R.id.new_wallet_button);
                loadWalletButton = v.findViewById(R.id.load_wallet_button);

                bottomDivider = v.findViewById(R.id.bottom_divider);
            }
        }

        public WalletAdapter() {

        }

        public void setWallets(List<Wallet> newList) {
            mWallets = newList;
        }

        @Override
        public WalletAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.wallet_list_cell, parent, false);
            ViewHolder vh = new ViewHolder(v);

            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {

            if (position < getItemCount() - 1) {
                Wallet w = mWallets.get(position);

                holder.buttonsContainer.setVisibility(View.GONE);

                holder.walletName.setText(w.getName());
                holder.walletHours.setText(WalletUtils.formatHoursToSuffix(w.getHours(), true));
                holder.walletBalance.setText(WalletUtils.formatCoinsToSuffix(w.getBalance(), true));

                holder.bottomDivider.setVisibility(View.VISIBLE);

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Wallet w = mWallets.get(position);
                        Log.d(TAG, "clicked wallet " + position + ": " + w.getName());

                        WalletsContainerFragment cfr = (WalletsContainerFragment) getFragmentManager().findFragmentByTag(WalletsContainerFragment.getFragmentTag());
                        if (cfr != null) {
                            List<Wallet> lw = new ArrayList<>();
                            lw.add(w);
                            cfr.updateHeaderInfo(lw, true);
                        }

                        // show addresses for the clicked wallet
                        Fragment listFragment = AddressListFragment.newInstance(w.getId());
                        FragmentTransaction transaction = getFragmentManager().beginTransaction();
                        transaction.setCustomAnimations(R.anim.slide_in_from_right, R.anim.slide_out_to_left,
                                R.anim.slide_in_from_left, R.anim.slide_out_to_right);
                        transaction.replace(R.id.fragment_container, listFragment, AddressListFragment.getFragmentTag());
                        transaction.addToBackStack(null);
                        transaction.commit();
                    }
                });
            } else {
                holder.buttonsContainer.setVisibility(View.VISIBLE);
                holder.newWalletButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "NEW WALLET");
                        DialogFragment walletFragment = NewWalletDialogFragment.newInstance(true);
                        walletFragment.show(getFragmentManager(), null);

                    }
                });
                holder.loadWalletButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "LOAD WALLET");
                        DialogFragment walletFragment = NewWalletDialogFragment.newInstance(false);
                        walletFragment.show(getFragmentManager(), null);

                    }
                });
                holder.bottomDivider.setVisibility(View.GONE);
            }


        }

        @Override
        public int getItemCount() {
            return (mWallets != null ? mWallets.size() : 0) + 1; // last cell is new/load buttons
        }
    }

}
