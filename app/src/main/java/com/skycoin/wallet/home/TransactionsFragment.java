package com.skycoin.wallet.home;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.skycoin.wallet.BaseActivity;
import com.skycoin.wallet.Bip21Utils;
import com.skycoin.wallet.R;
import com.skycoin.wallet.SkycoinApplication;
import com.skycoin.wallet.Utils;
import com.skycoin.wallet.nodebackend.NodeUtils;
import com.skycoin.wallet.nodebackend.SkycoinService;
import com.skycoin.wallet.nodebackend.TxHistoryResOLD;
import com.skycoin.wallet.nodebackend.TxHistoryRes;
import com.skycoin.wallet.preferences.PreferenceStore;
import com.skycoin.wallet.send.SendDialogFragment;
import com.skycoin.wallet.wallet.Address;
import com.skycoin.wallet.wallet.Wallet;
import com.skycoin.wallet.wallet.WalletUtils;

import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import static android.view.View.GONE;
import static java.net.HttpURLConnection.HTTP_OK;

public class TransactionsFragment extends Fragment {

    private static final String TAG = TransactionsFragment.class.getName();
    private RecyclerView mList;

    private List<String> mAddrList = new ArrayList<>(); // store away the last loaded addresses for lazy access

    public TransactionsFragment() {
        // Required empty public constructor
    }

    public static String getFragmentTag() {
        return TransactionsFragment.class.getName();
    }

    public static TransactionsFragment newInstance() {
        TransactionsFragment fragment = new TransactionsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.transactions_fragment, container, false);

        mList = v.findViewById(R.id.transactions_list);
        mList.setAdapter(new TransactionsAdapter());
        mList.setLayoutManager(new LinearLayoutManager(getActivity()));

