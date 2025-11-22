package com.skycoin.wallet.send;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.skycoin.wallet.BaseActivity;
import com.skycoin.wallet.R;
import com.skycoin.wallet.Utils;
import com.skycoin.wallet.nodebackend.NodeUtils;
import com.skycoin.wallet.nodebackend.SkycoinService;
import com.skycoin.wallet.nodebackend.Utxo;
import com.skycoin.wallet.nodebackend.UtxoRes;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static java.net.HttpURLConnection.HTTP_OK;

public class UtxoSelectionFragment extends Fragment {

    private static final String TAG = UtxoSelectionFragment.class.getName();

    private TextView mPoolBalance;
    private TextView mPoolHours;
    private TextView mPoolBurn;
    private TextView mPoolHoursToSend;

    private Button mNextButton;
    private Button mBackButton;

    private RecyclerView mUtxoList;

    public UtxoSelectionFragment() {

    }

    public static UtxoSelectionFragment newInstance() {
        return new UtxoSelectionFragment();
    }


    public void update() {
        calcPool();
        loadUtxos(((AdvancedSendActivity) getActivity()).mSelectedAddrs);
    }

    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);

    }

    /*public void onAttach(Context context) {
        super.onAttach(context);

        loadUtxos(((AdvancedSendActivity)getActivity()).mSelectedAddrs);
    }*/

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.utxo_select_send_fragment, container, false);

        mUtxoList = v.findViewById(R.id.utxo_list);
        mUtxoList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mUtxoList.setAdapter(new UtxoAdapter());

        mPoolBalance = v.findViewById(R.id.pool_balance_text);
        mPoolHours = v.findViewById(R.id.pool_hours_text);
        mPoolBurn = v.findViewById(R.id.pool_hours_burned_text);
        mPoolHoursToSend = v.findViewById(R.id.pool_hours_remaining_text);

        mNextButton = v.findViewById(R.id.next_button);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveToNextFragment();
            }
        });

        mBackButton = v.findViewById(R.id.back_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveToPrevFragment();
            }
        });

        ((BaseActivity)getActivity()).showLoadingPopup(null);

        NodeUtils.getNodeRules(getActivity(), Utils.getSkycoinUrl(getActivity()), new NodeUtils.NodeRulesCallback() {
            @Override
            public void onNodeRules(boolean success, @Nullable Throwable error, int burnFactor, int maxDecimals) {
                if (getActivity() == null || getActivity().isDestroyed()) {
                    return;
                }
                ((BaseActivity)getActivity()).hideLoadingPopup();
                if (!success) {
                    ((BaseActivity)getActivity()).showInfoPopup(getResources().getString(R.string.error),
                            getResources().getString(R.string.error_load_node_rules_warning),
                            getResources().getString(R.string.ok), null);
                }
                ((AdvancedSendActivity) getActivity()).mBurnFactor = burnFactor;
                ((AdvancedSendActivity) getActivity()).mMaxDecimals = maxDecimals;
                calcPool();
            }
        });

        return v;
    }

    private void moveToNextFragment() {
        ((AdvancedSendActivity) getActivity()).userCompletedStepTwo();
    }

    private void moveToPrevFragment() {
        ((UtxoAdapter) mUtxoList.getAdapter()).setUtxos(null);
        mUtxoList.getAdapter().notifyDataSetChanged();
        ((AdvancedSendActivity) getActivity()).userBackedToStepOne();
    }

    public void loadUtxos(final List<String> list) {

        if (list == null || list.size() == 0) {
            return;
        }

        Retrofit retrofit = NodeUtils.getRetrofit(Utils.getSkycoinUrl(getActivity()));
        if (retrofit == null) {
            ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                    getResources().getString(R.string.error_retrofit),
                    getResources().getString(R.string.ok), null);
            return;
        }

        final SkycoinService api = retrofit.create(SkycoinService.class);

        String addressList = "";
        for (String a : list) {
            addressList += a + ",";
        }
        addressList = addressList.substring(0, addressList.length() - 1);

        ((BaseActivity) getActivity()).showLoadingPopup(null);
        api.getUtxos(addressList).enqueue(new Callback<UtxoRes>() {
            @Override
            public void onResponse(Call<UtxoRes> call, Response<UtxoRes> response) {
                ((BaseActivity) getActivity()).hideLoadingPopup();
                if (response.code() != HTTP_OK || response.body() == null
                        || response.body().getUtxoList() == null) {
                    Log.d(TAG, "network error");
                    ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                            getResources().getString(R.string.error_network),
                            getResources().getString(R.string.ok), null);
                    return;
                }

                if (response.body().getUtxoList().size() == 0) {
                    Log.d(TAG, "wallet has no utxos to spend");
                    return;
                }
                ((UtxoAdapter) mUtxoList.getAdapter()).setUtxos(response.body().getUtxoList());
                mUtxoList.getAdapter().notifyDataSetChanged();

            }

            @Override
            public void onFailure(Call<UtxoRes> call, Throwable t) {
                ((BaseActivity) getActivity()).hideLoadingPopup();
                Log.d(TAG, "network error");
                ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                        getResources().getString(R.string.error_network),
                        getResources().getString(R.string.ok), null);
            }
        });
    }

    private void checkButtonStatus() {
        AdvancedSendActivity adv = (AdvancedSendActivity) getActivity();
        if (adv != null && adv.mSelectedUtxos.size() > 0) {
            mNextButton.setEnabled(true);
        } else {
            mNextButton.setEnabled(false);
        }
    }

    private void calcPool() {
        double bal = 0;
        long hours = 0;
        long burn = 0;
        long remains = 0;
        for (Utxo ux : ((AdvancedSendActivity) getActivity()).mSelectedUtxos) {
            bal += Double.parseDouble(ux.getCoins());
            hours += ux.getCalculatedHours();
        }

        burn = (long) Math.ceil(hours / (double) ((AdvancedSendActivity)getActivity()).mBurnFactor);
        remains = hours - burn;

        NumberFormat df = DecimalFormat.getInstance();
        df.setMaximumFractionDigits(((AdvancedSendActivity)getActivity()).mMaxDecimals);
        mPoolBalance.setText("" + df.format(bal));
        mPoolHours.setText("" + hours);
        mPoolBurn.setText("" + burn);
        mPoolHoursToSend.setText("" + remains);

        checkButtonStatus();
    }

    public class UtxoAdapter extends RecyclerView.Adapter<UtxoAdapter.ViewHolder> {

        private List<Utxo> mUtxos;

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView address;
            public TextView hours;
            public TextView balance;
            public View bottomDiv;

            public ViewHolder(View v) {
                super(v);
                address = v.findViewById(R.id.utxo_id);
                hours = v.findViewById(R.id.hours);
                balance = v.findViewById(R.id.amount);
                bottomDiv = v.findViewById(R.id.bottom_divider);
            }
        }

        public UtxoAdapter() {

        }

        public void setUtxos(List<Utxo> newList) {
            mUtxos = newList;
        }

        @Override
        public UtxoAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.utxo_selection_cell, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {

            final Utxo ux = mUtxos.get(position);
            holder.address.setText(ux.getAddress());
            holder.hours.setText("" + ux.getCalculatedHours());
            holder.balance.setText("" + ux.getCoins());

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (((AdvancedSendActivity) getActivity()).mSelectedUtxos.contains(ux)) {
                        ((AdvancedSendActivity) getActivity()).mSelectedUtxos.remove(ux);
                    } else {
                        ((AdvancedSendActivity) getActivity()).mSelectedUtxos.add(ux);
                    }
                    calcPool();
                    mUtxoList.getAdapter().notifyDataSetChanged(); // update selected backgrounds
                }
            });
            if (((AdvancedSendActivity) getActivity()).mSelectedUtxos.contains(ux)) {
                holder.address.setBackground(null);
                if (getItemCount() == 1) {
                    holder.itemView.setBackground(getResources().getDrawable(R.drawable.grey_rounded_rect));
                } else {
                    if (position == 0) {
                        holder.itemView.setBackground(getResources().getDrawable(R.drawable.grey_rounded_top));
                    } else if (position == getItemCount() - 1) {
                        holder.itemView.setBackground(getResources().getDrawable(R.drawable.grey_rounded_bottom));
                    } else {
                        holder.itemView.setBackgroundColor(getResources().getColor(R.color.blueTransparent));
                    }
                }
            } else {
                holder.itemView.setBackground(null);

                if (position == 0) {
                    holder.address.setBackground(getResources().getDrawable(R.drawable.grey_rounded_top));
                } else {
                    holder.address.setBackgroundColor(getResources().getColor(R.color.blueTransparent));
                }

            }

            if (position == mUtxos.size() - 1) {
                holder.bottomDiv.setVisibility(View.GONE);
            } else {
                holder.bottomDiv.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return mUtxos != null ? mUtxos.size() : 0;
        }
    }

}