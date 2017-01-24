/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.ethernet;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

public class EthernetSettings extends SettingsPreferenceFragment {
    private static final String TAG = "EthernetSettings";

    private static final String USB_ETHERNET_SETTINGS = "ethernet";

    private static final String KEY_ETH_IP_ADDRESS = "ethernet_ip_addr";
    private static final String KEY_ETH_MAC = "ethernet_mac";

    private final static String nullIpInfo = "0.0.0.0";

    private IntentFilter mIntentFilter;

    private ConnectivityManager mConnectivityManager;
    private EthernetManager mEthernetManager;
    private Context mContext;

    public EthernetSettings() {
        super();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "received");
            getEthInfo();
        }
    };

    public boolean isEthernetAvailable() {
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || networkInfo.getType() != ConnectivityManager.TYPE_ETHERNET)
            return false;
        else
            return true;
        /*
        if (mConnectivityManager.isNetworkSupported(ConnectivityManager.TYPE_ETHERNET)) {
            Log.e(TAG, "TYPE_ETHERNET");
            return mEthernetManager.isAvailable();
        }

        return false;
        */
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        mEthernetManager = (EthernetManager) mContext.getSystemService(Context.ETHERNET_SERVICE);

        addPreferencesFromResource(R.xml.ethernet_settings);

        if (mEthernetManager == null) {
            Log.e(TAG, "get ethernet manager failed");
            return;
        }

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        //mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION_IMMEDIATE);
        mIntentFilter.addAction(ConnectivityManager.INET_CONDITION_ACTION);

        getEthInfo();
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(mReceiver, mIntentFilter);
        if (mEthernetManager == null)
            return;

        getEthInfo();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mReceiver);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.MAIN_SETTINGS;
    }

    public IpConfiguration getIpConfiguration() {
        return mEthernetManager.getConfiguration();
    }

    public String getEthernetMacAddress() {
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || networkInfo.getType() != ConnectivityManager.TYPE_ETHERNET) {
            return "";
        } else {
            return networkInfo.getExtraInfo();
        }
    }

    public String getEthernetIpAddress() {
        LinkProperties linkProperties =
                mConnectivityManager.getLinkProperties(ConnectivityManager.TYPE_ETHERNET);

        if (linkProperties == null) {
            return "";
        }
        for (LinkAddress linkAddress: linkProperties.getAllLinkAddresses()) {
            InetAddress address = linkAddress.getAddress();
            if (address instanceof Inet4Address) {
                return address.getHostAddress();
            }
        }

        // IPv6 address will not be shown like WifiInfo internally does.
        return "";
    }


    private void setStringSummary(String preference, String value) {
        try {
            findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(preference).setSummary("");
        }
    }

     public void getEthInfo() {
        if (isEthernetAvailable())
            setStringSummary(USB_ETHERNET_SETTINGS, "Connected");
        else
            setStringSummary(USB_ETHERNET_SETTINGS, "Not connected");

        setStringSummary(KEY_ETH_IP_ADDRESS, getEthernetIpAddress());
        setStringSummary(KEY_ETH_MAC, getEthernetMacAddress());
    }
}

