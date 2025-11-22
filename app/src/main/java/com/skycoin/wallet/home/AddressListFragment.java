package com.skycoin.wallet.home;


import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.skycoin.wallet.BaseActivity;
import com.skycoin.wallet.Bip21Utils;
import com.skycoin.wallet.R;
import com.skycoin.wallet.Utils;
import com.skycoin.wallet.nodebackend.BalanceRes;
import com.skycoin.wallet.nodebackend.NodeUtils;
import com.skycoin.wallet.nodebackend.SkycoinService;
import com.skycoin.wallet.wallet.Address;
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

import static java.net.HttpURLConnection.HTTP_OK;

public class AddressListFragment extends Fragment {

    public static final String ARG_WALLET_ID = "com.skycoin.arg_wallet_id";

    private static final String TAG = AddressListFragment.class.getName();

    private RecyclerView mAddressRecycler;
    private AddressAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private String mWalletId;
    private Wallet mWallet;

    public AddressListFragment() {
        // Required empty public constructor
    }

    public static String getFragmentTag() {
        return AddressListFragment.class.getName();
    }

    public static AddressListFragment newInstance(String id) {
        AddressListFragment fragment = new AddressListFragment();

        Bundle b = new Bundle();
        b.putString(ARG_WALLET_ID, id);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();

        if (savedInstanceState != null) {
            args = savedInstanceState;
        }

        mWalletId = args.getString(ARG_WALLET_ID);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.address_list_fragment, container, false);

        mAddressRecycler = v.findViewById(R.id.address_list);
        mAddressRecycler.setHasFixedSize(true);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mAddressRecycler.setLayoutManager(mLayoutManager);

