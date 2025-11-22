package com.skycoin.wallet.wallet;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.skycoin.wallet.BaseActivity;
import com.skycoin.wallet.R;
import com.skycoin.wallet.Utils;
import com.skycoin.wallet.encryption.CryptoException;
import com.skycoin.wallet.encryption.EncryptionManager;
import com.skycoin.wallet.nodebackend.BalanceRes;
import com.skycoin.wallet.nodebackend.NodeUtils;
import com.skycoin.wallet.nodebackend.RawTx;
import com.skycoin.wallet.nodebackend.SkycoinService;
import com.skycoin.wallet.nodebackend.Utxo;
import com.skycoin.wallet.nodebackend.UtxoRes;
import com.skycoin.wallet.preferences.PreferenceStore;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import mobile.Mobile;
//import okhttp3.OkHttpClient;
//import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static java.net.HttpURLConnection.HTTP_OK;

public class WalletManager {

    public static final int ERROR_NO_ERROR = 0;
    public static final int ERROR_SEND_FAILED = 1;

    public interface SendCallback {
        public void sendComplete(String res, Exception error, int code);
    }

    public interface CreateCallback {
        public void createComplete(boolean success, Exception error);
    }

    private static final String TAG = WalletManager.class.getName();

    public static final String USE_STRING_ENCODING = "UTF-8";
    private static final String WALLET_PREF_KEY_DELIMITER = ",";
    private static final int DEFAULT_NUM_ADDRESSES = 1;

    private static final int MAX_SCAN_ADDRESSES = 100;

    public static final String DEFAULT_NAME = "Unnamed wallet";

    public static String generateNewSeed() throws Exception {
        String newSeed = Mobile.newWordSeed();
        return newSeed;
    }

    /**
     * Generates addresses with private keys etc, based on the given seed.
     *
     * @param seed
     * @param numAddresses
     * @return
     * @throws Exception
     */
    public static List<Address> getAddresses(@NonNull String seed, int numAddresses) throws Exception {
        String addressJson = Mobile.getAddresses(seed, numAddresses);
        Log.d(TAG, "generated " + numAddresses + " addresses: " + addressJson);

        Gson gson = new Gson();
        Type listType = new TypeToken<List<Address>>() {
        }.getType();
        List<Address> addresses = gson.fromJson(addressJson, listType);

        return addresses;
    }

    /**
     * deletes a wallet permanently
     */
    public static void deleteWallet(final @NonNull Context ctx, final @NonNull String walletId) {
        List<String> allKeys = getAllWalletPropKeys(ctx);
        Log.d(TAG,"deleting wallet id "+walletId);
        boolean found = false;
        // make a new list without the key we want to remove
        String newKeyString = "";
        for (String s : allKeys) {
            if (s.equals(walletId)) {
                Log.d(TAG, "found wallet id " + walletId + ", skip it so it is removed");
                found = true;
                continue;
            }
            newKeyString += s + WALLET_PREF_KEY_DELIMITER;
        }

        if (!found) {
            Log.d(TAG,"could not find wallet "+walletId+" to delete, skipping");
            return;
        }

        if (newKeyString.endsWith(WALLET_PREF_KEY_DELIMITER)) {
            newKeyString = newKeyString.substring(0, newKeyString.length() -1);
        }

        Log.d(TAG, "new wallet key list after delete:" + newKeyString);
        PreferenceStore.setWalletKeyList(ctx, newKeyString);

        // Now delete the actual encrypted wallet info under the walletId we just removed
        PreferenceStore.deleteWalletData(ctx, walletId);
    }

    /**
     * Takes the plaintext seed and a name. Encrypts the seed and saves it with the name.
     *
     * @param ctx
     * @param seed
     * @param name
     */
    public static void saveSeed(final @NonNull Context ctx, @NonNull String seed, @Nullable String name,
                                final CreateCallback callback) {

        if (TextUtils.isEmpty(name)) {
            name = DEFAULT_NAME;
        }

        try {
            String encryptedSeedAndIV = EncryptionManager.encrypt(seed.getBytes(USE_STRING_ENCODING));
            String id = saveNewWallet(ctx, encryptedSeedAndIV, name);
            // scan up to 100 addresses and detect funds
            findHighestAddressNumWithFunds(ctx, id, MAX_SCAN_ADDRESSES, callback);
        } catch (Exception ex) {
            Log.e(TAG, "could not encrypt seed", ex);
            // TODO: popup and warn
        }
    }

