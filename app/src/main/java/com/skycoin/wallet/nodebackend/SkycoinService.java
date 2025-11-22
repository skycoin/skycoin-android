package com.skycoin.wallet.nodebackend;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SkycoinService {

    public static final String BASE_URL = "https://node.skycoin.net/";

    @GET("api/v1/health")
    Call<NodeHealthRes> getNodeHealth();

    @GET("api/v1/version")
    Call<VersionRes> getVersion();

    @GET("api/v1/balance")
    Call<BalanceRes> getBalances(@Query("addrs") String addresses);

    @GET("api/v1/outputs")
    Call<UtxoRes> getUtxos(@Query("addrs") String addresses);

    @POST("api/v1/injectTransaction")
    Call<String> injectSignedRawTx(@Body RawTx body);

    @GET("api/v1/transactions")
    Call<List<TxHistoryRes>> getTxHistory(@Query("addrs") String addresses,
                                             @Query("verbose") boolean fullInfo,
                                             @Query("confirmed") int confirmed);


}