        mAdapter = new AddressListFragment.AddressAdapter();
        mAddressRecycler.setAdapter(mAdapter);
        ItemTouchHelper mIth = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START) {

                    private Drawable mDeleteIcon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_delete);
                    private int mInWidth = mDeleteIcon.getIntrinsicWidth();
                    private int mInHeight = mDeleteIcon.getIntrinsicHeight();
                    private ColorDrawable mBackground = new ColorDrawable();
                    private int mBackgroundColor = getResources().getColor(R.color.reminder_red);

                    public boolean onMove(RecyclerView recyclerView,
                                          RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        return false;// true if moved, false otherwise
                    }

                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                        // remove from adapter
                        WalletManager.setNumAddresses(getActivity(), mWalletId, mWallet.getAddresses().size() - 1);
                        ((HomeActivity) getActivity()).refreshWallets(true);
                    }

                    @Override
                    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                        if (viewHolder.getAdapterPosition() == mAdapter.getItemCount() - 2) {
                            // allow
                            final int dragFlags = 0;
                            final int swipeFlags = ItemTouchHelper.START;
                            return makeMovementFlags(dragFlags, swipeFlags);
                        }
                        return makeMovementFlags(0, 0);

                    }

                    // Let's draw our delete view
                    public void onChildDraw(Canvas canvas, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
                        View itemView = viewHolder.itemView;
                        int itemHeight = itemView.getBottom() - itemView.getTop();


                        // Draw the red delete background
                        mBackground.setColor(mBackgroundColor);
                        mBackground.setBounds(
                                itemView.getRight() + (int) dX,
                                itemView.getTop(),
                                itemView.getRight(),
                                itemView.getBottom()
                        );
                        mBackground.draw(canvas);

                        // Calculate position of delete icon
                        int iconTop = itemView.getTop() + (itemHeight - mInHeight) / 2;
                        int iconMargin = (itemHeight - mInHeight) / 2;
                        int iconLeft = itemView.getRight() - iconMargin - mInWidth;
                        int iconRight = itemView.getRight() - iconMargin;
                        int iconBottom = iconTop + mInHeight;

                        // Draw the delete icon
                        mDeleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        mDeleteIcon.draw(canvas);

                        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    }

                });
        mIth.attachToRecyclerView(mAddressRecycler);

        return v;
    }

    public void onStart() {
        super.onStart();
        reloadWalletData(); // get wallet from HomeActivity wallet list, then loads individual address balance
    }

    public String getWalletId() {
        return mWalletId;
    }

    public Wallet getWallet() {
        return mWallet;
    }

    public void reloadWalletData() {

        for (Wallet w : ((HomeActivity) getActivity()).getWallets()) {
            if (w.getId().equals(mWalletId)) {
                mWallet = w;
                break;
            }
        }

        mAdapter.setAddresses(mWallet.getAddresses());
        mAdapter.notifyDataSetChanged();

        Retrofit retrofit = NodeUtils.getRetrofit(Utils.getSkycoinUrl(getActivity()));
        if (retrofit == null) {
            ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                    getResources().getString(R.string.error_retrofit),
                    getResources().getString(R.string.ok), null);
            return;
        }

        SkycoinService api = retrofit.create(SkycoinService.class);

        if (mWallet.getAddresses() == null || mWallet.getAddresses().size() == 0) {
            // No addresses in wallet, just skip
            return;
        }

        String addressList = "";
        for (Address ad : mWallet.getAddresses()) {
            addressList += ad.getAddress() + ",";
        }
        addressList = addressList.substring(0, addressList.length() - 1);

        api.getBalances(addressList).enqueue(new Callback<BalanceRes>() {
            @Override
            public void onResponse(Call<BalanceRes> call, Response<BalanceRes> response) {

                if (getActivity() == null || getActivity().isDestroyed()) {
                    // user has backed out, just skip
                    return;
                }

                BalanceRes br = response.body();
                if (response.code() != HTTP_OK || br == null || br.getAddresses() == null) {
                    Log.d(TAG, "faile to load from backed:" + response.code());
                    ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                            getResources().getString(R.string.error_network),
                            getResources().getString(R.string.ok), null);
                } else {

                    Log.d(TAG, "got balance: " + br.getConfirmed().getCoins());

                    for (Address ad : mWallet.getAddresses()) {
                        BalanceRes.BalanceCollection bc = br.getAddresses().get(ad.getAddress());
                        if (bc != null) {
                            ad.setBalance(bc.getConfirmed().getCoins());
                            ad.setHours(bc.getConfirmed().getHours());
                        }
                    }

                    mAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<BalanceRes> call, Throwable t) {
                Log.e(TAG, "service error", t);
                if (getActivity() == null || getActivity().isDestroyed()) {
                    // user has backed out, just skip
                    return;
                }

                ((BaseActivity) getActivity()).showInfoPopup(getResources().getString(R.string.error),
                        getResources().getString(R.string.error_network),
                        getResources().getString(R.string.ok), null);
            }
        });
    }

    public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {

        private List<Address> mAddresses;

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView index;
            public TextView address;
            public TextView hours;
            public TextView balance;

            public LinearLayout buttonsContainer;
            public TextView newAddressButton;

            public ViewHolder(View v) {
                super(v);

                index = v.findViewById(R.id.index);
                address = v.findViewById(R.id.address);
                hours = v.findViewById(R.id.hours);
                balance = v.findViewById(R.id.balance);

                buttonsContainer = v.findViewById(R.id.buttons_container);
                newAddressButton = v.findViewById(R.id.new_address_button);
            }
        }

        public AddressAdapter() {

        }

        public void setAddresses(List<Address> newList) {
            mAddresses = newList;
        }

        @Override
        public AddressAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.address_list_cell, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {

            if (position < getItemCount() - 1) {
                holder.buttonsContainer.setVisibility(View.GONE);
                Address ad = mAddresses.get(position);
                holder.index.setText("" + position);
                holder.address.setText(ad.getAddress());
                holder.hours.setText(WalletUtils.formatHoursToSuffix(ad.getHours(), true));
                holder.balance.setText(WalletUtils.formatCoinsToSuffix(ad.getBalance(), true));

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Address a = mAddresses.get(position);
                        Log.d(TAG, "clicked address " + position + ": " + a.getAddress());

                        showQrCode(a.getAddress());
                    }
                });
            } else {
                holder.buttonsContainer.setVisibility(View.VISIBLE);
                holder.itemView.setOnClickListener(null);
                holder.newAddressButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "new address!");
                        WalletManager.setNumAddresses(getActivity(), mWalletId, mWallet.getAddresses().size() + 1);
                        ((HomeActivity) getActivity()).refreshWallets(true);
                    }
                });
            }

        }

        @Override
        public int getItemCount() {
            return 1 + (mAddresses != null ? mAddresses.size() : 0);
        }
    }

    private void showQrCode(final String code) {
        View v = getLayoutInflater().inflate(R.layout.qr_code_dialog_fragment, null, false);

        TextView heading = v.findViewById(R.id.heading);
        final ImageView qrImage = v.findViewById(R.id.qr_image);
        TextView addressButton = v.findViewById(R.id.address_label);
        final ImageView icon = v.findViewById(R.id.copy_icon);
        addressButton.setText(code);
        View addressContainer = v.findViewById(R.id.address_container);

        addressContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = android.content.ClipData.newPlainText("SKY address", code);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getActivity(), R.string.copied, Toast.LENGTH_SHORT).show();
                icon.setImageDrawable(getResources().getDrawable(R.drawable.valid));
            }
        });

        final ImageView warning = v.findViewById(R.id.warning_image);
        warning.setVisibility(View.GONE);

        final TextView urlText = v.findViewById(R.id.warning_url);

        final EditText coins = v.findViewById(R.id.amount_text);
        final EditText hours = v.findViewById(R.id.hours_text);
        final EditText message = v.findViewById(R.id.message_text);
        final Button updateButton = v.findViewById(R.id.update_qr_button);

        View.OnFocusChangeListener fl = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    String am = coins.getText().toString();
                    String hr = hours.getText().toString();
                    String mes = message.getText().toString();

                    updateQrAndUrl(qrImage, urlText, warning, code, am, hr,null, mes);
                }
            }
        };

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateQrAndUrl(qrImage, urlText, warning,
                        code, coins.getText().toString(), hours.getText().toString(),
                        null, message.getText().toString());
            }
        });

        coins.setOnFocusChangeListener(fl);
        hours.setOnFocusChangeListener(fl);
        message.setOnFocusChangeListener(fl);

        updateQrAndUrl(qrImage, urlText, warning,
                code, coins.getText().toString(), hours.getText().toString(),
                null, message.getText().toString());

        AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
        b.setView(v);
        b.show();

    }

    private void updateQrAndUrl(ImageView qrImage, TextView urlText,
                            ImageView warning,
                            String code, String amount, String hours,
                                String label, String message) {

        if (!TextUtils.isEmpty(amount) && amount.length() > 13) {
            Toast.makeText(getActivity(), R.string.qr_max_amount, Toast.LENGTH_LONG).show();
            warning.setVisibility(View.VISIBLE);
            urlText.setText(R.string.qr_encode_error);
            return;
        }
        if (!TextUtils.isEmpty(hours) && hours.length() > 9) {
            Toast.makeText(getActivity(), R.string.qr_max_hours, Toast.LENGTH_LONG).show();
            warning.setVisibility(View.VISIBLE);
            urlText.setText(R.string.qr_encode_error);
            return;
        }
        if (!TextUtils.isEmpty(message) && message.length() > 32) {
            Toast.makeText(getActivity(), R.string.qr_max_message, Toast.LENGTH_LONG).show();
            warning.setVisibility(View.VISIBLE);
            urlText.setText(R.string.qr_encode_error);
            return;
        }

        try {
            if (!TextUtils.isEmpty(amount)) {
                Float.parseFloat(amount);
            }
            if (!TextUtils.isEmpty(hours)) {
                Long.parseLong(hours);
            }
        } catch (Exception ex) {
            Toast.makeText(getActivity(), R.string.qr_bad_format, Toast.LENGTH_LONG).show();
            warning.setVisibility(View.VISIBLE);
            urlText.setText(R.string.qr_encode_error);
            return;
        }

        warning.setVisibility(View.GONE);

        String url = Bip21Utils.buildSkycoinBip21Url(code, amount, hours, label, message);
        try {
            Bitmap bitmap = textToQrBitmap(url, 256, 256); // enough bits to encode our bytes
            qrImage.setImageBitmap(bitmap);
        } catch (WriterException ex) {
            Log.e(TAG, "could not generate QR image", ex);
            Toast.makeText(getActivity(), R.string.qr_error, Toast.LENGTH_LONG).show();
        }

        urlText.setText(url);
    }

    private Bitmap textToQrBitmap(String value, int width, int height) throws WriterException {
        BitMatrix bitMatrix;
        try {
            bitMatrix = new MultiFormatWriter().encode(
                    value, BarcodeFormat.DATA_MATRIX.QR_CODE,
                    width, height, null);

        } catch (IllegalArgumentException Illegalargumentexception) {

            return null;
        }
        int bitMatrixWidth = bitMatrix.getWidth();
        int bitMatrixHeight = bitMatrix.getHeight();
        int[] pixels = new int[bitMatrixWidth * bitMatrixHeight];

        for (int y = 0; y < bitMatrixHeight; y++) {
            int offset = y * bitMatrixWidth;

            for (int x = 0; x < bitMatrixWidth; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ?
                        getResources().getColor(R.color.qrCodeBlack) : getResources().getColor(R.color.qrCodeWhite);
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(bitMatrixWidth, bitMatrixHeight, Bitmap.Config.ARGB_4444);
        bitmap.setPixels(pixels, 0, bitMatrixWidth, 0, 0, bitMatrixWidth, bitMatrixHeight);

        return bitmap;
    }


}
