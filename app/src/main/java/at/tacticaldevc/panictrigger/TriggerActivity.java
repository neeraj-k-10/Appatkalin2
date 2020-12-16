package at.tacticaldevc.panictrigger;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import at.tacticaldevc.panictrigger.contactList.Contact;
import at.tacticaldevc.panictrigger.utils.Utils;

import static java.lang.Thread.sleep;

public class TriggerActivity extends AppCompatActivity implements View.OnClickListener, LocationListener {
    private Vibrator v;
    private Contact[] notifyContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trigger);

        v = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        String[] perms;
        if ((perms = Utils.checkPermissions(this)).length > 0)
            requestPermissions(perms, 255);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.triggerButton:

                this.v.vibrate(1000);
                final View content = getLayoutInflater().inflate(R.layout.content_dialog_trigger_group_select, null);
                final ArrayAdapter<String> ad = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, Utils.getContactGroups(this));
                ((Spinner) content.findViewById(R.id.emergency_group)).setAdapter(ad);

                new AlertDialog.Builder(this)
                        .setView(content)
                        .setPositiveButton("Trigger", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                notifyContacts = Utils.getContactsByGroup(((Spinner) content.findViewById(R.id.emergency_group)).getSelectedItem().toString(), TriggerActivity.this);
                                getCurrentLocationAndPanic();
                            }
                        })
                        .show();
                break;
            case R.id.configure:
                Intent settings = new Intent(this, SettingsActivity.class);
                startActivity(settings);
                break;
        }
    }

    private void sendOutPanic(Location loc) {
        String keyword = getSharedPreferences("conf", MODE_PRIVATE).getString(getString(R.string.var_words_keyword), "Panic");
        SmsManager manager = SmsManager.getDefault();

        if (callEmergServices()) {
            Intent emergService = new Intent(Intent.ACTION_CALL);
            emergService.setData(Uri.parse("tel:102"));
            startActivity(emergService);
        }

        else {
            for (Contact c : notifyContacts) {
                StringBuilder sb = new StringBuilder(keyword);
                if (loc != null) {
                    sb.append("\n" + loc.getLatitude() + "\n" + loc.getLongitude());
                    sb.append("\n " + "http://www.google.com/maps/place/" + loc.getLatitude() + "," + loc.getLongitude() + " \n");
                }

                TelephonyManager phoneMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                String wantPermission = Manifest.permission.READ_PHONE_STATE;
                if (this.checkSelfPermission(wantPermission) != PackageManager.PERMISSION_GRANTED) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(TriggerActivity.this)
                            .setTitle("The app will restart!")
                            .setMessage("It looks like not all permissions have been granted.\nPlease grant them or the app will not work!");
                    alertDialog.show();
                    try {
                        sleep(2000);
                        finish();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
//                String phonenumber = phoneMgr.getLine1Number();
//                Toast.makeText(this, "PhoneNumber: " + phonenumber, Toast.LENGTH_SHORT).show();

//                manager.sendTextMessage(c.number, phonenumber, sb.toString(), null, null);
                manager.sendTextMessage(c.number, null, sb.toString(), null, null);

                //make call to 102
                Intent emergService = new Intent(Intent.ACTION_CALL);
                emergService.setData(Uri.parse("tel:102"));
                startActivity(emergService);
            }
        }
    }

    private void getCurrentLocationAndPanic() {
        LocationManager locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            if (locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                locManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
            else if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                locManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
            else
                sendOutPanic(locManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER));
        } catch (Exception e) {
            Toast.makeText(this, "GPS fix could not be acquired. Please check your settings!", Toast.LENGTH_LONG).show();
            sendOutPanic(null);
        }
    }

    private boolean callEmergServices() {
        Set<String> contacts = getSharedPreferences("conf", MODE_PRIVATE).getStringSet(getString(R.string.var_numbers_notify), new HashSet<String>());
        return contacts.isEmpty();
    }

    @Override
    public void onLocationChanged(Location location) {
        sendOutPanic(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (!Utils.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
        if (requestCode == 255 && grantResults.length>0) {
            for (int grantResult : grantResults) {
                if(grantResult!=PackageManager.PERMISSION_GRANTED) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(TriggerActivity.this)
                            .setTitle("The app will restart!")
                            .setMessage("It looks like not all permissions have been granted.\nPlease grant them or the app will not work!");
                    alertDialog.show();
                    try {
                        sleep(2000);
                        this.finish();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            findViewById(R.id.triggerButton).setEnabled(false);
        }
//            }
    }
}