    /**
     * Returns the list of wallet prop keys. A "prop key" is the key under which the
     * encrypted seed,iv,name,addrcount is saved in the PreferenceStore
     *
     * @param ctx
     * @return
     */
    private static List<String> getAllWalletPropKeys(final @NonNull Context ctx) {
        String str = PreferenceStore.getWalletKeyList(ctx);
        if (TextUtils.isEmpty(str)) {
            return new ArrayList<>();
        }
        String[] props = str.split(",");
        List<String> ret = new ArrayList<>();
        ret = Arrays.asList(props);
        return ret;
    }

    private static String saveNewWallet(final @NonNull Context ctx, @NonNull String encryptedSeedAndIV, @NonNull String name) throws Exception {
        List<String> allKeys = getAllWalletPropKeys(ctx);
        // get a new key to save the wallet seed under
        String newId = UUID.randomUUID().toString();

        // cant add to the list from AsList so make new and copy
        List<String> newKeys = new ArrayList<>();
        newKeys.addAll(allKeys);
        Log.d(TAG, "got " + newKeys.size() + " current keys: " + newKeys);
        String newKeyString = "";
        if (newKeys.size() > 0) { // special case: first time
            for (String s : newKeys) {
                newKeyString += s + WALLET_PREF_KEY_DELIMITER;
            }
        }
        newKeyString += newId; // add the new at the end

        Log.d(TAG, "new wallet key list:" + newKeyString);
        PreferenceStore.setWalletKeyList(ctx, newKeyString);
        // Wallet id-key is now saved in global wallet-id-list

        // Now we save the actual encrypted wallet info under
        // the prefs key we just generated
        String nameBase64 = Base64.encodeToString(name.getBytes(USE_STRING_ENCODING), Base64.NO_WRAP);
        String finalDataToStore = encryptedSeedAndIV + WALLET_PREF_KEY_DELIMITER + nameBase64 + WALLET_PREF_KEY_DELIMITER + DEFAULT_NUM_ADDRESSES;
        Log.d(TAG, "final payload: " + finalDataToStore);
        PreferenceStore.storeWalletData(ctx, finalDataToStore, newId);

        return newId;
    }

    // returns an array: ["encrypted-data-base64,IV-base64"],["wallet name-cleartext"]
    public static String[] getSeedAndNameForWallet(final @NonNull Context ctx, String id) {
        String encodedAndEncryptedData = PreferenceStore.getWalletData(ctx, id);

        String[] parts = encodedAndEncryptedData.split(WALLET_PREF_KEY_DELIMITER);// [encrypted-seed],[IV],[name],[numAddresses] (all base64 except numaddr)

        String walletNameClearText = "";
        try {
            walletNameClearText = new String(Base64.decode(parts[2], Base64.NO_WRAP), USE_STRING_ENCODING);
        } catch (UnsupportedEncodingException ex) {
            Log.w(TAG, "could not make string from bytes", ex);
            // just return wallet without name, it is not critical
        }

        String[] pair = new String[]{parts[0] + "," + parts[1], walletNameClearText};
        return pair;
    }

    // num active addresses are not encrypted so this never decrypts the seed
    public static int getNumActiveAddresses(Context ctx, String walletId) {
        String encodedAndEncryptedData = PreferenceStore.getWalletData(ctx, walletId);

        String[] parts = encodedAndEncryptedData.split(WALLET_PREF_KEY_DELIMITER);// [encrypted-seed],[IV],[name],[numAddresses] (all base64 except numaddr)

        int res = DEFAULT_NUM_ADDRESSES;
        try {
            res = Integer.parseInt(parts[3]);
        } catch (Exception ex) {
            Log.d(TAG, "could not parse num active addresses", ex);
        }
        return res;
    }

