package com.skycoin.wallet.nodebackend;

import com.google.gson.annotations.SerializedName;

public class Utxo {

    private String hash;
    private long time;
    @SerializedName("block_seq")
    private long blockSeq;
    @SerializedName("src_tx")
    private String srcTx;
    private String address;
    private String coins; // decimal
    private long hours;
    @SerializedName("calculated_hours")
    private long calculatedHours;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getBlockSeq() {
        return blockSeq;
    }

    public void setBlockSeq(long blockSeq) {
        this.blockSeq = blockSeq;
    }

    public String getSrcTx() {
        return srcTx;
    }

    public void setSrcTx(String srcTx) {
        this.srcTx = srcTx;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCoins() {
        return coins;
    }

    public void setCoins(String coins) {
        this.coins = coins;
    }

    public long getHours() {
        return hours;
    }

    public void setHours(long hours) {
        this.hours = hours;
    }

    public long getCalculatedHours() {
        return calculatedHours;
    }

    public void setCalculatedHours(long calculatedHours) {
        this.calculatedHours = calculatedHours;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof Utxo)) {
            return false;
        }
        Utxo other = (Utxo) obj;
        if (other.hash == null || this.hash == null) {
            return false;
        }

        // same txid means equals
        return this.hash.equals(other.hash);
    }


}
