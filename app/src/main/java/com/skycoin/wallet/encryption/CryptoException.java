package com.skycoin.wallet.encryption;


// This is just because the crypto operations throws a shitload of exceptions and it
// is a pina to catch all of them
public class CryptoException extends Exception {

    public CryptoException(String message) {
        super(message);
    }

    public CryptoException(String message, Throwable cause) {
        super(message, cause);
    }

}
