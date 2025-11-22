package com.skycoin.wallet.nodebackend;

import com.google.gson.annotations.SerializedName;

public class VersionRes {

    @SerializedName("version")
    private String mVersion;

    @SerializedName("commit")
    private String mCommit;

    @SerializedName("branch")
    private String mBranch;

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String mVersion) {
        this.mVersion = mVersion;
    }

    public String getCommit() {
        return mCommit;
    }

    public void setCommit(String mCommit) {
        this.mCommit = mCommit;
    }

    public String getBranch() {
        return mBranch;
    }

    public void setBranch(String mBranch) {
        this.mBranch = mBranch;
    }
}
