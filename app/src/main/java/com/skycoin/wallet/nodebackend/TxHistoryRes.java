package com.skycoin.wallet.nodebackend;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TxHistoryRes {

    /* local flags, not serialized from json */
    public boolean isSend = false;
    /* end local flags */

    public Status status;
    public long time;
    @SerializedName("txn")
    public Tx tx;

    public static class Status {
        public boolean confirmed;
        public boolean unconfirmed;
        public long height;
        public long block_seq;
        public boolean unknown;
    }

    public static class Tx {
        public int length;
        public int type;
        public long timestamp;
        public String txid;
        @SerializedName("inner_hash")
        public String innerHash;
        public long fee;
        public List<String> sigs;

        public List<HistInxo> inputs;
        public List<HistOutxo> outputs;
    }

    public static class HistInxo {

        public String uxid;
        public String owner;
        public String coins; // decimal number
        public long hours;
        @SerializedName("calculated_hours")
        public long calcHours;

    }

    public static class HistOutxo {

        public String uxid;
        public String dst;
        public String coins; // decimal number
        public long hours;

    }


}
