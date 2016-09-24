package com.topjohnwu.magisk;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.topjohnwu.magisk.services.MonitorService;
import com.topjohnwu.magisk.utils.Async;
import com.topjohnwu.magisk.utils.Logger;
import com.topjohnwu.magisk.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WelcomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String SELECTED_ITEM_ID = "SELECTED_ITEM_ID";

    private final Handler mDrawerHandler = new Handler();
    private String currentTitle;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;
    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @IdRes
    private int mSelectedId = R.id.magisk;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        ButterKnife.bind(this);

        // Startups
        PreferenceManager.setDefaultValues(this, R.xml.defaultpref, false);
        if (Utils.autoToggleEnabled(getApplicationContext())) {
            if (!Utils.isMyServiceRunning(MonitorService.class, getApplicationContext())) {
                Intent myIntent = new Intent(getApplication(), MonitorService.class);
                getApplication().startService(myIntent);
            }
        }
        Utils.SetupQuickSettingsTile(getApplicationContext());

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }

        this.getFragmentManager().addOnBackStackChangedListener(
                () -> {
                    Fragment hm = getFragmentManager().findFragmentByTag("root");
                    if (hm != null) {
                        if (hm.isVisible()) {
                            navigationView.setCheckedItem(R.id.root);
                        }
                    }
                    hm = getFragmentManager().findFragmentByTag("autoroot");
                    if (hm != null) {
                        if (hm.isVisible()) {
                            navigationView.setCheckedItem(R.id.autoroot);
                        }
                    }
                    hm = getFragmentManager().findFragmentByTag("magisk");
                    if (hm != null) {
                        if (hm.isVisible()) {
                            navigationView.setCheckedItem(R.id.magisk);
                        }
                    }
                    hm = getFragmentManager().findFragmentByTag("modules");
                    if (hm != null) {
                        if (hm.isVisible()) {
                            navigationView.setCheckedItem(R.id.modules);
                        }
                    }
                    hm = getFragmentManager().findFragmentByTag("downloads");
                    if (hm != null) {
                        if (hm.isVisible()) {
                            navigationView.setCheckedItem(R.id.downloads);
                        }
                    }
                    hm = getFragmentManager().findFragmentByTag("log");
                    if (hm != null) {
                        if (hm.isVisible()) {
                            navigationView.setCheckedItem(R.id.log);
                        }
                    }
                    hm = getFragmentManager().findFragmentByTag("settings");
                    if (hm != null) {
                        if (hm.isVisible()) {
                            navigationView.setCheckedItem(R.id.settings);
                        }
                    }
                });

        Utils.init(this);
        new Async.CheckUpdates(this).execute();
        new Async.LoadModules(this).execute();
        new Async.LoadRepos(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                super.onDrawerSlide(drawerView, 0); // this disables the arrow @ completed tate
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, 0); // this disables the animation
            }
        };

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //noinspection ResourceType
        mSelectedId = savedInstanceState == null ? mSelectedId : savedInstanceState.getInt(SELECTED_ITEM_ID);
        navigationView.setCheckedItem(mSelectedId);

        if (savedInstanceState == null) {
            mDrawerHandler.removeCallbacksAndMessages(null);
            mDrawerHandler.postDelayed(() -> navigate(mSelectedId), 250);
        }

        navigationView.setNavigationItemSelectedListener(this);
        if (getIntent().hasExtra("relaunch")) {
            navigate(R.id.root);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SELECTED_ITEM_ID, mSelectedId);
    }

    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            int backStackEntryCount = getFragmentManager().getBackStackEntryCount();
            Logger.dh("Welcomeactivity: Entrycount is " + backStackEntryCount);
            if (backStackEntryCount >= 2) {
                super.onBackPressed();
            } else {
                finish();
            }
        }

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem menuItem) {
        mSelectedId = menuItem.getItemId();
        mDrawerHandler.removeCallbacksAndMessages(null);
        mDrawerHandler.postDelayed(() -> navigate(menuItem.getItemId()), 250);

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle(currentTitle);
    }

    public void navigate(final int itemId) {
        Fragment navFragment = null;
        String tag = "";
        switch (itemId) {
            case R.id.magisk:
                setTitle(R.string.magisk);
                tag = "magisk";
                navFragment = new MagiskFragment();
                break;
            case R.id.root:
                setTitle(R.string.root);
                tag = "root";
                navFragment = new RootFragment();
                break;
            case R.id.autoroot:
                setTitle(R.string.auto_toggle_apps);
                tag = "autoroot";
                navFragment = new AutoRootFragment();
                break;
            case R.id.modules:
                setTitle(R.string.modules);
                tag = "modules";
                navFragment = new ModulesFragment();
                break;
            case R.id.downloads:
                setTitle(R.string.downloads);
                tag = "downloads";
                navFragment = new ReposFragment();
                break;
            case R.id.log:
                setTitle(R.string.log);
                tag = "log";
                navFragment = new LogFragment();
                break;
            case R.id.settings:
                setTitle(R.string.settings);
                tag = "settings";
                navFragment = new SettingsFragment();
                break;
            case R.id.app_about:
                startActivity(new Intent(this, AboutActivity.class));
                return;
        }

        if (navFragment != null) {

            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
            try {
                toolbar.setElevation(navFragment instanceof ModulesFragment ? 0 : 10);
                currentTitle = getTitle().toString();

                transaction.replace(R.id.content_frame, navFragment, tag).addToBackStack(currentTitle).commit();
            } catch (IllegalStateException ignored) {
            }
        }
    }
}