package com.skycoin.wallet.wallet;

import com.google.gson.annotations.SerializedName;

public class Address {

    @SerializedName("NextSeed")
    private String mNextSeed;

    // dont store this
    @SerializedName("Secret")
    private String mSecret;

    @SerializedName("Public")
    private String mPublic;

    @SerializedName("Address")
    private String mAddress;

    private long mHours;

    private double mBalance;// droplets (div by 1m to get whole sky)

    public String getNextSeed() {
        return mNextSeed;
    }

    public String getSecret() {
        return mSecret;
    }

    public String getPublic() {
        return mPublic;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String address) {
        mAddress = address;
    }

    public long getHours() {
        return mHours;
    }

    public void setHours(long hours) {
        mHours = hours;
    }

    public double getBalance() {
        return mBalance;
    }

    public void setBalance(double balance) {
        mBalance = balance;
    }

    public void setSecret(String secret) {
        mSecret = secret;
    }

    public String toString() {
        return "Addr:"+mAddress+", secret:"+mSecret;
    }

}
