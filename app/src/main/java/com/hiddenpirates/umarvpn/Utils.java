package com.hiddenpirates.umarvpn;

import android.net.Uri;

public class Utils {

    public static String getImgURL(int resourceId) {
        return Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + resourceId).toString();
    }
}
