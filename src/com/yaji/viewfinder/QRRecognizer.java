package com.yaji.viewfinder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.client.result.EmailAddressParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.ResultParser;
import com.google.zxing.client.result.SMSParsedResult;
import com.google.zxing.common.HybridBinarizer;

public class QRRecognizer {
    private static final String LOG_TAG = "yaji";

    String mText;
    ParsedResultType mType;
    Intent mIntent;

    public boolean recognize(Bitmap bitmap) {
        boolean isFound = false;

        // Initialize
        mText = null;
        mType = null;
        mIntent = null;

        // Get BinaryBitmap object to be used for QR code recognition.
        LuminanceSource source = new RGBLuminanceSource(bitmap);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

        // QR code result
        Result result = null;
        BarcodeFormat format = null;
        try {
            // Create reader for multi-format.
            Reader reader = new MultiFormatReader();
            result = reader.decode(binaryBitmap);
            if (result != null) {
                format = result.getBarcodeFormat();
                mText = result.getText();
            }
        } catch (NotFoundException e) {
            Log.w(LOG_TAG, "NotFoundException:" + e.getMessage());
        } catch (ChecksumException e) {
            Log.w(LOG_TAG, "ChecksumException:" + e.getMessage());
        } catch (FormatException e) {
            Log.w(LOG_TAG, "FormatException:" + e.getMessage());
        }

        // Check if we succeeded in recognition.
        if (format != null) {
            Log.d(LOG_TAG, "QR code, format:" + format.name() + ", text:" + result.getText());
            if (format.name().equals(BarcodeFormat.QR_CODE.name())) {
                ParsedResult parsedResult = ResultParser.parseResult(result);
                mType = parsedResult.getType();
                Log.d(LOG_TAG, "QR code, type:" + mType.name());

                switch (mType) {
                case ADDRESSBOOK:
                case PRODUCT:
                case WIFI:
                case ISBN:
                case CALENDAR:
                    // Not supported.
                    // See
                    // http://code.google.com/p/zxing/source/browse/trunk/android/src/com/google/zxing/client/android/result/ResultHandler.java
                    break;
                case EMAIL_ADDRESS:
                    EmailAddressParsedResult er = (EmailAddressParsedResult) parsedResult;
                    mIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(er.getMailtoURI()));
                    mIntent.putExtra(Intent.EXTRA_SUBJECT, er.getSubject());
                    mIntent.putExtra(Intent.EXTRA_TEXT, er.getBody());
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    break;
                case URI:
                    mIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mText));
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    break;
                case GEO:
                    mIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mText));
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    break;
                case TEL:
                    mIntent = new Intent(Intent.ACTION_DIAL, Uri.parse(mText));
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    break;
                case SMS:
                    SMSParsedResult sr = (SMSParsedResult) parsedResult;
                    mIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(sr.getSMSURI()));
                    mIntent.putExtra("sms_body", sr.getBody());
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    break;
                default:
                    break;
                }
            }
            isFound = true;
        }
        return isFound;
    }

    public String getText() {
        return mText;
    }

    public ParsedResultType getType() {
        return mType;
    }

    public Intent getLaunchIntent() {
        return mIntent;
    }
}
