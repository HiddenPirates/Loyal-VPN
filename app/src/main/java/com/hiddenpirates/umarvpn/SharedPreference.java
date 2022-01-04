package com.hiddenpirates.umarvpn;

import android.content.Context;
import android.content.SharedPreferences;

import com.hiddenpirates.umarvpn.model.Server;

import static com.hiddenpirates.umarvpn.Utils.getImgURL;

public class SharedPreference {

    private static final String APP_PREFS_NAME = "CakeVPNPreference";

    private final SharedPreferences mPreference;
    private final SharedPreferences.Editor mPrefEditor;

    private static final String SERVER_COUNTRY = "server_country";
    private static final String SERVER_FLAG = "server_flag";
    private static final String SERVER_OVPN = "server_ovpn";
    private static final String SERVER_OVPN_USER = "server_ovpn_user";
    private static final String SERVER_OVPN_PASSWORD = "server_ovpn_password";

    public SharedPreference(Context context) {
        this.mPreference = context.getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE);
        this.mPrefEditor = mPreference.edit();
    }

    /**
     * Save server details
     * @param server details of ovpn server
     */
    public void saveServer(Server server){
        mPrefEditor.putString(SERVER_COUNTRY, server.getCountry());
        mPrefEditor.putString(SERVER_FLAG, server.getFlagUrl());
        mPrefEditor.putString(SERVER_OVPN, server.getOvpn());
        mPrefEditor.putString(SERVER_OVPN_USER, server.getOvpnUserName());
        mPrefEditor.putString(SERVER_OVPN_PASSWORD, server.getOvpnUserPassword());
        mPrefEditor.commit();
    }

    /**
     * Get server data from shared preference
     * @return server model object
     */
    public Server getServer() {

        return new Server(
                mPreference.getString(SERVER_COUNTRY,"Singapore (VIP)"),
                mPreference.getString(SERVER_FLAG,getImgURL(R.drawable.singapore)),
                mPreference.getString(SERVER_OVPN,"singapore.ovpn")
        );
    }
}
