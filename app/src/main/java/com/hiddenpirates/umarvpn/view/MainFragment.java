package com.hiddenpirates.umarvpn.view;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.google.android.ads.nativetemplates.NativeTemplateStyle;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.hiddenpirates.umarvpn.CheckInternetConnection;
import com.hiddenpirates.umarvpn.R;
import com.hiddenpirates.umarvpn.SharedPreference;
import com.hiddenpirates.umarvpn.databinding.FragmentMainBinding;
import com.hiddenpirates.umarvpn.interfaces.ChangeServer;
import com.hiddenpirates.umarvpn.model.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import de.blinkt.openvpn.OpenVpnApi;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.OpenVPNThread;
import de.blinkt.openvpn.core.VpnStatus;

import static android.app.Activity.RESULT_OK;

public class MainFragment extends Fragment implements View.OnClickListener, ChangeServer {

    private Server server;
    private CheckInternetConnection connection;

    boolean vpnStart = false;
    private SharedPreference preference;

    private FragmentMainBinding binding;
    boolean watched = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);

        View view = binding.getRoot();
        initializeAll();

        return view;
    }

    /**
     * Initialize all variable and object
     */
    private void initializeAll() {
        preference = new SharedPreference(requireContext());
        server = preference.getServer();
        updateCurrentServerIconAndName(server.getFlagUrl(), server.getCountry());
        connection = new CheckInternetConnection();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.connectVpnBtn.setOnClickListener(this);
        binding.selectedServerIcon.setOnClickListener(this);

        // Checking is vpn already running or not
        isServiceRunning();
        VpnStatus.initLogCache(requireActivity().getCacheDir());

        // Initializing Ads

        final AdLoader adLoader = new AdLoader.Builder(requireContext(), getResources().getString(R.string.admob_native_ad))
                .forNativeAd(nativeAd -> {

                    NativeTemplateStyle styles = new NativeTemplateStyle.Builder().build();
                    binding.nativeAdViewInMainActivity.setStyles(styles);
                    binding.nativeAdViewInMainActivity.setNativeAd(nativeAd);
                    binding.nativeAdViewInMainActivity.setVisibility(View.VISIBLE);
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Toast.makeText(getActivity(), "No Ads", Toast.LENGTH_SHORT).show();
                        Log.d(MainActivity.TAG, loadAdError.getMessage());
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        .build())
                .build();

        adLoader.loadAd(MainActivity.adRequest);
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.connectVpnBtn) {

            if (vpnStart) {
                confirmDisconnect();
            }
            else {

                if (MainActivity.mRewardedAd != null) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("Connection Ready!");
                    builder.setMessage("Watch a video ad to connect this server.");
                    builder.setCancelable(false);

                    builder.setPositiveButton("Watch Ad", (dialog, which) -> {

                        MainActivity.mRewardedAd.show(requireActivity(), rewardItem -> watched = true);

                        MainActivity.mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                            @Override
                            public void onAdDismissedFullScreenContent() {

                                if (watched){
                                    prepareVpn();
                                }
                                else {
                                    Toast.makeText(requireContext(), "You have not watched full video ad.", Toast.LENGTH_LONG).show();
                                }

                                loadNewRewardedAd();
                            }
                        });
                    });

                    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

                    AlertDialog alert = builder.create();

                    alert.show();
                }
                else {
                    prepareVpn();
                }
            }
        }
        else if (v.getId() == R.id.selectedServerIcon) {
            MainActivity.openCloseDrawer();
        }
    }

    /**
     * Show show disconnect confirm dialog
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    public void confirmDisconnect(){
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle(requireActivity().getResources().getString(R.string.app_name));
        builder.setIcon(getResources().getDrawable(R.drawable.ic_baseline_vpn_lock_24));
        builder.setMessage(requireActivity().getResources().getString(R.string.connection_close_confirm));

        builder.setPositiveButton("Yes", (dialog, id) -> {

            stopVpn();

            if (MainActivity.mInterstitialAd != null){

                MainActivity.mInterstitialAd.show(requireActivity());

                InterstitialAd.load(requireContext(), getResources().getString(R.string.admob_interstitial_ad), MainActivity.adRequest, new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        MainActivity.mInterstitialAd = interstitialAd;
                    }
                });
            }
        });
        builder.setNegativeButton("No", (dialog, id) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Prepare for vpn connect with required permission
     */
    private void prepareVpn() {

        if (!vpnStart) {

            if (getInternetStatus()) {

                // Checking permission for network monitor
                Intent intent = VpnService.prepare(getContext());

                if (intent != null) {
                    startActivityForResult(intent, 1);
                }
                else {
                    startVpn();     //have already permission
                }

                // Update confection status
                status("connecting");

            }
            else {
                // No internet connection available
                showToast("you have no internet connection !!");
            }

        }
        else if (stopVpn()) {
            // VPN is stopped, show a Toast message.
            showToast("Disconnected Successfully !!");
        }
    }

    /**
     * Stop vpn
     * @return boolean: VPN status
     */
    public boolean stopVpn() {
        try {
            OpenVPNThread.stop();
            status("connect");
            vpnStart = false;
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Taking permission for network access
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            //Permission granted, start the VPN
            startVpn();
        } else {
            showToast("Permission Denied !! ");
        }
    }

    /**
     * Internet connection status.
     */
    public boolean getInternetStatus() {
        return connection.netCheck(requireContext());
    }

    /**
     * Get service status
     */
    public void isServiceRunning() {
        setStatus(OpenVPNService.getStatus());
    }

    /**
     * Start the VPN
     */
    @SuppressLint("SetTextI18n")
    private void startVpn() {
        try {
            // .ovpn file
            InputStream conf = requireActivity().getAssets().open(server.getOvpn());
            InputStreamReader isr = new InputStreamReader(conf);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder config = new StringBuilder();
            String line;

            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                }
                config.append(line).append("\n");
            }

            br.readLine();
            OpenVpnApi.startVpn(getContext(), config.toString(), server.getCountry(), server.getOvpnUserName(), server.getOvpnUserPassword());

            // Update log
            binding.connectionStatusTv.setText("Status: Connecting...");
            vpnStart = true;

        }
        catch (IOException | RemoteException e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("SetTextI18n")
    public void setStatus(String connectionState) {

        if (connectionState!= null){

            switch (connectionState) {

                case "DISCONNECTED":
                    status("connect");
                    vpnStart = false;
                    OpenVPNService.setDefaultStatus();
                    binding.connectionStatusTv.setText("Status: Disconnected");
                    break;

                case "CONNECTED":
                    vpnStart = true;// it will use after restart this activity
                    status("connected");
                    binding.connectionStatusTv.setText("Status: Connected");
                    break;

                case "WAIT":
                    status("connecting");
                    binding.connectionStatusTv.setText("Status: Waiting for server connection...");
                    break;

                case "GET_CONFIG":
                    status("connecting");
                    binding.connectionStatusTv.setText("Status: Getting info...");
                    break;

                case "TCP_CONNECT":
                    status("connecting");
                    binding.connectionStatusTv.setText("Status: Authenticating tcp connection...");
                    break;

                case "RESOLVE":
                    status("connecting");
                    binding.connectionStatusTv.setText("Status: Resolving connection...");
                    break;

                case "AUTH_PENDING":
                    status("connecting");
                    binding.connectionStatusTv.setText("Status: Authentication pending...");
                    break;

                case "AUTH":
                    status("connecting");
                    binding.connectionStatusTv.setText("Status: Server authenticating...");
                    break;

                case "RECONNECTING":
                    status("connecting");
                    binding.connectionStatusTv.setText("Status: Status: Reconnecting...");
                    break;

                case "NONETWORK":
                    binding.connectionStatusTv.setText("Status: No network connection");
                    break;
            }
        }
    }

    /**
     * Change button background color and text
     * @param status: VPN current status
     */

    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    public void status(String status) {

        switch (status) {

            case "connect":
                vpnStart = false;
                binding.connectVpnBtn.setText(getResources().getString(R.string.connect));
                binding.connectVpnBtn.setBackgroundColor(getResources().getColor(R.color.disconnect_teal));
                binding.logoImageView.setImageDrawable(getResources().getDrawable(R.drawable.app_icon_transparent_teal));
                break;

            case "connecting":
                binding.connectVpnBtn.setBackgroundColor(getResources().getColor(R.color.disconnect_teal));
                binding.connectVpnBtn.setText(getResources().getString(R.string.connecting));
                binding.logoImageView.setImageDrawable(getResources().getDrawable(R.drawable.app_icon_transparent_teal));
                break;

            case "connected":
                binding.connectVpnBtn.setBackgroundColor(ContextCompat.getColor(requireActivity(), R.color.green));
                binding.connectVpnBtn.setText(getResources().getString(R.string.disconnect));
                binding.logoImageView.setImageDrawable(getResources().getDrawable(R.drawable.app_icon_transparent_green));
                break;

            case "tryDifferentServer":
                binding.connectVpnBtn.setBackgroundColor(getResources().getColor(R.color.disconnect_teal));
                binding.connectVpnBtn.setText("Try Different\nServer");
                break;

            case "loading":
                binding.connectVpnBtn.setBackgroundColor(getResources().getColor(R.color.disconnect_teal));
                binding.connectVpnBtn.setText("Loading Server...");
                break;

            case "invalidDevice":
                binding.connectVpnBtn.setBackgroundColor(getResources().getColor(R.color.disconnect_teal));
                binding.connectVpnBtn.setText("Invalid Device");
                break;

            case "authenticationCheck":
                binding.connectVpnBtn.setBackgroundColor(getResources().getColor(R.color.disconnect_teal));
                binding.connectVpnBtn.setText("Authentication \n Checking...");
                break;
        }
    }

    /**
     * Receive broadcast message
     */
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                setStatus(intent.getStringExtra("state"));
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            try {
                String duration = intent.getStringExtra("duration");

                if (duration == null)
                    duration = "00:00:00";

                updateConnectionStatus(duration);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Update status UI
     * @param duration: running time
     */

    @SuppressLint("SetTextI18n")
    public void updateConnectionStatus(String duration) {
        binding.durationTv.setText("Duration: " + duration);
    }

    /**
     * Show toast message
     * @param message: toast message
     */

    public void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    /**
     * VPN server country icon change
     * @param serverIcon: icon URL
     */

    public void updateCurrentServerIconAndName(String serverIcon, String serverLocation) {
        Glide.with(requireContext()).load(serverIcon).into(binding.selectedServerIcon);
        binding.connectionLocationTv.setText(serverLocation);
    }

    /**
    * Loading new rewarded ad
     */

    public void loadNewRewardedAd(){

        RewardedAd.load(requireActivity(), getResources().getString(R.string.admob_rewarded_ad),
                MainActivity.adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        MainActivity.mRewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        MainActivity.mRewardedAd = rewardedAd;
                        Log.d(MainActivity.TAG, "rewarded");
                    }
                });
    }

    /**
     * Change server when user select new server
     * @param server ovpn server details
     */

    @Override
    public void newServer(Server server) {
        this.server = server;
        updateCurrentServerIconAndName(server.getFlagUrl(), server.getCountry());

        // Stop previous connection
        if (vpnStart) {
            stopVpn();
        }

        prepareVpn();
    }

    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(broadcastReceiver, new IntentFilter("connectionState"));

        if (server == null) {
            server = preference.getServer();
        }
        super.onResume();

        if (!OpenVPNService.getStatus().equalsIgnoreCase("CONNECTED")){
            vpnStart = false;
            binding.connectionStatusTv.setText("Status: Disconnected");
            binding.connectVpnBtn.setBackgroundColor(getResources().getColor(R.color.disconnect_teal));
            binding.connectVpnBtn.setText("Tap to connect");
            binding.logoImageView.setImageDrawable(getResources().getDrawable(R.drawable.app_icon_transparent_teal));
        }
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    /**
     * Save current selected server on local shared preference
     */
    @Override
    public void onStop() {
        if (server != null) {
            preference.saveServer(server);
        }

        super.onStop();
    }
}
