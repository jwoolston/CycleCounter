package com.realenvprod.cyclecounter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import com.realenvprod.cyclecounter.counter.Counter;
import com.realenvprod.cyclecounter.fragment.AddCounterFragment;
import com.realenvprod.cyclecounter.fragment.KnownCounterFragment;
import com.realenvprod.cyclecounter.fragment.KnownCounterFragment.OnCounterListItemInteractionListener;
import com.realenvprod.cyclecounter.fragment.SensorMapFragment;
import com.realenvprod.cyclecounter.fragment.UnknownCounterFragment;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                   OnCounterListItemInteractionListener {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        showKnownCounterList();

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    protected void onResume() {
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        // Register for scan results
        EventBus.getDefault().register(this);

        super.onResume();
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_known_devices:
                showKnownCounterList();
                break;
            case R.id.nav_unknown_devices:
                showUnknownCounterList();
                break;
            case R.id.nav_map:
                showMap();
                break;
            case R.id.nav_reports:
            case R.id.nav_settings:
            case R.id.nav_send:
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showKnownCounterList() {
        Log.d(TAG, "Showing known sensor list.");
        Fragment fragment = KnownCounterFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    private void showUnknownCounterList() {
        Log.d(TAG, "Showing unknown sensor list.");
        Fragment fragment = UnknownCounterFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    private void showMap() {
        Log.d(TAG, "Showing map fragment.");
        final Fragment fragment = new SensorMapFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    private void showAddCounterFragment(@NonNull Counter counter) {
        Log.d(TAG, "Showing add counter fragment.");
        final Fragment fragment = AddCounterFragment.newInstance(counter);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }

    @Override
    public void onCounterListItemInteraction(Counter item) {

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCounterDiscovered(final Counter counter) {
        if (!counter.isKnown) {
            Log.d(TAG, "Received counter discovery: " + counter);
            AlertDialog.Builder builder = new Builder(this);
            builder.setTitle("New Cycle Sensor Discovered.");
            builder.setMessage("A new cycle sensor has been discovered. Would you like to add it to the database?");
            builder.setCancelable(true).setPositiveButton("YES", new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    showAddCounterFragment(counter);
                }
            }).setNegativeButton("NO", null);
            builder.show();
        }
    }
}
