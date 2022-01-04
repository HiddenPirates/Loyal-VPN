package com.hiddenpirates.umarvpn.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.Task;
import com.hiddenpirates.umarvpn.R;
import com.hiddenpirates.umarvpn.adapter.ServerListRVAdapter;
import com.hiddenpirates.umarvpn.interfaces.ChangeServer;
import com.hiddenpirates.umarvpn.interfaces.NavItemClickListener;
import com.hiddenpirates.umarvpn.model.Server;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import com.hiddenpirates.umarvpn.Utils;

public class MainActivity extends AppCompatActivity implements NavItemClickListener {

    private final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
    private Fragment fragment;
    private RecyclerView serverListRv;
    private ArrayList<Server> serverLists;
    public static DrawerLayout drawer;
    private ChangeServer changeServer;
    int UPDATE_REQUEST_CODE = 8348;
    public static AdRequest adRequest;
    public static RewardedAd mRewardedAd;
    public static InterstitialAd mInterstitialAd;
    boolean watched = false;
    public static final String TAG = "NURALAM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeAll();

        Log.d(TAG, "onCreate");

        ImageButton menuRight = findViewById(R.id.navbar_right);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);

        menuRight.setOnClickListener(v -> openCloseDrawer());

        transaction.add(R.id.container, fragment);
        transaction.commit();

        if (serverLists != null) {
            ServerListRVAdapter serverListRVAdapter = new ServerListRVAdapter(serverLists, this);
            serverListRv.setAdapter(serverListRVAdapter);
        }

//        -------------------------------------------------------------------------------------------
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {

                try {
                    appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE, this, UPDATE_REQUEST_CODE);
                }
                catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });
//        -------------------------------------------------------------------------------------------

        MobileAds.initialize(getApplicationContext(), initializationStatus -> {

            Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
            for (String adapterClass : statusMap.keySet()) {
                AdapterStatus status = statusMap.get(adapterClass);
                Log.d("MyApp", String.format(
                        "Adapter name: %s, Description: %s, Latency: %d",
                        adapterClass, status.getDescription(), status.getLatency()));
            }
        });

        adRequest = new AdRequest.Builder().build();

//        -------------------------------------------------------------------------------------------

        InterstitialAd.load(this, getResources().getString(R.string.admob_interstitial_ad), adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                Log.d(TAG, "interstitial");
            }
        });

//        -------------------------------------------------------------------------------------------

        loadNewRewardedAd();
    }

    /**
     * Initialize all object, listener etc
     */
    private void initializeAll() {
        drawer = findViewById(R.id.drawer_layout);

        fragment = new MainFragment();
        serverListRv = findViewById(R.id.serverListRv);
        serverListRv.setHasFixedSize(true);

        serverListRv.setLayoutManager(new LinearLayoutManager(this));

        serverLists = getServerList();
        changeServer = (ChangeServer) fragment;

    }

    /**
     * Close navigation drawer
     */
    public static void openCloseDrawer(){

        if (drawer.isDrawerOpen(GravityCompat.END)) {
            drawer.closeDrawer(GravityCompat.END);
        }
        else {
            drawer.openDrawer(GravityCompat.END);
        }
    }

    /**
     * Generate server array list
     */
    private ArrayList<Server> getServerList() {

        ArrayList<Server> servers = new ArrayList<>();

        servers.add(
                new Server("India (VIP)",
                        Utils.getImgURL(R.drawable.india),
                        "india.ovpn"
                )
        );
        servers.add(
                new Server("Singapore (VIP)",
                        Utils.getImgURL(R.drawable.singapore),
                        "singapore.ovpn"
                )
        );
        servers.add(
                new Server("France (VIP)",
                        Utils.getImgURL(R.drawable.france),
                        "france.ovpn"
                )
        );
        servers.add(
                new Server("United States (VIP)",
                        Utils.getImgURL(R.drawable.usa),
                        "usa.ovpn"
                )
        );
        servers.add(
                new Server("England (VIP)",
                        Utils.getImgURL(R.drawable.england),
                        "england.ovpn"
                )
        );
        servers.add(
                new Server("Japan",
                        Utils.getImgURL(R.drawable.japan),
                        "japan.ovpn"
                )
        );
        servers.add(
                new Server("South Korea",
                        Utils.getImgURL(R.drawable.korea),
                        "south-korea.ovpn"
                )
        );

        return servers;
    }

    /**
     * On navigation item click, close drawer and change server
     */
    @Override
    public void clickedItem(int index) {

        if (mRewardedAd != null){

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Connection Ready!");
            builder.setMessage("Watch a video ad to connect this server.");
            builder.setCancelable(false);

            builder.setPositiveButton("Watch Ad", (dialog, which) -> {

                MainActivity.mRewardedAd.show(MainActivity.this, rewardItem -> watched = true);

                MainActivity.mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                    @Override
                    public void onAdDismissedFullScreenContent() {

                        if (watched){

                            openCloseDrawer();
                            changeServer.newServer(serverLists.get(index));
                        }
                        else {
                            Toast.makeText(MainActivity.this, "You have not watched full video ad.", Toast.LENGTH_LONG).show();
                            openCloseDrawer();
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
            openCloseDrawer();
            changeServer.newServer(serverLists.get(index));
        }
    }

    /**
     * Loading new rewarded ad
     */
    public void loadNewRewardedAd(){

        RewardedAd.load(this, getResources().getString(R.string.admob_rewarded_ad),
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mRewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        mRewardedAd = rewardedAd;
                        Log.d(TAG, "rewarded");
                    }
                });
    }
}