        ImageView refresh = v.findViewById(R.id.refresh_button);
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadTransactions();
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTransactions();
    }

    private void loadTransactions() {

        /*HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
*/

        Retrofit retrofit = NodeUtils.getRetrofit(Utils.getSkycoinUrl(getActivity()));
        if (retrofit == null) {
            ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                    getResources().getString(R.string.error_retrofit),
                    getResources().getString(R.string.ok), null);
            return;
        }

        final SkycoinService api = retrofit.create(SkycoinService.class);

        mAddrList = getAllAddresses(((HomeActivity) getActivity()).getWallets());
        ((TransactionsAdapter) mList.getAdapter()).setTransactions(new ArrayList<TxHistoryRes>()); // clear current list
        callTxHistory(api);
    }

    private void callTxHistory(final SkycoinService api) {
        Log.d(TAG, "using new v25 tx history");

        if (getActivity() == null || getActivity().isDestroyed()) {
            return;
        }

        if (mAddrList == null || mAddrList.size() == 0) {
            // timed out, user gone
            return;
        }

        String allAddresses = "";
        for (String addr : mAddrList) {
            allAddresses += addr + ",";
        }
        allAddresses = allAddresses.substring(0, allAddresses.length() - 1);
        Log.d(TAG, "addr arg: " + allAddresses);
        ((BaseActivity) getActivity()).showLoadingPopup(null);
        api.getTxHistory(allAddresses, true, 1).enqueue(new Callback<List<TxHistoryRes>>() {
            @Override
            public void onResponse(Call<List<TxHistoryRes>> call, Response<List<TxHistoryRes>> response) {
                if (getActivity() == null || getActivity().isDestroyed()) {
                    return;
                }

                if (response.code() != HTTP_OK || response.body() == null) {
                    Log.d(TAG, "failed to load from backed:" + response.code());
                    ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                            getResources().getString(R.string.error_network),
                            getResources().getString(R.string.ok), null);
                } else {
                    Log.d(TAG, "got txhistory for all addresses has " + response.body().size() + " txs");
                    handleTxHistoryPayload(response.body());
                }
                ((BaseActivity) getActivity()).hideLoadingPopup();
            }

            @Override
            public void onFailure(Call<List<TxHistoryRes>> call, Throwable t) {
                if (getActivity() == null || getActivity().isDestroyed()) {
                    return;
                }

                Log.e(TAG, "error getting txhistory", t);
                ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                        getResources().getString(R.string.error_network),
                        getResources().getString(R.string.ok), null);
                ((BaseActivity) getActivity()).hideLoadingPopup();
            }
        });
    }

    public void handleTxHistoryPayload(List<TxHistoryRes> history) {
        if (history == null || history.size() == 0) {
            // no transactions for these addresses
            mList.getAdapter().notifyDataSetChanged();
            return;
        }

        for (TxHistoryRes tx : history) {
            for (TxHistoryRes.HistInxo in : tx.tx.inputs) {
                if (mAddrList.contains(in.owner)) {
                    tx.isSend = true;
                    break;
                }
            }
        }

        ((TransactionsAdapter) mList.getAdapter()).getTransactions().addAll(history);
        // sort them by time descending
        Collections.sort(((TransactionsAdapter) mList.getAdapter()).getTransactions(), new Comparator<TxHistoryRes>() {
            @Override
            public int compare(TxHistoryRes o1, TxHistoryRes o2) {
                if (o1.time < o2.time) {
                    return 1;
                } else if (o1.time > o2.time) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });

        mList.getAdapter().notifyDataSetChanged();

        Log.d(TAG, "loaded transaction history for address");
    }

    public List<String> getAllAddresses(List<Wallet> wallets) {
        List<String> res = new ArrayList<>();

        for (Wallet w : wallets) {
            for (Address a : w.getAddresses()) {
                res.add(a.getAddress());
            }
        }

        return res;
    }

    public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {

        private List<TxHistoryRes> mTransactions;

        public class ViewHolder extends RecyclerView.ViewHolder {

            ImageView mSendIcon;
            TextView mAmount;
            TextView mHours;
            TextView mFiat;
            TextView mHeading;
            TextView mHeadingSub;
            TextView mAddress;
            TextView mBurnLabel;
            ImageView mBurnIcon;


            public ViewHolder(View v) {
                super(v);
                mSendIcon = v.findViewById(R.id.send_icon);
                mAmount = v.findViewById(R.id.amount_label);
                mHours = v.findViewById(R.id.tx_hours_label);
                mFiat = v.findViewById(R.id.fiat_label);
                mHeading = v.findViewById(R.id.heading);
                mHeadingSub = v.findViewById(R.id.heading_sub);
                mAddress = v.findViewById(R.id.address);
                mBurnLabel = v.findViewById(R.id.burn_label);
                mBurnIcon = v.findViewById(R.id.burn_icon);
            }
        }

        public TransactionsAdapter() {

        }

        public void setTransactions(List<TxHistoryRes> newList) {
            mTransactions = newList;
        }

        public List<TxHistoryRes> getTransactions() {
            return mTransactions;
        }

        @Override
        public TransactionsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tx_history_cell, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(TransactionsAdapter.ViewHolder holder, final int position) {
            final TxHistoryRes tx = mTransactions.get(position);

            final String burn;
            if (tx.isSend) {
                holder.mSendIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_yellow_send));
                holder.mHeading.setText(R.string.tx_sent_sky);
                holder.mBurnIcon.setVisibility(View.VISIBLE);
                holder.mBurnLabel.setVisibility(View.VISIBLE);

                long totalHoursIn = findTxHoursInputed(tx);
                long totalHoursOut = findTxHoursOutputed(tx);
                long diff = totalHoursIn - totalHoursOut;
                burn = WalletUtils.formatHoursToSuffix(diff, true);
                holder.mBurnLabel.setText(burn);
            } else {
                burn = null;
                holder.mSendIcon.setImageDrawable(getResources().getDrawable(R.drawable.recv_blue));
                holder.mHeading.setText(R.string.tx_recv_sky);
                holder.mBurnIcon.setVisibility(View.GONE);
                holder.mBurnLabel.setVisibility(View.GONE);
            }
            final String target = findTargetAddress(tx);
            holder.mAddress.setText(target);
            holder.mHeadingSub.setText(WalletUtils.formatUnixDateToReadable(getActivity(), tx.time * 1000));

            final double amount = findTxAmount(tx, target);
            holder.mAmount.setText(WalletUtils.formatCoinsToSuffix(amount * 1000000, true));
            final long hours = findTxHoursToDst(tx, target);
            holder.mHours.setText(hours +
                    " " + getResources().getString(R.string.hours_name));

            float usd = PreferenceStore.getUsdPrice(getActivity());
            if (usd > 0) {
                NumberFormat df = DecimalFormat.getInstance();
                df.setMaximumFractionDigits(2);
                holder.mFiat.setVisibility(View.VISIBLE);
                holder.mFiat.setText("$" + df.format(usd * amount * (amount < 0 ? -1 : 1)));
            } else {
                holder.mFiat.setVisibility(View.INVISIBLE);
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "clicked tx " + tx.tx.txid);
                    TransactionDetailsFragment df = TransactionDetailsFragment.newInstance(tx, amount, target, hours, burn);
                    df.setCancelable(true);
                    df.show(getFragmentManager(), null);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mTransactions != null ? mTransactions.size() : 0;
        }
    }

    public double findTxAmount(TxHistoryRes tx, String probablyDst) {
        double res = 0;

        for (TxHistoryRes.HistOutxo out : tx.tx.outputs) {
            if (out.dst.equals(probablyDst)) {
                try {
                    double d = Double.parseDouble(out.coins);
                    if (tx.isSend) {
                        res -= d;
                    } else {
                        res += d;
                    }
                } catch (Exception ex) {
                    // bad tx data, not much to do
                    Log.w(TAG, "could not parse sky amount", ex);
                }
            }
        }

        return res;
    }

    public long findTxHoursToDst(TxHistoryRes tx, String probablyDst) {
        long res = 0;

        for (TxHistoryRes.HistOutxo out : tx.tx.outputs) {
            if (out.dst.equals(probablyDst)) {
                try {
                    long l = out.hours;
                    if (tx.isSend) {
                        res -= l;
                    } else {
                        res += l;
                    }
                } catch (Exception ex) {
                    // bad tx data, not much to do
                    Log.w(TAG, "could not parse sky amount", ex);
                }
            }
        }

        return res;
    }

    public long findTxHoursInputed(TxHistoryRes tx) {
        long res = 0;

        for (TxHistoryRes.HistInxo in : tx.tx.inputs) {
            try {
                res += in.calcHours;
            } catch (Exception ex) {
                // bad tx data, not much to do
                Log.w(TAG, "could not parse hours amount", ex);
            }
        }

        return res;
    }

    public long findTxHoursOutputed(TxHistoryRes tx) {
        long res = 0;

        for (TxHistoryRes.HistOutxo out : tx.tx.outputs) {
            try {
                res += out.hours;
            } catch (Exception ex) {
                // bad tx data, not much to do
                Log.w(TAG, "could not parse hours amount", ex);
            }
        }

        return res;
    }

    // Its a bit tricky to decide which one is the 'main' dst otput when we only want to show 1,
    // so we make a best-effort to find the interesting output
    public String findTargetAddress(TxHistoryRes tx) {
        if (tx.isSend) {
            // 1) if a send has an output-dst address not owned by us, we assume it is the main
            // destination (there may also be a change address owned by us, but we dont care)
            for (TxHistoryRes.HistOutxo out : tx.tx.outputs) {
                if (!mAddrList.contains(out.dst)) {
                    return out.dst;
                }
            }

            // 2) otherwise, we have sent from one of our wallets to another of our wallets
            // and we cant really know which is the change address and which is the recv address.
            // However we do a best-effort and try to find an output-address that is not also
            // an input-address since the change address is often the same as one of the
            // input-owned-addresses
            /* grab all input addresses for easy comparison */
            List<String> inputAddrs = new ArrayList<>(tx.tx.inputs.size());
            for (TxHistoryRes.HistInxo in : tx.tx.inputs) {
                inputAddrs.add(in.owner);
            }

            for (TxHistoryRes.HistOutxo out : tx.tx.outputs) {
                if (!inputAddrs.contains(out.dst)) {
                    return out.dst;
                }
            }

            // 3) we've done some weird-ass transaction where the output addresses are also
            // the owners of the inputs. We have probably sent from one address to itself.
            // just grab the first output
            return tx.tx.outputs.get(0).dst;
        } else {
            // check for an output we own
            for (TxHistoryRes.HistOutxo out : tx.tx.outputs) {
                if (mAddrList.contains(out.dst)) {
                    return out.dst;
                }
            }
        }

        // we own no outputs and no inputs, should not happen
        return null;
    }


    // FIXME: store as arguments to fragment instead of params
    public static class TransactionDetailsFragment extends DialogFragment {

        private TxHistoryRes mTx;
        private double mAmount;
        private long mHours;
        private String mDst;
        private String mBurn;

        public void setTx(TxHistoryRes tx) {
            mTx = tx;
        }

        public void setAmount(double a) {
            mAmount = a;
        }

        public void setDst(String dst) {
            mDst = dst;
        }

        public void setHours(long hours) {
            mHours = hours;
        }

        public void setBurn(@Nullable String burn) {
            mBurn = burn;
        }

        static TransactionDetailsFragment newInstance(TxHistoryRes tx, double am, String dst, long hrs, @Nullable String burn) {
            TransactionDetailsFragment f = new TransactionDetailsFragment();
            f.setTx(tx);
            f.setAmount(am);
            f.setDst(dst);
            f.setHours(hrs);
            if (!TextUtils.isEmpty(burn)) {
                f.setBurn(burn);
            }
            return f;
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            View v = inflater.inflate(R.layout.transaction_details_fragment, container, false);

            if (mTx == null) {
                return v; // somethings wrong, dont crash
            }

            TextView txLabel = v.findViewById(R.id.tx_id_label);
            txLabel.setText(mTx.tx.txid);
            txLabel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = android.content.ClipData.newPlainText("SKY address", mTx.tx.txid);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getActivity(), R.string.copied, Toast.LENGTH_SHORT).show();
                }
            });

            TextView heading = v.findViewById(R.id.title);
            ImageView im = v.findViewById(R.id.send_icon);
            if (mTx.isSend) {
                heading.setText(R.string.sent_sky);
                im.setImageDrawable(getResources().getDrawable(R.drawable.ic_yellow_send));
            } else {
                heading.setText(R.string.received_sky);
                im.setImageDrawable(getResources().getDrawable(R.drawable.recv_blue));
            }

            TextView am = v.findViewById(R.id.amount_label);
            am.setText(WalletUtils.formatCoinsToSuffix(mAmount * 1000000, true) +
                    " " + getResources().getString(R.string.currency_short));

            TextView hr = v.findViewById(R.id.hours_label);
            hr.setText(mHours + " " + getResources().getString(R.string.hours_name));

            TextView fiat = v.findViewById(R.id.fiat_label);
            float usd = PreferenceStore.getUsdPrice(getActivity());
            if (usd > 0) {
                NumberFormat df = DecimalFormat.getInstance();
                df.setMaximumFractionDigits(2);
                fiat.setVisibility(View.VISIBLE);
                fiat.setText("$" + df.format(usd * mAmount * (mAmount < 0 ? -1 : 1)));
            } else {
                fiat.setVisibility(View.INVISIBLE);
            }

            LinearLayout bc = v.findViewById(R.id.burn_container);
            if (!TextUtils.isEmpty(mBurn)) {
                bc.setVisibility(View.VISIBLE);
                TextView tv = v.findViewById(R.id.burn_label);
                tv.setText(mBurn);
            } else {
                bc.setVisibility(View.GONE);
            }

            TextView date = v.findViewById(R.id.date_label);
            String time = WalletUtils.formatUnixDateToClockTime(getActivity(), mTx.time * 1000);
            date.setText(time + " " + WalletUtils.formatUnixDateToReadable(getActivity(), mTx.time * 1000));

            ImageView checkMark = v.findViewById(R.id.status_checkbox);
            checkMark.setVisibility(mTx.status.confirmed ? View.VISIBLE : GONE);
            TextView status = v.findViewById(R.id.status_label);
            status.setText(mTx.status.confirmed ? R.string.confirmed : R.string.pending);

            TextView from = v.findViewById(R.id.from_label);
            TextView add = v.findViewById(R.id.address_label);
            if (mTx.tx.inputs.size() > 0) {
                add.setText(mTx.tx.inputs.get(0).owner);
                Wallet w = WalletUtils.findWalletForAddress(((HomeActivity) getActivity()).getWallets(), mTx.tx.inputs.get(0).owner);
                if (w != null) {
                    from.setText(w.getName());
                } else {
                    from.setVisibility(View.GONE);
                }
            } else {
                from.setVisibility(View.GONE);
            }

            TextView to = v.findViewById(R.id.to_label);
            TextView toadd = v.findViewById(R.id.to_address_label);
            toadd.setText(mDst);
            Wallet w = WalletUtils.findWalletForAddress(((HomeActivity) getActivity()).getWallets(), mDst);
            if (w != null) {
                to.setText(w.getName());
            } else {
                to.setVisibility(View.GONE);
            }

            TextView note = v.findViewById(R.id.notes_label);
            String ns = ((SkycoinApplication) getActivity().getApplication()).getDb().getNoteForTx(mTx.tx.txid);
            note.setText(ns);

            Button sab = v.findViewById(R.id.send_again_button);
            if (mTx.isSend) {
                sab.setVisibility(View.VISIBLE);
                sab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG,"clicked SEND button");
                        Bip21Utils.Bip21Data req = new Bip21Utils.Bip21Data();
                        req.scheme = Bip21Utils.BIP21_SCHEME_ID;
                        req.address = mDst;
                        DialogFragment sendFragment = SendDialogFragment.newInstanceWithRequest(req);
                        sendFragment.show(getFragmentManager(), null);
                    }
                });
            } else {
                sab.setVisibility(View.GONE);
            }


            return v;
        }
    }


}
