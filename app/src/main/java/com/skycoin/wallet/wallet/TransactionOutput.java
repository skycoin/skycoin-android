package com.skycoin.wallet.wallet;

import com.google.gson.annotations.SerializedName;

import java.math.BigInteger;

public class TransactionOutput {

    @SerializedName("Address")
    public String address;

    transient public BigInteger coinsDroplets = new BigInteger("0");

    @SerializedName("Coins")
    public long coins;

    @SerializedName("Hours")
    public long hours;

    public String toString() {
        return super.toString() + "{"+address+","+coinsDroplets+","+hours+"}";
    }

}
