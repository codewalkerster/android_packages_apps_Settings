package com.android.settings.ethernet;

import android.content.Context;
import android.net.EthernetManager;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.LinkAddress;
import android.net.NetworkUtils;
import android.net.StaticIpConfiguration;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.text.TextUtils;

import java.net.Inet4Address;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

public class EthernetStaticIP  extends SettingsPreferenceFragment 
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "EthernetStaticIP";
    public static final boolean DEBUG = true;
    // public static final boolean DEBUG = false;
    private static void LOG(String msg) {
        if ( DEBUG ) {
            Log.d(TAG, msg);
        }
    }

    private static final String KEY_USE_STATIC_IP = "use_static_ip";

    private static final String KEY_IP_ADDRESS = "ip_address";
    private static final String KEY_GATEWAY = "gateway";
    private static final String KEY_NETWORK_PREFIX_LEGHT = "network_prefix_lenght";
    private static final String KEY_DNS1 = "dns1";
    private static final String KEY_DNS2 = "dns2";

    private static final int MENU_ITEM_SAVE = Menu.FIRST;
    private static final int MENU_ITEM_CANCEL = Menu.FIRST + 1;

    private String[] mPreferenceKeys = {
        KEY_IP_ADDRESS,
        KEY_GATEWAY,
        KEY_NETWORK_PREFIX_LEGHT,
        KEY_DNS1,
        KEY_DNS2,
    };

    private CheckBoxPreference mUseStaticIpCheckBox;

    private boolean isOnPause = false;

    private boolean chageState = false;

    private Context mContext;
    private IpConfiguration mIpConfiguration;
    private EthernetManager mEthernetManager;

    public EthernetStaticIP() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mContext = getActivity();

        mEthernetManager = (EthernetManager)mContext.getSystemService(Context.ETHERNET_SERVICE);

        mIpConfiguration = new IpConfiguration();

        addPreferencesFromResource(R.xml.ethernet_static_ip);

        mUseStaticIpCheckBox = (CheckBoxPreference)findPreference(KEY_USE_STATIC_IP);

        for ( int i = 0; i < mPreferenceKeys.length; i++ ) {
            Preference preference = findPreference(mPreferenceKeys[i]);
            preference.setOnPreferenceChangeListener(this);
        }

        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        mIpConfiguration = mEthernetManager.getConfiguration();

        if(!isOnPause) {
            updateIpSettingsInfo();
        }
        isOnPause = false;
    }

    @Override
    public void onPause() {
        isOnPause = true;
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        menu.add(Menu.NONE, MENU_ITEM_SAVE, 0, R.string.staticip_save)
                .setEnabled(true)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(Menu.NONE, MENU_ITEM_CANCEL, 0, R.string.staticip_cancel)
                .setEnabled(true)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

        case MENU_ITEM_SAVE:
            saveIpSettingsInfo();
            finish();
            return true;
        case MENU_ITEM_CANCEL:
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateIpSettingsInfo() {
        LOG("Static IP status updateIpSettingsInfo");
        mUseStaticIpCheckBox.setChecked(mIpConfiguration.getIpAssignment() == IpAssignment.STATIC);
    }

    private void saveIpSettingsInfo() {
        if (mUseStaticIpCheckBox.isChecked()) {

            mIpConfiguration.setIpAssignment(IpAssignment.STATIC);

            StaticIpConfiguration staticConfig = new StaticIpConfiguration();
            mIpConfiguration.setStaticIpConfiguration(staticConfig);

            EditTextPreference preference = 
                (EditTextPreference)findPreference(KEY_IP_ADDRESS);
            String ipAddr = preference.getText();

            if (TextUtils.isEmpty(ipAddr))
                return;

            Log.e(TAG, "ipAddr = " + ipAddr);

            Inet4Address inetAddr = null;
            try {
                inetAddr = (Inet4Address) NetworkUtils.numericToInetAddress(ipAddr);
            } catch (IllegalArgumentException|ClassCastException e) {
                return;
            }

            preference = (EditTextPreference)findPreference(KEY_NETWORK_PREFIX_LEGHT);
            int networkPrefixLength = -1;
            try {
                networkPrefixLength = Integer.parseInt(preference.getText());
                if (networkPrefixLength < 0 || networkPrefixLength > 32) {
                    return;
                }
                staticConfig.ipAddress = new LinkAddress(inetAddr, networkPrefixLength);
            } catch (NumberFormatException e) {
                return;
            }

            LOG("networkPrefixLength = " + networkPrefixLength);

            preference = (EditTextPreference)findPreference(KEY_GATEWAY);
            String gateway = preference.getText();
            if (!TextUtils.isEmpty(gateway)) {
                try {
                    staticConfig.gateway =
                            (Inet4Address) NetworkUtils.numericToInetAddress(gateway);
                } catch (IllegalArgumentException|ClassCastException e) {
                    return;
                }
            }

            LOG("gateway = " + gateway);

            preference = (EditTextPreference)findPreference(KEY_DNS1);
            String dns1 = preference.getText();
            if (!TextUtils.isEmpty(dns1)) {
                try {
                    staticConfig.dnsServers.add(
                            (Inet4Address) NetworkUtils.numericToInetAddress(dns1));
                } catch (IllegalArgumentException|ClassCastException e) {
                    return;
                }
            }
            LOG("DNS1 = " + dns1);

            preference = (EditTextPreference)findPreference(KEY_DNS2);
            String dns2 = preference.getText();
            if (!TextUtils.isEmpty(dns2)) {
                try {
                    staticConfig.dnsServers.add(
                            (Inet4Address) NetworkUtils.numericToInetAddress(dns2));
                } catch (IllegalArgumentException|ClassCastException e) {
                    return;
                }
            }
            LOG("DNS2 = " + dns2);
        } else {
            mIpConfiguration.setIpAssignment(IpAssignment.DHCP);
            mIpConfiguration.setStaticIpConfiguration(null);
        }
        mEthernetManager.setConfiguration(mIpConfiguration);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        boolean result = true;
        LOG("onPreferenceTreeClick()  chageState = " + chageState);
        chageState = true;

        return result;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {

        boolean result = true;
        String key = preference.getKey();
        LOG("onPreferenceChange() : key = " + key);

        if (null == key) {
            return true;
        } else if ( key.equals(KEY_IP_ADDRESS)
                || key.equals(KEY_GATEWAY)
                || key.equals(KEY_DNS1)
                || key.equals(KEY_DNS2) ) {

            String value = (String)newValue;

            LOG("onPreferenceChange() : value = " + value);

            if (TextUtils.isEmpty(value)) {
                ((EditTextPreference)preference).setText(value);
                preference.setSummary(value);
                result = true;
            } else  if ( !isValidIpAddress(value) ) {
                LOG("onPreferenceChange() : IP address user inputed is INVALID." );
                Toast.makeText(getActivity(), 
                        R.string.ethernet_ip_settings_invalid_ip, Toast.LENGTH_LONG).show();
                return false;
            } else {
                ((EditTextPreference)preference).setText(value);
                preference.setSummary(value);
                result = true;
            }
        }

        return result;
    }

    private boolean isValidIpAddress(String value) {
        int start = 0;
        int end = value.indexOf('.');
        int numBlocks = 0;

        while (start < value.length()) {

            if ( -1 == end ) {
                end = value.length();
            }

            try {
                int block = Integer.parseInt(value.substring(start, end));
                if ((block > 255) || (block < 0)) {
                    Log.w(TAG, "isValidIpAddress() : invalid 'block', block = " + block);
                    return false;
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "isValidIpAddress() : e = " + e);
                return false;
            }

            numBlocks++;

            start = end + 1;
            end = value.indexOf('.', start);
        }

        return numBlocks == 4;
    }
}
