package com.skycoin.wallet.nodebackend;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class BalanceRes {

    @SerializedName("predicted")
    private Balance mPredicted;

    @SerializedName("confirmed")
    private Balance mConfirmed;

    @SerializedName("addresses")
    private Map<String,BalanceCollection> mAddresses;

    public Map<String, BalanceCollection> getAddresses() {
        return mAddresses;
    }

    public void setAddresses(Map<String, BalanceCollection> addresses) {
        mAddresses = addresses;
    }

    public Balance getPredicted() {
        return mPredicted;
    }

    public void setPredicted(Balance predicted) {
        mPredicted = predicted;
    }

    public Balance getConfirmed() {
        return mConfirmed;
    }

    public void setConfirmed(Balance confirmed) {
        mConfirmed = confirmed;
    }

    public static class BalanceCollection {
        @SerializedName("predicted")
        private Balance mPredicted;

        @SerializedName("confirmed")
        private Balance mConfirmed;

        public Balance getPredicted() {
            return mPredicted;
        }

        public void setPredicted(Balance predicted) {
            mPredicted = predicted;
        }

        public Balance getConfirmed() {
            return mConfirmed;
        }

        public void setConfirmed(Balance confirmed) {
            mConfirmed = confirmed;
        }
    }

    public static class Balance {
        @SerializedName("coins")
        private long mCoins;
        @SerializedName("hours")
        private long mHours;

        public long getCoins() {
            return mCoins;
        }

        public void setCoins(long coins) {
            mCoins = coins;
        }

        public long getHours() {
            return mHours;
        }

        public void setHours(long hours) {
            mHours = hours;
        }
    }

}
