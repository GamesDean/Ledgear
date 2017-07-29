package com.menowattge.myled;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.menowattge.myled.R.id.fragment_container;
import static com.menowattge.myled.R.id.fragment_preset;


public  class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, CharacteristicFragment.OnFragmentInteractionListener,
PresetFragment.OnFragmentInteractionListener

{

    // implementando le interfacce di Google Api Client posso costruire un oggetto che mi permette di
    // poter avviare il gps direttamente dall'applicazione senza dover costringere l'utente ad effettuare questa
    // operazione manualmente.
    Fragment covEr;
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    static Button startScanningButton;
    static Button stopScanningButton;
    Button connectButton;
    Button disconnectButton;
    Button sendButton;

    TextView peripheralTextView;

    TextView bleDevices;
    TextView counter;
    ListView listView;
    ArrayAdapter adapter;
    ArrayList recentlySeen;
    private boolean stoP=true;
    private boolean starT=false;
    private int i=0;
    private String rssi = "";
    private String device = "";
    private Handler handLer = new Handler();
    private static final int SCAN_PERIOD = 5000;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    private byte[] byteProva;
    // li uso poi nel fragment
    protected BluetoothDevice devProva=null;



    @Override
    public void onAttachFragment(final Fragment fragment) {
        // TODO Auto-generated method stub
        super.onAttachFragment(fragment);


        System.out.println("FRAGMENT ID "+String.valueOf(fragment.getId()));


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        getFragmentManager().popBackStack();
        startScanningButton.setVisibility(View.VISIBLE);
        //startScanning();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth Low Energy non supportato",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        // lista che mostrerà a video i risultati della scansione
        listView = (ListView) findViewById(R.id.listView);
        adapter=new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        bleDevices = (TextView) findViewById(R.id.textView);
        counter = (TextView) findViewById(R.id.counter);
        //textview per visualizzare i dati,con scroll laterale. MEGLIO UNA LISTA
      //  peripheralTextView = (TextView) findViewById(R.id.PeripheralTextView);
     //   peripheralTextView.setMovementMethod(new ScrollingMovementMethod());

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


      //  connectButton = (Button) findViewById(R.id.button2);

        //pulsante per disconnettersi dal dispositivo
       // disconnectButton = (Button) findViewById(R.id.button3);

        //pulsante per inviare dati al dispositivo
       // sendButton = (Button) findViewById(R.id.button4);


        //devo passare attraverso il manager e l'adapter per poter utilizzare lo scanner
        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        CheckPermission();

        listView.setVisibility(View.INVISIBLE);

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
                    builder.setMessage("Permessi non concessi, l'app non sarà in grado di scandire la rete e trovare dispositivi in background.");
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
            String name = "Device Name : ";
            device = "\n" + name + result.getDevice().getName();
            rssi = " -- RSSI : " + result.getRssi() + "\n";



           // final BluetoothDevice btDevice = result.getDevice();
            // sembra indifferente l'utilizzo dell'uno o dell'altro
            devProva = btAdapter.getRemoteDevice(result.getDevice().getAddress());



            //aggiungo nuovi device ai recenti,ad ogni iterazione della callback
            recentlySeen = new ArrayList();
            recentlySeen.add(device+rssi);
            // recentlySeen.add(rssi);

          //  System.out.println("RSSI : "+rssi);
           // System.out.println("ITERAZIONI : "+i);

            //aggiungo agli ufficiali alla prima iterazione o se l'adapter è vuoto
            if (i==0 || adapter.isEmpty()) {
                adapter.add(device+rssi);
               // adapter.add(rssi);
                listView.setAdapter(adapter);
            }

            //successive iterazioni se NON già presente nell'adapter,lo aggiungo
            if (!(adapter.equals(recentlySeen.contains(device))) ){
                    listView.setAdapter(adapter);
                }
            //
            if(!device.isEmpty()) {
                bleDevices.setText("Devices Found");

                handLer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bleDevices.setText("Press scan");
                        //svuoto i valori di "rssi" per poter effettuare i controlli allo start
                        rssi ="";
                    }
                }, SCAN_PERIOD);
            }

            //fondamentale
            mGatt = null;





            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    covEr = new CharacteristicFragment();
                    FragmentTransaction transaction=getFragmentManager().beginTransaction();
                    transaction.replace(fragment_container,covEr); // give your fragment container id in first parameter
                    transaction.addToBackStack(null);  // if written, this transaction will be added to backstack
                    transaction.commit();

                    startScanningButton.setVisibility(View.INVISIBLE);



                }
            });







            // connectToDevice(btDevice);  mi connetto al click,non in automatico

       /*

       //COMMENTO I PULSANTI,CIO' CHE AVVIENE QUI VERRA' GESTITO NELLA LISTA O AL DROPDOWN O IN UN'ALTRA ACTIVITY

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
        */

        }


    };




     CountDownTimer countdown = null;

    // contatore,appare un countdown di 10 sec
    public void counTer(boolean flaG) {
    //se passo start(false)
        if(!flaG) {

            countdown = new CountDownTimer(SCAN_PERIOD, 1000) {

                public void onTick(long millisUntilFinished) {
                    counter.setText("wait: " + millisUntilFinished / 1000);
                    //così evito il click e la questione del tasto che compare
                    listView.setVisibility(View.INVISIBLE);

                }

                public void onFinish() {
                    counter.setText("ready");
                    listView.setVisibility(View.VISIBLE);

                }


            }.start();

        }
        //se passo stop(true)
        else if (flaG){
            countdown.cancel();
            countdown.onFinish();
        }


    }




    public void startScanning() {

        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);

        //avvia il contatore
        counTer(starT);

        //alla pressione del tasto "scan" in primis ripulisco l'adapter
        adapter.clear();

        //tengo traccia delle iterazioni della callback DEBUG
        i++;

        //eseguo il controllo basandomi sull'rssi : se non presente,non ci sono device nel range
        if (rssi.isEmpty()) {
            bleDevices.setText("No BLE Devices");
            handLer.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bleDevices.setText("Press scan");

                }
            },SCAN_PERIOD);
        }


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

            }
        }, SCAN_PERIOD);

    }

    // TODO : !! DA ELIMINARE !!

    public void stopScanning() {

        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);

        //uso thread asincrono
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
                //innesto i thread,solo quello della UI può modificare gli oggetti a video
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bleDevices.setText("Press scan");
                        //stoppo il contatore
                        counTer(stoP);

                    }
                });



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

    /*
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
                                            INIZIO SEZIONE DEDICATA ALLA CONNESSIONE
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
     */


    private final static String TAG = MainActivity.class.getSimpleName();

    protected BluetoothGatt mGatt;
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


    //
    public UUID uuId,charUuid;
    byte[] byteArray = {1,2,3};


    protected final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
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
            public void onServicesDiscovered ( final BluetoothGatt gatt, int status){
                List<BluetoothGattService> services = gatt.getServices();
                // Log.i("onServicesDiscovered", services.toString());

                // gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));


                uuId = services.get(1).getUuid();
                charUuid = services.get(1).getCharacteristics().get(0).getUuid();

                readCharacteristic(uuId, charUuid);

                //ottengo un'istanza del fragment
                android.app.FragmentManager fm = getFragmentManager();

                CharacteristicFragment fragment = (CharacteristicFragment) fm.findFragmentById(fragment_container);

                //OLD--al tap del testo (che mostra per ora l' UUID del device) invio un array di byte
                //NEW--al tap avvia un nuovo fragment dove gestirò l'invio dei byte

                fragment.fragmentTextView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        // operazione equivalente all'uso degli intent con le activity
                        covEr = new PresetFragment();
                        FragmentTransaction transaction=getFragmentManager().beginTransaction();
                        transaction.replace(fragment_preset,covEr);
                        transaction.addToBackStack(null);
                        transaction.commit();

                       // writeCharacteristic(uuId,charUuid,byteArray);
                    }
                });


                PresetFragment presetFragment = (PresetFragment) fm.findFragmentById(fragment_preset);
                presetFragment.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String item = (String) adapter.getItem(position);
                        Toast.makeText(
                                getApplicationContext(),
                                "hai selezionato "+ item,
                                Toast.LENGTH_LONG).show();

                        Log.i("TOTTA","selezionato");

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

/*
                presetFragment.buttonSend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        writeCharacteristic(uuId,charUuid,byteProva);
                        System.out.println("invio riuscito");
                    }
                });
*/





       /*     sendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    writeCharacteristic(uuId,charUuid,byteArray);
                }
            });
       */




        }




        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,  final BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());

            final byte[] characteristicValue = characteristic.getValue();



            try {
              //  System.out.println("CHAR-UUID : " + uuId);
                //permette di modificare la ui



                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                      //  peripheralTextView.append("\n\nCHARACTERISTIC : " + uuId);

                        //ottengo un'istanza del fragment
                        android.app.FragmentManager fm = getFragmentManager();

                        CharacteristicFragment fragment = (CharacteristicFragment)fm.findFragmentById(fragment_container);
                        UUID  uuId = characteristic.getUuid();
                        //passo l'uuid al metodo definito del fragment
                        fragment.showUuid(uuId);

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




    @Override
    public void onFragmentInteraction(Uri uri) {

    }


}
