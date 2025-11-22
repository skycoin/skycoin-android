package com.skycoin.wallet.wallet;

import com.google.gson.annotations.SerializedName;

public class TransactionInput {

    @SerializedName("Hash")
    public String hash;
    @SerializedName("Secret")
    public String secret;

    // used to help transaction building
    public transient String owningAddress;

    public String toString() {
        return super.toString() + "{"+hash+",SECRET}";
    }

}
