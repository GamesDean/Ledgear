package com.menowattge.myled;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    // implementando le interfacce di Google Api Client posso costruire un oggetto che mi permette di
    // poter avviare il gps direttamente dall'applicazione senza dover costringere l'utente ad effettuare questa
    // operazione manualmente.

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    Button startScanningButton;
    Button stopScanningButton;
    Button connectButton;
    Button disconnectButton;
    Button sendButton;
    TextView peripheralTextView;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth Low Energy non supportato",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        //textview per visualizzare i dati,con scroll laterale. MEGLIO UNA LISTA
        peripheralTextView = (TextView) findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());

        //pulsanti sovrapposti per avviare e terminare la scansione
        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        //lo setto invisibile di default
        stopScanningButton.setVisibility(View.INVISIBLE);

        //pulsante per connettersi al dispositivo
        connectButton = (Button) findViewById(R.id.button2);

        //pulsante per disconnettersi dal dispositivo
        disconnectButton = (Button) findViewById(R.id.button3);

        //pulsante per inviare dati al dispositivo
        sendButton = (Button) findViewById(R.id.button4);


        //devo passare attraverso il manager e l'adapter per poter utilizzare lo scanner
        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        CheckPermission();

    }


    @Override
    protected void onResume() {
        super.onResume();
        //gps
        //genero a runtime un intero random per evitare il crash dovuto all'utilizzo dello stesso GoogleApiClientId
        Random rand = new Random();
        int maxGapiClientId = 50000;
        int minGapiClientId = 1;
        int gapiClientId = rand.nextInt(maxGapiClientId) + minGapiClientId;

        //creo l'oggetto poi lo passo al metodo sottostante che controlla lo stato del GPS
        GoogleApiClient gapiClient = setGoogleApiClient(gapiClientId);
        locationChecker(gapiClient, MainActivity.this);

        //bt
        ActivateBluetooth(btAdapter);

    }



    /*
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
                        INIZIO SEZIONE DEDICATA ALLE CONNESSIONI BLUETOOTH E GPS
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
     */


    // metodo che crea l'oggetto per poter comunicare all'utente di dover attivare il GPS internamente all'app
    public GoogleApiClient setGoogleApiClient(int gapiClientId) {

        GoogleApiClient mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .enableAutoManage(this, gapiClientId, this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        return mGoogleApiClient;
    }

    //metodo che propone all'utente di attivare il bt --onResume()--
    public void ActivateBluetooth(BluetoothAdapter btAdapter) {

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }

    //metodo che,all'installazione e primo avvio dell'app,chiede i permessi per poter utilizzare il GPS --onCreate()--
    public void CheckPermission() {

        if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    //   requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                }
            });
            builder.show();
        }
    }


    /**
     * Prompt user to enable GPS and Location Services
     *
     * @param mGoogleApiClient
     * @param activity
     */
    public static void locationChecker(GoogleApiClient mGoogleApiClient, final Activity activity) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);
        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    activity, 1000);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
    }


    // Override del metodo che gestisce i permessi accordati dall'utente
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Funzionalità Limitata");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }




    /*
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
                                    - FINE - SEZIONE DEDICATA ALLE CONNESSIONI BLUETOOTH E GPS
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
     */








    /*
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
                                            INIZIO SEZIONE DEDICATA ALLA SCANSIONE
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
     */

    // Device scan callback
    private ScanCallback leScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            String device = "\n" + "Device Name : " + result.getDevice().getName();
            String rssi = " - RSSI : " + result.getRssi() + "\n";
            String scanRecord = " \n- INFO PER ORA INUTILI -\n\n" + result.getScanRecord();
            String deviceAddress = "Device Address : " + result.getDevice().getAddress();

           // final BluetoothDevice btDevice = result.getDevice();
            // sembra indifferente l'utilizzo dell'uno o dell'altro
            final BluetoothDevice devProva = btAdapter.getRemoteDevice(result.getDevice().getAddress());

            peripheralTextView.setTextSize(18);
            peripheralTextView.setTextColor(Color.parseColor("#cc0000"));


            peripheralTextView.setText(device);
            peripheralTextView.append(rssi);
            peripheralTextView.append(deviceAddress);
            peripheralTextView.append("\n-------------------------------------------");
            peripheralTextView.append(scanRecord);
            peripheralTextView.append("\n-------------------------------------------");

            mGatt = null;
            // connectToDevice(btDevice);  mi connetto al click,non in automatico

            connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                          //  connectToDevice(btDevice);
                            connectToDevice(devProva);

                        }
                    });

                }
            });

            disconnectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                          //  BluetoothGatt btGatt = connectToDevice(btDevice);
                            BluetoothGatt btGatt = connectToDevice(devProva);

                            discConnectToDevice(btGatt);


                        }
                    });
                }
            });


        }


    };


    // da spostare sopra dove dichiaro le altre var ma per ora è qui per comodità
    private Handler handLer = new Handler();
    private static final long SCAN_PERIOD = 3000; // timeout 10 secondi - messo 3 per comodità


    public void startScanning() {

        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
        //se non trova nulla stampa a video
        peripheralTextView.setText("NO BLE DEVICES FOUND");

        // task che avvia la scansione
        AsyncTask.execute(new Runnable() {

            @Override
            public void run() {
                // aggiunto btScanner in questa posizione per evitare crash dopo avvio se bt == off
                btScanner = btAdapter.getBluetoothLeScanner();
                btScanner.startScan(leScanCallback);
            }
        });


        //maniglia che stoppa la scansione in automatico  dopo 10 secondi
        handLer.postDelayed(new Runnable() {
            @Override
            public void run() {

                btScanner.stopScan(leScanCallback);
                startScanningButton.setVisibility(View.VISIBLE);
                stopScanningButton.setVisibility(View.INVISIBLE);
                // da rimuovere e magari far capire con uno spinner
                peripheralTextView.append("\n \n ## Dopo 10 secondi la scansione si arresta ##".toUpperCase());

            }
        }, SCAN_PERIOD);

    }


    public void stopScanning() {

        peripheralTextView.append("\n\n" + "Stopped Scanning");
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });

    }


    // metodi delle interfacce

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }




    /*
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
                                            FINE SEZIONE DEDICATA ALLA SCANSIONE
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
     */




    private final static String TAG = MainActivity.class.getSimpleName();

    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic mWriteCharacteristic;


    // inizia la procedura di connessione
    public BluetoothGatt connectToDevice(BluetoothDevice device) {
      if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
      }
        return mGatt;  // AGGIUNTO OGGI COSI' LO PASSO A QUESTO QUI SOTTO
    }

    public void discConnectToDevice(BluetoothGatt mGatt) {
        mGatt.disconnect();
    }





    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {

                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(TAG, "Connected to GATT server.");
                    Log.i(TAG, "Attempting to start service discovery:" + mGatt.discoverServices());
                    Log.i("gattCallback", "STATE_CONNECTED");
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    Log.i(TAG, "Disconnected from GATT server.");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
           List<BluetoothGattService> services = gatt.getServices();
            // Log.i("onServicesDiscovered", services.toString());

           // gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));

            final UUID uuId = services.get(1).getUuid();
            final UUID charUuid = services.get(1).getCharacteristics().get(0).getUuid();
            final byte[] byteArray = {1,2,3};  //0001,0010,0011

            readCharacteristic(uuId,charUuid);

            sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    writeCharacteristic(uuId,charUuid,byteArray);
                }
            });

        }





        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());

            final byte[] characteristicValue = characteristic.getValue();
            final UUID uuId = characteristic.getUuid();


            try {
                System.out.println("CHAR-UUID : " + uuId);
                //permette di eseguire il task senza cagature di cazzo
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        peripheralTextView.append("\n\nCHARACTERISTIC : " + uuId);


                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
            // gatt.disconnect();



            // QUI GESTISCO L'INVIO DEI DATI HO COMMENTATO PERCHE VEDO SE USARE I METODI IN FONDO

    /*       sendButton.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View v) {

                   AsyncTask.execute(new Runnable() {
                       @Override
                       public void run() {


                           if (((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) |
                                   (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
                               // writing characteristic functions
                               mWriteCharacteristic = characteristic;
                           }



                           // "str" is the string or character you want to write

                           byte[] valueToWrite = mWriteCharacteristic.getValue();
                           mWriteCharacteristic.setValue(valueToWrite);

                           boolean successfullyWritten = gatt.writeCharacteristic(mWriteCharacteristic);


                           if (successfullyWritten) {
                               System.out.println("CHARACTERISTIC WRITTEN");

                           }


                       }
                   });


               }
           });
*/

        }




    };


