package com.skycoin.wallet.wallet;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Wallet {

    private String mId;

    private String mName;

    /**
     * This seed is encrypted and must be decrypted every time it is used
     */
    private String mSeed;

    private long mBalance;

    private long mHours;

    private List<Address> mAddresses;

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    /**
     * This seed is encrypted and must be decrypted every time it is used
     *
     * @return
     */
    public String getSeed() {
        return mSeed;
    }

    public long getBalance() {
        return mBalance;
    }

    public long getHours() {
        return mHours;
    }

    public List<Address> getAddresses() {
        return mAddresses;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public void setSeed(String seed) {
        mSeed = seed;
    }

    public void setBalance(long balance) {
        this.mBalance = balance;
    }

    public void setHours(long hours) {
        this.mHours = hours;
    }

    public void setAddresses(List<Address> addresses) {
        this.mAddresses = addresses;
    }

    public String toString() {
        return super.toString() + "ID:"+mId+", name:"+mName+" addresses:["+getAddresses()+"]";
    }

}
