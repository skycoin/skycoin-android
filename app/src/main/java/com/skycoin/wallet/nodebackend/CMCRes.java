package com.skycoin.wallet.nodebackend;

public class CMCRes {

    public Data data;

    public static class Data {

        /*
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
         */

        public int id;
        public String name;
        public String symbol;

        public Quotes quotes;

    }

    public static class Quotes {

        public Quote USD;

    }

    public static class Quote {

        public double price;

    }

}