    /**
     * Returns a wallet with a name and an encrypted seed
     *
     * @param ctx
     * @param id
     * @return
     * @throws UnsupportedEncodingException
     * @throws Exception
     */
    public static Wallet decryptWallet(final @NonNull Context ctx, String id) throws UnsupportedEncodingException, Exception {
        String[] pair = getSeedAndNameForWallet(ctx, id);

        Wallet w = new Wallet();
        w.setId(id);
        w.setName(pair[1]);
        w.setSeed(pair[0]);

        return w;
    }

    public static Wallet loadSingleWallet(Context ctx, String id) {
        Wallet wall = null;
        try {
            wall = decryptWallet(ctx, id);
            fillWalletAddresses(ctx, wall, id);
            Log.d(TAG, "loaded wallet " + wall);
            cleanAddresses(wall.getAddresses());
        } catch (Exception ex) {
            Log.w(TAG, "could not get wallet:", ex);
        }
        return wall;
    }

    // removes the secret key from the address so we dont keep it around in memory
    private static void cleanAddresses(List<Address> addresses) {
        for (Address adr : addresses) {
            adr.setSecret(null);
        }
    }

    /**
     * Fills wallet list with static data i.e. just the addresses.
     * The balances on each address need to be loaded separately
     *
     * @param ctx
     * @return
     */
    public static List<Wallet> getAllWallets(final @NonNull Context ctx) {
        List<String> walletIds = getAllWalletPropKeys(ctx);

        List<Wallet> walletList = new ArrayList<>(walletIds.size());
        for (String id : walletIds) {
            try {
                Wallet wall = decryptWallet(ctx, id);
                fillWalletAddresses(ctx, wall, id);
                cleanAddresses(wall.getAddresses());
                walletList.add(wall);
            } catch (Exception ex) {
                Log.w(TAG, "could not get wallet:", ex);
            }
        }

        return walletList;
    }

    public static boolean doesSeedExist(final Context ctx, final String seed) throws Exception {
        List<String> walletIds = getAllWalletPropKeys(ctx);
        for (String id : walletIds) {
            String[] pair = getSeedAndNameForWallet(ctx, id);
            if (pair != null && pair.length > 0) {
                String existingSeed = pair[0];
                String decryptedSeed = new String(EncryptionManager.decrypt(existingSeed), WalletManager.USE_STRING_ENCODING);
                if (!TextUtils.isEmpty(decryptedSeed) && decryptedSeed.equals(seed)) {
                    return true;
                }
            }
        }


        return false;
    }

    /**
     * Addresses returned contain secret keys, make sure to clean them
     *
     * @param ctx
     * @param wall
     * @param id
     * @throws Exception
     */
    public static void fillWalletAddresses(Context ctx, Wallet wall, String id) throws Exception {
        String[] pairs = getSeedAndNameForWallet(ctx, id);
        String decryptedSeed = new String(EncryptionManager.decrypt(pairs[0]), USE_STRING_ENCODING);
        int num = getNumActiveAddresses(ctx, id);
        String addresses = Mobile.getAddresses(decryptedSeed, num);

        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<Address>>() {
        }.getType();
        List<Address> al = gson.fromJson(addresses, listType);
        Log.d(TAG, "got " + al.size() + " addresses for wallet");
        wall.setAddresses(al);
    }

    public static void setNumAddresses(Context ctx, String id, int num) {
        String encodedAndEncryptedData = PreferenceStore.getWalletData(ctx, id);

        String[] parts = encodedAndEncryptedData.split(WALLET_PREF_KEY_DELIMITER);// [encrypted-seed],[IV],[name],[numAddresses] (all base64 except numaddr)

        String newData = parts[0] + WALLET_PREF_KEY_DELIMITER + parts[1] +
                WALLET_PREF_KEY_DELIMITER + parts[2] + WALLET_PREF_KEY_DELIMITER + num;

        Log.d(TAG, "changing wallet data from:" + encodedAndEncryptedData + " TO:" + newData);
        PreferenceStore.storeWalletData(ctx, newData, id);
    }