//versione più ordinata di quanto eseguo grezzamente in onServiceDiscovered
    public boolean readCharacteristic(UUID gatservice_uuid, UUID char_uuid){


            try{
                //if(mBluetoothGatt==null || mBluetoothGattServiceList==null) return false;
                BluetoothGattService bgs = mGatt.getService(gatservice_uuid);
                if(bgs==null) return false;
                BluetoothGattCharacteristic bgc = bgs.getCharacteristic(char_uuid);
                if(bgc==null) return false;
                int properties = bgc.getProperties();
                if(((properties&BluetoothGattCharacteristic.PROPERTY_READ ) == BluetoothGattCharacteristic.PROPERTY_READ ))
                {
                    return mGatt.readCharacteristic(bgc);

                }else{
                    Log.e(gatservice_uuid+"->"+char_uuid," can not read !");
                    return false;
                }
            }catch(Exception ex){
                return false ;
            }

    }

    //versione ordinata di quanto gestisco con il tasto invio dati
    // vedere quale è migliore

    public boolean writeCharacteristic(UUID gatservice_uuid,UUID char_uuid,byte[] value){

            try{
                //if(mBluetoothGatt==null || mBluetoothGattServiceList==null) return false;
                BluetoothGattService bgs = mGatt.getService(gatservice_uuid);
                if(bgs==null){
                    Log.e("BGS:"+bgs+"->"," can not find ! write error");
                    return false;
                }
                BluetoothGattCharacteristic bgc = bgs.getCharacteristic(char_uuid);
                    Log.i("BGS","OK");
                if(bgc==null) {
                    Log.e("BGC:"+bgc+"->"," can not find ! write error");
                    return false;
                }
                int properties = bgc.getProperties();
                    Log.i("BGC","OK");
                if( ( ( properties&BluetoothGattCharacteristic.PROPERTY_WRITE ) == BluetoothGattCharacteristic.PROPERTY_WRITE )
                        || ( ( properties&BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE ) == BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE )
                        || ( ( properties&BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE ) == BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE ) )
                {
                    bgc.setValue(value);
                    Log.i("WRITE","done");
                    return mGatt.writeCharacteristic(bgc);


                }else{
                    Log.e(gatservice_uuid+"->",char_uuid+" can not write !");
                    return false;
                }
            }catch(Exception ex){
                    Log.e("WRITECHAR_ERROR","writeCharacteristic mBluetoothGatt dead ");

                return false ;
            }
        }





}