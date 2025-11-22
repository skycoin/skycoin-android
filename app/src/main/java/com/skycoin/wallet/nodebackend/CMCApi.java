package com.skycoin.wallet.nodebackend;

/*
CMC can only identify on id which for skycoin is 1619

example:
{
    "data": {
        "id": 1619,
        "name": "Skycoin",
        "symbol": "SKY",
        "website_slug": "skycoin",
        "rank": 133,
        "circulating_supply": 10000000.0,
        "total_supply": 25000000.0,
        "max_supply": 100000000.0,
        "quotes": {
            "USD": {
                "price": 6.13051,
                "volume_24h": 722324.0,
                "market_cap": 61305100.0,
                "percent_change_1h": -0.01,
                "percent_change_24h": -6.36,
                "percent_change_7d": -5.81
            }
        },
        "last_updated": 1532118873
    },
    "metadata": {
        "timestamp": 1532118478,
        "error": null
    }
}

 */

import retrofit2.Call;
import retrofit2.http.GET;

public interface CMCApi {

    public static final String BASE_URL = "https://api.coinmarketcap.com";

    @GET("v2/ticker/1619/")
    Call<CMCRes> getSKYInfo();



}
