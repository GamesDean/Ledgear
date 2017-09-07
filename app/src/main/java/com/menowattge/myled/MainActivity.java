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
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import pl.bclogic.pulsator4droid.library.PulsatorLayout;

import static com.menowattge.myled.R.id.fragment_container;


public  class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, CharacteristicFragment.OnFragmentInteractionListener

{

    // implementando le interfacce di Google Api Client posso costruire un oggetto che mi permette di
    // poter avviare il gps direttamente dall'applicazione senza dover costringere l'utente ad effettuare questa
    // operazione manualmente.
    Fragment covEr;
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;

    //sono due, uno grande iniziale, l'altro piccolo, sopra la listview
    static PulsatorLayout startScanningButton;
    static PulsatorLayout startScanningButtonBig;
    ProgressBar progressBar;



    TextView bleDevices;
    TextView counter;
    ListView listView;
    ArrayAdapter adapter;
    protected  ArrayList<String>recentlySeen = new ArrayList<>();
    protected  ArrayList<String>rssiList = new ArrayList<>();

    private boolean stoP=true;
    private boolean starT=false;
    private int i=0;
    protected String rssi = "";
    private String device = "";
    private Handler handLer = new Handler();
    private static final int SCAN_PERIOD = 5000;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    protected byte[] byteValue;
    // li uso poi nel fragment
    protected BluetoothDevice deviceAddress;



