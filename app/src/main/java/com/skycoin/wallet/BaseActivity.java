package com.skycoin.wallet;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.widget.Toast;

import com.skycoin.wallet.home.PinDialogFragment;
import com.skycoin.wallet.home.TransactionsFragment;
import com.skycoin.wallet.onboarding.PinFragment;
import com.skycoin.wallet.wallet.Wallet;
import com.skycoin.wallet.wallet.WalletManager;

import java.util.List;

/**
 * This Activity contains some lifecycle checks that should always
 * be run like checking if the user has chosen a PIN, if the PIN
 * should be requested, etc.
 * <p>
 * Some activities should not run these and should inherit from Activity directly
 */
public class BaseActivity extends FragmentActivity {

    private static final String TAG = BaseActivity.class.getName();

    protected ProgressDialog mSpinner;

    protected List<Wallet> mAllWallets;

    protected void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
    }

    protected void onResume() {
        super.onResume();
    }

    public void loadAllWallets() {
        mAllWallets = WalletManager.getAllWallets(this);
    }

    public void showLoadingPopup(String overrideText) {
        if (mSpinner != null && mSpinner.isShowing()) {
            return;
        }
        mSpinner = new ProgressDialog(this, R.style.SkyProgressDialog);
        mSpinner.setCancelable(false);
        mSpinner.setCanceledOnTouchOutside(false);
        mSpinner.setMessage(TextUtils.isEmpty(overrideText) ? getResources().getString(R.string.loading) : overrideText);
        mSpinner.show();
    }

    public void hideLoadingPopup() {
        if (mSpinner != null) {
            mSpinner.dismiss();
            mSpinner = null;
        }
    }

    public void showInfoPopup(String heading, String message, String buttonText, final PopupCallback cb) {
        AlertDialog.Builder b = new AlertDialog.Builder(this, R.style.SkyAlertDialog);

        AlertDialog ad = b.setCancelable(false).setTitle(heading).setMessage(message).setPositiveButton(buttonText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        if (cb != null) {
                            cb.onCallback(null, 0);
                        }
                    }
                }).create();
        ad.show();
    }
    public void showConfirmPopup(String heading, String message, String yesButtonText, String noButtonText, final ConfirmCallback cb) {
        AlertDialog.Builder b = new AlertDialog.Builder(this, R.style.SkyAlertDialog);

        AlertDialog ad = b.setCancelable(false).setTitle(heading).setMessage(message).setPositiveButton(yesButtonText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                        if (cb != null) {
                            cb.onCallback(true, 0);
                        }
                    }
                }).setNegativeButton(noButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (cb != null) {
                    cb.onCallback(false, 0);
                }
            }
        }).create();
        ad.show();
    }

    public void showTxSuccessPopup(String heading, String message, String buttonText, final PopupCallback cb, final String txId) {
        AlertDialog.Builder b = new AlertDialog.Builder(this, R.style.SkyAlertDialog);

        AlertDialog ad = b.setCancelable(false).setTitle(heading).setMessage(message).setPositiveButton(buttonText,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        if (cb != null) {
                            cb.onCallback(null, 0);
                        }
                    }
                }).setNeutralButton(R.string.copy_tx_to_clipboard, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ClipboardManager clipboard = (android.content.ClipboardManager) BaseActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = android.content.ClipData.newPlainText("Last Skycoin TX id", txId);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(BaseActivity.this,R.string.copied, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                if (cb != null) {
                    cb.onCallback(null, 0);
                }
            }
        }).create();
        ad.show();

    }

    public boolean shouldRequirePin() {
        return true;
    }

    public void onStart() {
        super.onStart();

        if (shouldRequirePin()) {
            requestPin(new PinDialogFragment.PinCallback() {
                @Override
                public void onCallback(boolean succeeded, String res, int code) {
                    onPinSuccess();
                }
            }, false, null);
        }

    }

    public void onPinSuccess() {
        // default do nothing. Override in subclass if needed
    }

    public void requestPin(final PinDialogFragment.PinCallback cb, boolean cancelable, String optionalMessage) {
        DialogFragment sendFragment = PinDialogFragment.newInstance(cb, optionalMessage);
        sendFragment.setCancelable(cancelable);
        sendFragment.show(getSupportFragmentManager(), PinDialogFragment.getFragmentTag());
    }

    public interface PopupCallback {
        public void onCallback(String res, int code);
    }

    public interface ConfirmCallback {
        public void onCallback(boolean confirmed, int code);
    }


}
