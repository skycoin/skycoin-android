package com.skycoin.wallet.nodebackend;

/*
    "blockchain": {
        "head": {
            "seq": 58894,
            "block_hash": "3961bea8c4ab45d658ae42effd4caf36b81709dc52a5708fdd4c8eb1b199a1f6",
            "previous_block_hash": "8eca94e7597b87c8587286b66a6b409f6b4bf288a381a56d7fde3594e319c38a",
            "timestamp": 1537581604,
            "fee": 485194,
            "version": 0,
            "tx_body_hash": "c03c0dd28841d5aa87ce4e692ec8adde923799146ec5504e17ac0c95036362dd",
            "ux_hash": "f7d30ecb49f132283862ad58f691e8747894c9fc241cb3a864fc15bd3e2c83d3"
        },
        "unspents": 38171,
        "unconfirmed": 1,
        "time_since_last_block": "4m46s"
    },
    "version": {
        "version": "0.24.1",
        "commit": "8798b5ee43c7ce43b9b75d57a1a6cd2c1295cd1e",
        "branch": "develop"
    },
    "coin": "skycoin",
    "user_agent": "skycoin:0.25.0-rc1",
    "open_connections": 8,
    "outgoing_connections": 5,
    "incoming_connections": 3,
    "uptime": "6m30.629057248s",
    "csrf_enabled": true,
    "csp_enabled": true,
    "wallet_api_enabled": true,
    "gui_enabled": true,
    "unversioned_api_enabled": false,
    "json_rpc_enabled": false,
    "user_verify_transaction": {
        "burn_factor": 2,
        "max_transaction_size": 32768,
        "max_decimals": 3
    },
    "unconfirmed_verify_transaction": {
        "burn_factor": 2,
        "max_transaction_size": 32768,
        "max_decimals": 3
    },
    "started_at": 1542443907

 */

import com.google.gson.annotations.SerializedName;

public class NodeHealthRes {

    @SerializedName("coin")
    public String coinName;

    @SerializedName("user_verify_transaction")
    public VerifyTransaction userVerifyTxRules;

    public String uptime;

    @SerializedName("version")
    public VersionRes versionInfo;

    public static class VerifyTransaction {
        @SerializedName("burn_factor")
        public int burnFactor;
        @SerializedName("max_transaction_size")
        public int maxTxSize;
        @SerializedName("max_decimals")
        public int maxDecimals;
    }

}