    @Override
    public void onBackPressed() {
        super.onBackPressed();
        final Animation animScaleUp = android.view.animation.AnimationUtils.loadAnimation(startScanningButton.getContext(),  R.anim.scale_up);
        getFragmentManager().popBackStack();
        startScanningButton.setAnimation(animScaleUp);
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
        //casella di testo che mostra a video un avviso se non vengono rilevati dispositivi
        bleDevices = (TextView) findViewById(R.id.textView);
        counter = (TextView) findViewById(R.id.counter);


        startScanningButton = (PulsatorLayout) findViewById(R.id.StartScanButton);
        startScanningButton.setEnabled(false);

        startScanningButtonBig = (PulsatorLayout)findViewById(R.id.StartScanButtonBig);
        startScanningButtonBig.start();

        final Animation animScaleUpBig = android.view.animation.AnimationUtils.loadAnimation(startScanningButtonBig.getContext(),  R.anim.scale_up);

        //imposto l'animazione
        startScanningButtonBig.startAnimation(animScaleUpBig);


        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
                startScanningButton.setVisibility(View.INVISIBLE);
            }
        });

        startScanningButtonBig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScanning();
                startScanningButtonBig.setVisibility(View.INVISIBLE);
            }
        });

        //devo passare attraverso il manager e l'adapter per poter utilizzare lo scanner
        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        CheckPermission();

        listView.setVisibility(View.INVISIBLE);

        progressBar = (ProgressBar)findViewById(R.id.progressBar2);
        progressBar.setVisibility(View.INVISIBLE);




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
        public void onScanResult(int callbackType, final ScanResult result) {

            super.onScanResult(callbackType,result);

            String name = "Device Name : ";
            device = name + result.getDevice().getName();
            rssi = " -- RSSI : " + result.getRssi() + "\n";


            deviceAddress = btAdapter.getRemoteDevice(result.getDevice().getAddress());

            if(device!=""|| !device.equals(null)||!device.isEmpty()||!device.equals("null")) {
                recentlySeen.add(device);
                //recentlySeen.add(rssi);
            }

            Set<String> noDuplicates = new LinkedHashSet<String>(recentlySeen);
            recentlySeen.clear();
            recentlySeen.addAll(noDuplicates);




            System.out.println("lista :"+recentlySeen);




           // final BluetoothDevice btDevice = result.getDevice();
            // sembra indifferente l'utilizzo dell'uno o dell'altro

            // TODO : cambiare nome a deviceAddress





           System.out.println("deviceAddress : "+deviceAddress);




            if(!device.isEmpty()) {
               // imposta il testo a vuoto
                //TODO : indeciso se far comparire la scritta "rilevati dispositivi"
                bleDevices.setText("");

                handLer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //svuoto i valori di "rssi" al termine della scansione per poter effettuare i controlli allo start
                        rssi ="";
                    }
                }, SCAN_PERIOD);
            }


            //fondamentale
            mGatt = null;

            //animazione pulsante piccolo
            final Animation animScaleDown = android.view.animation.AnimationUtils.loadAnimation(startScanningButton.getContext(),  R.anim.scale_down);


            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                 //   if(device!=null) {
                        covEr = new CharacteristicFragment();
                        FragmentTransaction transaction=getFragmentManager().beginTransaction();
                        //animazione per il fragment
                        transaction.setCustomAnimations(android.R.animator.fade_in,android.R.animator.fade_out);
                        // con replace sostituisco il layout del main con quello del fragment
                        transaction.replace(fragment_container,covEr);
                        // premendo il tasto back, torno al main ed il fragment viene tolto dallo stack
                        transaction.addToBackStack(null);
                        transaction.commit();

                        //nel dubbio disabilito i pulsanti, altrimenti si  creano conflitti
                        startScanningButtonBig.setEnabled(false);
                        startScanningButton.setEnabled(false);
                        //animazione che rende non visibile il pulsante piccolo
                        startScanningButton.startAnimation(animScaleDown);

                  //  }
                 /*   else{
                        Toast.makeText(getApplicationContext(),"dispositivo non valido",Toast.LENGTH_SHORT).show();
                    }
                    */


                }
            });


        }
    };



     CountDownTimer countdown = null;
    // contatore,appare un countdown di 4 sec


    public void counTer(boolean flaG) {
    //se passo start(false)

        final Animation animFadeInList = android.view.animation.AnimationUtils.loadAnimation(listView.getContext(),  android.R.anim.fade_in);
        final Animation animScaleUp = android.view.animation.AnimationUtils.loadAnimation(startScanningButton.getContext(),  R.anim.scale_up);
        final Animation progressOut = android.view.animation.AnimationUtils.loadAnimation(progressBar.getContext(),  android.R.anim.fade_out);

// TODO : probabile le istruzioni condizionali ed il parametro booleano non servano più - passare le animazioni come parametro se possibile

        if(!flaG) {

            countdown = new CountDownTimer(SCAN_PERIOD, 1000) {

                public void onTick(long millisUntilFinished) {
                    counter.setText("wait: " + millisUntilFinished / 1000);
                    //durante la scansione, lista invisibile
                    listView.setVisibility(View.INVISIBLE);
                    //torna visibile
                    progressBar.setVisibility(View.VISIBLE);

                }

                public void onFinish() {
                    // al termine del countdown,la progress bar circolare non è più visibile
                    progressBar.setAnimation(progressOut);
                    //deve comunque essere presente altrimenti l'animazione non è stabile
                    progressBar.setVisibility(View.INVISIBLE);
                    // al termine del countdown, appaiono con un'animazione il pulsante scan(little) e la listview
                    startScanningButton.setAnimation(animScaleUp);
                    startScanningButton.setVisibility(View.VISIBLE);
                    listView.setAnimation(animFadeInList);
                    listView.setVisibility(View.VISIBLE);
                    counter.setText("");

                   for(Object item : recentlySeen){
                       System.out.println("recently : "+item);
                       adapter.add(item);
                       listView.setAdapter(adapter);
                   }


                   // adapter.clear();

                }


            }.start();

        }
        // TODO : eliminare dato che stop non c'è più
        //se passo stop(true)
        else if (flaG){
            countdown.cancel();
            countdown.onFinish();
        }


    }




    public void startScanning() {


        //avvia il contatore
        counTer(starT);

        //alla pressione del tasto "scan" in primis ripulisco l'adapter e la lista dei recenti
        adapter.clear();
        recentlySeen.clear();

        //tengo traccia delle iterazioni della callback DEBUG
        //i++;

        //eseguo il controllo basandomi sull'rssi : se non presente,non ci sono device nel range
        if (rssi.isEmpty()) {

            bleDevices.setText("nessun dispostivo rilevato");


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


        //maniglia che stoppa la scansione in automatico  dopo 4 secondi
        handLer.postDelayed(new Runnable() {
            @Override
            public void run() {

                btScanner.stopScan(leScanCallback);
                startScanningButton.setEnabled(true);
                startScanningButton.start();


            }
        }, SCAN_PERIOD);

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

                final CharacteristicFragment fragment = (CharacteristicFragment) fm.findFragmentById(fragment_container);
                //animazione pulsante invia
                final Animation anim = android.view.animation.AnimationUtils.loadAnimation(fragment.invia.getContext(),  R.anim.shake);
                final Animation animDue = android.view.animation.AnimationUtils.loadAnimation(fragment.invia.getContext(),  R.anim.scale_up);

                //suggerimento dello spinner
                fragment.spinnerDue.setPrompt(fragment.selezionaProgramma);

                fragment.spinnerDue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String item = (String) fragment.adapter.getItem(position);

                        //lo rendo cliccabile
                      //  fragment.invia.setEnabled(true);
                        //reimposto il colore pieno rendendolo nuovamente ben visibile
                       // fragment.invia.setAlpha(1f);

                        //per farlo fare solo alla prima iterazione
                        int x=0;
                        if(x==0) {
                            fragment.invia.startAnimation(animDue);
                            fragment.invia.setVisibility(View.VISIBLE);
                            fragment.textView.setVisibility(View.VISIBLE);

                        }
                        x++;

                        //azzero la barra circolare del progresso
                        fragment.invia.setProgress(0);
                        //ad ogni selezione parte l'animazione che fa capire di dover cliccare il pulsante
                        fragment.invia.startAnimation(anim);

                        if(item.equals(fragment.programmaUno)){byteValue = new byte[]{1};}
                        else if(item.equals(fragment.programmaDue)){byteValue = new byte[]{2};}
                        else if(item.equals(fragment.programmaTre)){byteValue = new byte[]{3};}
                        else if(item.equals(fragment.programmaQuattro)){byteValue = new byte[]{4};}
                        else if(item.equals(fragment.programmaCinque)){byteValue = new byte[]{5};}
                        else if(item.equals(fragment.programmaSei)){byteValue = new byte[]{6};}
                        else if(item.equals(fragment.programmaSette)){byteValue = new byte[]{7};}
                        else if(item.equals(fragment.programmaOtto)){byteValue = new byte[]{8};}
                        else if(item.equals(fragment.programmaNove)){byteValue = new byte[]{9};}
                        else if(item.equals(fragment.programmaDieci)){byteValue = new byte[]{10};}
                        else if(item.equals(fragment.programmaUndici)){byteValue = new byte[]{11};}
                        else if(item.equals(fragment.programmaDodici)){byteValue = new byte[]{12};}
                        else if(item.equals(fragment.programmaTredici)){byteValue = new byte[]{13};}
                        else if(item.equals(fragment.programmaQuattordici)){byteValue = new byte[]{14};}
                        else if(item.equals(fragment.programmaQuindici)){byteValue = new byte[]{15};}
                        else if(item.equals(fragment.programmaSedici)){byteValue = new byte[]{16};}
                        else if(item.equals(fragment.programmaDiciassette)){byteValue = new byte[]{17};}
                        else if(item.equals(fragment.programmaDiciotto)){byteValue = new byte[]{18};}
                        else if(item.equals(fragment.programmaDiciannove)){byteValue = new byte[]{19};}


                        fragment.invia.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                fragment.invia.setProgress(100);


                                writeCharacteristic(uuId,charUuid,byteValue);

                                Toast.makeText(getApplicationContext(),
                                        "ho inviato il numero : "+byteValue[0] ,Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

        }




        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,  final BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());

            try {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

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

        }





    };





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