    // when saving a new wallet, scan absMax addresses for funds
    public static void findHighestAddressNumWithFunds(final Context ctx, final String id, int absMax, final CreateCallback callback) throws Exception {

        final Wallet wall = decryptWallet(ctx, id);

        String[] pairs = getSeedAndNameForWallet(ctx, id);
        String decryptedSeed = new String(EncryptionManager.decrypt(pairs[0]), USE_STRING_ENCODING);
        String addresses = Mobile.getAddresses(decryptedSeed, absMax);

        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<Address>>() {
        }.getType();
        List<Address> al = gson.fromJson(addresses, listType);
        wall.setAddresses(al);

        String addressList = "";
        for (Address ad : wall.getAddresses()) {
            addressList += ad.getAddress() + ",";
        }

        addressList = addressList.substring(0, addressList.length() - 1);
        Retrofit retrofit = NodeUtils.getRetrofit(Utils.getSkycoinUrl(ctx));
        if (retrofit == null) {
            callback.createComplete(false, null);
            return;
        }

        SkycoinService api = retrofit.create(SkycoinService.class);

        api.getBalances(addressList).enqueue(new Callback<BalanceRes>() {
            @Override
            public void onResponse(Call<BalanceRes> call, Response<BalanceRes> response) {
                BalanceRes br = response.body();
                if (response.code() != HTTP_OK || br == null || br.getConfirmed() == null) {
                    Log.d(TAG, "failed to load from backed:" + response.code());
                    callback.createComplete(false, null);
                } else {
                    Map<String, BalanceRes.BalanceCollection> map = br.getAddresses();
                    int highest = 0;
                    for (String add : map.keySet()) {
                        BalanceRes.BalanceCollection bal = map.get(add);
                        long coins = bal.getConfirmed().getCoins();
                        if (coins > 0) {
                            int index = -1;
                            for (int i = 0; i < wall.getAddresses().size(); i++) {
                                Address a = wall.getAddresses().get(i);
                                if (a.getAddress().equals(add)) {
                                    index = i;
                                    break;
                                }
                            }
                            if (index > highest) {
                                highest = index;
                            }
                        }
                    }

                    Log.d(TAG, "highest address with coins: " + highest);
                    setNumAddresses(ctx, id, highest + 1);
                    callback.createComplete(true, null);
                }

            }

            @Override
            public void onFailure(Call<BalanceRes> call, Throwable t) {
                Log.e(TAG, "service error", t);
                callback.createComplete(false, new Exception(t));
            }
        });

    }

    public static void advancedSend(final Context ctx, final String walletId, final List<Utxo> selectedInputs, final String toAddress,
                                    final String changeAddress, final BigInteger droplets, final SendCallback callback,
                                    long hoursToSend, int burnFactor) {

        InputsPackage inputs = makeUtxosToInputs(selectedInputs);

        TransactionOutput[] outputs = buildTransactionOutputs(inputs, droplets, toAddress, changeAddress, hoursToSend, burnFactor);
        for (TransactionOutput ut : outputs) {
            //BigDecimal dec = BigDecimal.valueOf(ut.decimalCoins);
            //BigDecimal mill = BigDecimal.valueOf(1000000.0f);
            //ut.coinsDroplets = dec.multiply(mill).longValue();
            ut.coins += ut.coinsDroplets.longValue();
            Log.d(TAG, "creating output " + ut.coinsDroplets);
        }

        try {
            fillUtxosWithAddressSecretKey(ctx, walletId, inputs);
        } catch (Exception ex) {
            Log.e(TAG, "could not find secret keys for utxos", ex);
            callback.sendComplete(ctx.getResources().getString(R.string.error_encryption_keys), ex, ERROR_SEND_FAILED);
            return;
        }

        Log.d(TAG, "phew, all done. Build TX!");

        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Utils.getSkycoinUrl(ctx))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            final SkycoinService api = retrofit.create(SkycoinService.class);

