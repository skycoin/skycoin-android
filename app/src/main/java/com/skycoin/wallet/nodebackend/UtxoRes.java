package com.skycoin.wallet.nodebackend;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UtxoRes {

    @SerializedName("head_outputs")
    private List<Utxo> utxoList;

    public List<Utxo> getUtxoList() {
        return utxoList;
    }

    public void setUtxoList(List<Utxo> utxoList) {
        this.utxoList = utxoList;
    }
}
