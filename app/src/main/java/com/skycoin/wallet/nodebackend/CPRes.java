package com.skycoin.wallet.nodebackend;

/*
         {
         "id":"sky-skycoin",
         "name":"Skycoin",
         "symbol":"SKY",
         "rank":179,
         "circulating_supply":13701297,
         "total_supply":25000000,
         "max_supply":100000000,
         "beta_value":1.54157,
         "last_updated":"2019-03-03T17:35:03Z",
         "quotes":{
             "USD":{
                     "price":1.16032648,
                     "volume_24h":1842472.2247736,
                     "volume_24h_change_24h":-15.47,
                     "market_cap":15897977,
                     "market_cap_change_24h":1.21,
                     "percent_change_1h":0.88,
                     "percent_change_12h":0.18,
                     "percent_change_24h":0.84,
                     "percent_change_7d":20.37,
                     "percent_change_30d":22.25,
                     "percent_change_1y":-92.7,
                     "ath_price":53.8294,
                     "ath_date":"2017-12-30T02:54:00Z",
                     "percent_from_price_ath":-97.84
                 }
             }
         }
         */

import com.google.gson.annotations.SerializedName;

public class CPRes {

    public String id;
    public String name;
    public String symbol;

    public Quotes quotes;

    public static class Quotes {

        public SingleQuote USD;
    }

    public static class SingleQuote {

        public double price;

    }

}