            Gson gson = new Gson();
            String inputsJson = gson.toJson(inputs.inputs);
            String outputsJson = gson.toJson(outputs);
            String signed = Mobile.prepareTransaction(inputsJson, outputsJson);
            RawTx rawTx = new RawTx();
            rawTx.rawtx = signed;
            api.injectSignedRawTx(rawTx).enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.code() != HTTP_OK || response.body() == null) {
                        Log.d(TAG, "network error");
                        String errorBodyString = "?";
                        if (response.errorBody() != null) {
                            try {
                                errorBodyString = response.errorBody().string();
                            } catch (Exception innerEx) {
                                // ignore
                            }
                        }

                        callback.sendComplete(ctx.getResources().getString(R.string.error_network), new Exception("HTTP ERROR:" + response.code() +
                                "|" + response.message() +
                                "|" + errorBodyString), ERROR_SEND_FAILED);
                        return;
                    }

                    Log.d(TAG, "posted transaction, res code:" + response.code() + " message:" + response.body());
                    callback.sendComplete(response.body(), null, ERROR_NO_ERROR);
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e(TAG, "failed to post transaction", t);
                    callback.sendComplete(ctx.getResources().getString(R.string.error_send), new Exception(t), ERROR_SEND_FAILED);
                }
            });
        } catch (Exception ex) {
            Log.e(TAG, "could not send", ex);
            callback.sendComplete(ctx.getResources().getString(R.string.error_send), ex, ERROR_SEND_FAILED);
        }
    }

    public static void send(final Context ctx, final Wallet fromWallet, final String toAddress,
                            final BigInteger droplets, final int burnFactor, final SendCallback callback) {

        /*HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();*/


        // fetch all utxos for the wallet
        Retrofit retrofit = NodeUtils.getRetrofit(Utils.getSkycoinUrl(ctx));
        if (retrofit == null) {
            callback.sendComplete(ctx.getResources().getString(R.string.error_network), null, ERROR_SEND_FAILED);
            return;
        }

        final SkycoinService api = retrofit.create(SkycoinService.class);

        String addressList = "";
        for (Address ad : fromWallet.getAddresses()) {
            addressList += ad.getAddress() + ",";
        }
        addressList = addressList.substring(0, addressList.length() - 1);
        api.getUtxos(addressList).enqueue(new Callback<UtxoRes>() {
            @Override
            public void onResponse(Call<UtxoRes> call, Response<UtxoRes> response) {
                if (response.code() != HTTP_OK || response.body() == null
                        || response.body().getUtxoList() == null) {
                    Log.d(TAG, "network error");
                    callback.sendComplete(ctx.getResources().getString(R.string.error_network), null, ERROR_SEND_FAILED);
                    return;
                }


                if (response.body().getUtxoList().size() == 0) {
                    Log.d(TAG, "wallet has no utxos to spend");
                    callback.sendComplete(ctx.getResources().getString(R.string.error_no_utxos), null, ERROR_SEND_FAILED);
                    return;
                }

                for (Utxo u : response.body().getUtxoList()) {
                    Log.d(TAG, "got utxo for wallet: " + u.getAddress() + "=" + u.getCoins() + " / " + u.getCalculatedHours());
                }

                InputsPackage inputs = selectTransactionInputs(ctx, response.body().getUtxoList(), droplets, callback);
                if (inputs == null) {
                    return; // abort
                }

                TransactionOutput[] outputs = buildTransactionOutputs(inputs, droplets, toAddress,
                        fromWallet.getAddresses().get(0).getAddress(), burnFactor);

                for (Utxo ut : inputs.utxos) {
                    Log.d(TAG, "using utxo " + ut.getCoins());
                }
                for (TransactionOutput ut : outputs) {
                    //BigDecimal dec = BigDecimal.valueOf(ut.decimalCoins);
                    //BigDecimal mill = BigDecimal.valueOf(1000000.0f);
                    //ut.coinsDroplets = dec.multiply(mill).longValue();
                    ut.coins += ut.coinsDroplets.longValue();
                    Log.d(TAG, "creating output with droplets: " + ut.coinsDroplets);
                }

                // get secret key for address owning the utxo
                try {
                    fillUtxosWithAddressSecretKey(ctx, fromWallet.getId(), inputs);
                } catch (Exception ex) {
                    Log.e(TAG, "could not find secret keys for utxos", ex);
                    callback.sendComplete(ctx.getResources().getString(R.string.error_encryption_keys), ex, ERROR_SEND_FAILED);
                    return;
                }

                Log.d(TAG, "phew, all done. Build TX!");

                try {
                    Gson gson = new Gson();
                    String inputsJson = gson.toJson(inputs.inputs);
                    String outputsJson = gson.toJson(outputs);
                    String signed = Mobile.prepareTransaction(inputsJson, outputsJson);
                    RawTx rawTx = new RawTx();
                    rawTx.rawtx = signed;
                    api.injectSignedRawTx(rawTx).enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {
                            if (response.code() != HTTP_OK || response.body() == null) {
                                Log.d(TAG, "network error");
                                String errorBodyString = "?";
                                if (response.errorBody() != null) {
                                    try {
                                        errorBodyString = response.errorBody().string();
                                    } catch (Exception innerEx) {
                                        // ignore
                                    }
                                }

                                //callback.sendComplete(ctx.getResources().getString(R.string.error_network),null, ERROR_SEND_FAILED);

                                callback.sendComplete(ctx.getResources().getString(R.string.error_network), new Exception("HTTP ERROR:" + response.code() +
                                        "|" + response.message() +
                                        "|" + errorBodyString), ERROR_SEND_FAILED);
                                return;
                            }

                            Log.d(TAG, "posted transaction, res code:" + response.code() + " message:" + response.body());
                            callback.sendComplete(response.body(), null, ERROR_NO_ERROR);
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Log.e(TAG, "failed to post transaction", t);
                            callback.sendComplete(ctx.getResources().getString(R.string.error_send), new Exception(t), ERROR_SEND_FAILED);
                        }
                    });
                } catch (Exception ex) {
                    Log.e(TAG, "could not send", ex);
                    callback.sendComplete(/*ctx.getResources().getString(R.string.error_send)*/ex.getMessage(), ex, ERROR_SEND_FAILED);
                }
            }

            @Override
            public void onFailure(Call<UtxoRes> call, Throwable t) {
                Log.e(TAG, "failed to get utxos for wallet", t);
                callback.sendComplete(ctx.getResources().getString(R.string.error_could_not_read_blockchain_info_from_node), null, ERROR_SEND_FAILED);
            }
        });
    }

    private static InputsPackage selectTransactionInputs(Context ctx, List<Utxo> available, BigInteger droplets, final SendCallback callback) {

        if (droplets.compareTo(new BigInteger("1")) == -1) {
            Log.w(TAG, "amount too small");
            callback.sendComplete(ctx.getResources().getString(R.string.error_amount_too_small), null, ERROR_SEND_FAILED);
            return null;
        }

        /* loop though utxos and pick until we have the required amount */
        // sort descending on SKY amount
        Collections.sort(available, new Comparator<Utxo>() {
            @Override
            public int compare(Utxo utxo, Utxo t1) {
                BigDecimal ut1Val = new BigDecimal(utxo.getCoins());
                BigDecimal ut2Val = new BigDecimal(t1.getCoins());

                return ut2Val.compareTo(ut1Val);
            }
        });

        for (Utxo utxo : available) {
            Log.d(TAG, "Sorted descending " + utxo.getCoins());
        }

        List<Utxo> useThese = new ArrayList<>();

        // first find the biggest utxo with at least 1 Coin Hour
        for (Utxo utxo : available) {
            if (utxo.getCalculatedHours() > 0) {
                useThese.add(utxo);
                Log.d(TAG, "using utxo " + utxo.getHash() + " for first:" + utxo.getCoins() + "/" + utxo.getCalculatedHours());
                break;
            }
        }

        if (useThese.size() == 0) {
            // no coin hours available in wallet
            Log.w(TAG, "no coin hours in wallet abort tx building");
            callback.sendComplete(ctx.getResources().getString(R.string.error_no_coin_hours), null, ERROR_SEND_FAILED);
            return null;
        }

        BigDecimal firstDecimal = new BigDecimal(useThese.get(0).getCoins());
        firstDecimal = firstDecimal.multiply(new BigDecimal(1000000));

        droplets = droplets.subtract(firstDecimal.toBigInteger());

        if (droplets.compareTo(new BigInteger("0")) != 1) {
            return makeUtxosToInputs(useThese);
        }
        Log.d(TAG, "remaining droplets:" + droplets);

        // need more utxos, check all with 0 coin hours
        for (Utxo utxo : available) {
            if (!useThese.contains(utxo) && utxo.getCalculatedHours() == 0) {
                useThese.add(utxo);
                Log.d(TAG, "also using utxo " + utxo.getHash() + ":" + utxo.getCoins() + "/" + utxo.getCalculatedHours());

                BigDecimal candidateDec = new BigDecimal(utxo.getCoins());
                candidateDec = candidateDec.multiply(new BigDecimal(1000000));

                droplets = droplets.subtract(candidateDec.toBigInteger());

                if (droplets.compareTo(new BigInteger("0")) != 1) {
                    break;
                }
                Log.d(TAG, "remaining droplets:" + droplets);
            }
        }
        Log.d(TAG, "remaining droplets:" + droplets);
        if (droplets.compareTo(new BigInteger("0")) != 1) {
            return makeUtxosToInputs(useThese);
        }

        // need even more utxos, check all with any coin hours
        for (Utxo utxo : available) {
            if (!useThese.contains(utxo)) {
                useThese.add(utxo);
                Log.d(TAG, "also using utxo " + utxo.getHash() + ":" + utxo.getCoins() + "/" + utxo.getCalculatedHours());
                BigDecimal candidateDec = new BigDecimal(utxo.getCoins());
                candidateDec = candidateDec.multiply(new BigDecimal(1000000));

                droplets = droplets.subtract(candidateDec.toBigInteger());

                if (droplets.compareTo(new BigInteger("0")) != 1) {
                    break;
                }
                Log.d(TAG, "remaining droplets:" + droplets);
            }
        }
        Log.d(TAG, "remaining droplets:" + droplets);
        if (droplets.compareTo(new BigInteger("0")) != 1) {
            return makeUtxosToInputs(useThese);
        }

        Log.w(TAG, "insufficient balance");
        callback.sendComplete(ctx.getResources().getString(R.string.error_not_enough_coins), null, ERROR_SEND_FAILED);

        return null;
    }

    private static InputsPackage makeUtxosToInputs(List<Utxo> txos) {
        InputsPackage res = new InputsPackage();
        res.inputs = new TransactionInput[txos.size()];
        res.utxos = txos.toArray(new Utxo[0]);

        for (int i = 0; i < txos.size(); i++) {
            Utxo utx = txos.get(i);
            TransactionInput inp = new TransactionInput();
            inp.hash = utx.getHash();
            inp.owningAddress = utx.getAddress();
            inp.secret = null;
            res.inputs[i] = inp;
            BigDecimal bigDec = new BigDecimal(utx.getCoins());
            bigDec = bigDec.multiply(new BigDecimal(1000000l));
            res.consumedDroplets = res.consumedDroplets.add(bigDec.toBigInteger());
            res.consumedHours += utx.getCalculatedHours();
            Log.d(TAG, "consumed coins: " + res.consumedDroplets);
        }

        return res;
    }

    private static TransactionOutput[] buildTransactionOutputs(InputsPackage inputs, BigInteger dropletsToSend, String toAddress,
                                                               String changeAddress, final int burnFactor) {
        BigInteger change = inputs.consumedDroplets.subtract(dropletsToSend);

        Log.d(TAG, "coins to send: " + dropletsToSend + ", change after consumed inputs: " + change);

        TransactionOutput[] outs;
        long hoursToBurn = (long) Math.ceil(inputs.consumedHours / (double) burnFactor);
        Log.e(TAG, "consumed hours: " + inputs.consumedHours + ",hours to burn with burn factor " + burnFactor + ": " + hoursToBurn);
        if (change.compareTo(new BigInteger("0")) == 1) {
            Log.d(TAG, "change: " + change + " because compareto " + change.compareTo(new BigInteger("0")));
            outs = new TransactionOutput[2];
            outs[1] = new TransactionOutput();
            outs[1].address = changeAddress;
            outs[1].coinsDroplets = change;
            outs[1].hours = (inputs.consumedHours - hoursToBurn) / 2;
            outs[0] = new TransactionOutput();
            outs[0].address = toAddress;
            outs[0].coinsDroplets = dropletsToSend;
            outs[0].hours = (inputs.consumedHours - hoursToBurn) / 2;
        } else {
            // special case. No SKY change exists so we cant make a change transaction with 25%
            // of the coin hours. This means all un-burnt 50% goes to the receiver.
            Log.d(TAG, "no change, send all CH to receiver");
            outs = new TransactionOutput[1];
            outs[0] = new TransactionOutput();
            outs[0].address = toAddress;
            outs[0].coinsDroplets = dropletsToSend;
            outs[0].hours = inputs.consumedHours - hoursToBurn;
        }
        return outs;
    }

    private static TransactionOutput[] buildTransactionOutputs(InputsPackage inputs, BigInteger dropletsToSend, String toAddress,
                                                               String changeAddress, long hoursToSend, int burnFactor) {
        BigInteger change = inputs.consumedDroplets.subtract(dropletsToSend);

        long hoursToBurn = (long) Math.ceil(inputs.consumedHours / (double) burnFactor);

        TransactionOutput[] outs;
        if (change.compareTo(new BigInteger("0")) == 1) {
            Log.d(TAG, "change: " + change);
            outs = new TransactionOutput[2];
            outs[1] = new TransactionOutput();
            outs[1].address = changeAddress;
            outs[1].coinsDroplets = change;
            outs[1].hours = inputs.consumedHours - hoursToBurn - hoursToSend;
            outs[0] = new TransactionOutput();
            outs[0].address = toAddress;
            outs[0].coinsDroplets = dropletsToSend;
            outs[0].hours = hoursToSend;
        } else {
            // special case. No SKY change exists so we cant make a change transaction with
            // remaining coin hours. This means all un-burnt 50% goes to the receiver.
            Log.d(TAG, "no change, send all CH to receiver");
            outs = new TransactionOutput[1];
            outs[0] = new TransactionOutput();
            outs[0].address = toAddress;
            outs[0].coinsDroplets = dropletsToSend;
            outs[0].hours = inputs.consumedHours - hoursToBurn;
        }

        return outs;
    }


    private static void fillUtxosWithAddressSecretKey(Context ctx, String id, InputsPackage inputsPackage) throws Exception {

        Wallet tmpWallet = new Wallet();

        fillWalletAddresses(ctx, tmpWallet, id);

        // now the tmp wallet is filled with address objects including the secret keys
        // make a map of them for easy access
        Map<String, String> secMap = new HashMap<>();
        for (Address ad : tmpWallet.getAddresses()) {
            secMap.put(ad.getAddress(), ad.getSecret());
        }

        for (TransactionInput inp : inputsPackage.inputs) {
            Log.d(TAG, "finding secret key for address " + inp.owningAddress);
            inp.secret = secMap.get(inp.owningAddress);
        }

        //now all inps have the secret set, remember to throw them away as soon as you are done with them
    }

    private static class InputsPackage {
        TransactionInput[] inputs;
        Utxo[] utxos;
        BigInteger consumedDroplets = new BigInteger("0");
        long consumedHours;

        public String toString() {
            return super.toString() + "{" + inputs + "," + utxos + "," + consumedDroplets + "," + consumedHours + "}";
        }
    }

}
