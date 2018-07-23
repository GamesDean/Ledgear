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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
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

import es.dmoral.toasty.Toasty;
import pl.bclogic.pulsator4droid.library.PulsatorLayout;

import static com.menowattge.myled.R.id.fragment_container;


public  class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, CharacteristicFragment.OnFragmentInteractionListener

{

    /*
    *
    * Implementando le interfacce di Google Api Client posso costruire un oggetto che mi permette di
    * poter avviare il gps direttamente dall'applicazione senza dover costringere l'utente ad effettuare questa
    * operazione manualmente.
    *
    */


    Fragment covEr;
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;

    static PulsatorLayout startScanningButton;
    static PulsatorLayout startScanningButtonBig;
    ProgressBar progressBar;

    TextView bleDevices;
    TextView counter;
    ListView listView;
    ArrayAdapter adapter;
    protected  ArrayList<String>recentlySeen = new ArrayList<>();
    protected  ArrayList<String>rssiList = new ArrayList<>();

    protected String rssi = "";
    private String device = "";
    private Handler handLer = new Handler();
    private static final int SCAN_PERIOD = 5000;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 1;
    protected byte[] byteValue ;
    protected byte programma=0;
    protected byte potenza=0;

    // li uso nel fragment
    protected BluetoothDevice deviceAddress;
    protected String noDev = "nessun dispostivo rilevato";

    private final static String TAG = MainActivity.class.getSimpleName();
    protected BluetoothGatt mGatt;
    public UUID uuId,charUuid;
    protected int x=0;
    protected int j=0;

    private final static UUID BATTERY_UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private final static UUID BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    private Handler mHandler;


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        final Animation animScaleUp = android.view.animation.AnimationUtils.loadAnimation(startScanningButton.getContext(),  R.anim.scale_up);
        getFragmentManager().popBackStack();
        startScanningButton.setAnimation(animScaleUp);
        startScanningButton.setVisibility(View.VISIBLE);
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

        adapter=new ArrayAdapter<>(this, R.layout.litst_text);
        // casella di testo che mostra a video un avviso se non vengono rilevati dispositivi
        bleDevices = (TextView) findViewById(R.id.textView);
        counter = (TextView) findViewById(R.id.counter);

        startScanningButton = (PulsatorLayout) findViewById(R.id.StartScanButton);
        startScanningButton.setEnabled(false);

        startScanningButtonBig = (PulsatorLayout)findViewById(R.id.StartScanButtonBig);
        startScanningButtonBig.start();

        final Animation animScaleUpBig = android.view.animation.AnimationUtils.loadAnimation(startScanningButtonBig.getContext(),  R.anim.scale_up);

        // imposto l'animazione
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
                bleDevices.clearAnimation();
                bleDevices.setText("");
                startScanning();
                startScanningButtonBig.setVisibility(View.INVISIBLE);
            }
        });

        //devo passare attraverso il manager e l'adapter per poter utilizzare lo scanner
        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();

        //CheckPermission();

        listView.setVisibility(View.INVISIBLE);

        progressBar = (ProgressBar)findViewById(R.id.progressBar2);
        progressBar.setVisibility(View.INVISIBLE);

        // animazione che fa lampeggiare la scritta
        bleDevices.setText("premi scan");
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(500); // intervallo di tempo del lampeggiamento
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        bleDevices.startAnimation(anim);

    }




    @Override
    protected void onResume() {
        super.onResume();
        // gps
        // genero a runtime un intero random per evitare il crash dovuto all'utilizzo dello stesso GoogleApiClientId
        Random rand = new Random();
        int maxGapiClientId = 50000;
        int minGapiClientId = 1;
        int gapiClientId = rand.nextInt(maxGapiClientId) + minGapiClientId;

        // creo l'oggetto poi lo passo al metodo sottostante che controlla lo stato del GPS
        GoogleApiClient gapiClient = setGoogleApiClient(gapiClientId);

       // if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        CheckPermission(gapiClient);



        // bt
        ActivateBluetooth(btAdapter);

    }



    /*
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
                                    -- INIZIO SEZIONE DEDICATA ALLE CONNESSIONI BLUETOOTH E GPS --
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
     */



    /**
     *  Creates the GAPI Client Object for let the user toggle the GPS on inside the application
     *
     * @param gapiClientId
     * @return
     */
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

    /**
     * Prompt user to enable the Bluetooth
     * @param  btAdapter
     */
    public void ActivateBluetooth(BluetoothAdapter btAdapter) {

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
    }


    /**
     * Ask for GPS permission, just once (first install only)
     */

    public void CheckPermission(final GoogleApiClient gapiClient) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (!prefs.contains("FirstTime")) {
            // if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            builder.setTitle("INFO Geolocalizzazione");
            builder.setMessage(" In applicazione del Regolamento generale sulla protezione dei dati (GDPR)" +
                    " del 27 aprile 2016 si dichiara all’utilizzatore dell’app, denominata LEDGEAR, che nessun " +
                    "dato personale verrà archiviato e/o trasferito e/o sarà oggetto di proliferazione." +
                    " Si dichiara, inoltre, che nessun dato geografico verrà archiviato e/o trasferito e/o" +
                    " sarà oggetto di proliferazione." +
                    "Si ricorda che l’uscita dall’app Ledgear rende non più necessario l’uso del Bluetooth e del circuito GPS: " +
                    "per risparmiare energia si consiglia di disattivarli entrambi");
            builder.setPositiveButton(R.string.ho_letto, null);



            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);

                    locationChecker(gapiClient, MainActivity.this);

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("FirstTime",true);
                    editor.commit();
                    System.out.println("PRIMO AVVIO\n");

                }
            });

            builder.show();
        }else{
            System.out.println("SUCCESSIVI AVVII_\n");
            locationChecker(gapiClient, MainActivity.this);
        }

    }





    /**
     * Prompt user to enable GPS and Location Services
     *
     * @param mGoogleApiClient
     * @param activity
     */
    public static void locationChecker(GoogleApiClient mGoogleApiClient, final Activity activity) {
        final LocationRequest locationRequest = LocationRequest.create();
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
                        // All location settings are satisfied. The client can initialize location requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:

                        // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    activity, 1000);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    /**
     *
     * Checks and prompt user the permissions needed by the app to work properly
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[],int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("Permessi accordati");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Funzionalità Limitata");
                    builder.setMessage("Permessi non concessi");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }

            }
        }
    }






    /*
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
                                    END -- SEZIONE DEDICATA ALLE CONNESSIONI BLUETOOTH E GPS --
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
     */





    /*
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
                                           -- INIZIO SEZIONE DEDICATA ALLA SCANSIONE --
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
     */


    /**
     *  Scans and filters the results
     */

    private ScanCallback leScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType,result);
            device = "Nome : " + result.getDevice().getName();
            rssi = " - RSSI : " + result.getRssi();

            // indirizzo esadecimale del dispositivo
            deviceAddress = btAdapter.getRemoteDevice(result.getDevice().getAddress());
            // ad ogni iterazione della callback aggiorno la lista dei dispositivi aggiungendo tutto ciò
            // che viene rilevato dalla scansione, anche i doppioni
            recentlySeen.add(device);

            // "noDuplicates" è una struttura dati che non ammette doppioni, la riempio con i dati raccolti
            // in questo modo vengono eliminate le entrate multiple.
            Set<String> noDuplicates = new LinkedHashSet<String>(recentlySeen);
            // svuoto la lista
            recentlySeen.clear();
            // riempio nuovamente la lista ma con i dati privi di entrate multiple
            recentlySeen.addAll(noDuplicates);


            // serve a contenere i dati dell'iterazione precedente ed eiliminare i ripetuti
            Set<String> onlyRssi = new LinkedHashSet<>(rssiList);
            // svuoto la lista degli rssi contenente i dati ripetuti
            rssiList.clear();
            // ora che la lista è vuota,aggiungo
            rssiList.add(rssi);
            // riaggiungo alla lista(che in questo frangente contiene i dati della scansione avvenuta)i dati presenti in onlyRssi
            rssiList.addAll(onlyRssi);

            // --> a questo punto avrò dei valori senza doppioni <-- //

            System.out.println("lista :"+recentlySeen);
            System.out.println("rssi_list :"+rssiList);
            System.out.println("deviceAddress : "+deviceAddress);

            if(!device.isEmpty()) {
                bleDevices.setText("");
                handLer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // svuoto i valori di "rssi" al termine della scansione per poter effettuare i controlli allo start
                        rssi ="";
                    }
                }, SCAN_PERIOD);
            }


            // fondamentale
            mGatt = null;

            // animazione pulsante piccolo
            final Animation animScaleDown = android.view.animation.AnimationUtils.loadAnimation(startScanningButton.getContext(),  R.anim.scale_down);

            // gestisco il click sulla lista
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                        covEr = new CharacteristicFragment();
                        FragmentTransaction transaction=getFragmentManager().beginTransaction();
                        // animazione per il fragment
                        transaction.setCustomAnimations(android.R.animator.fade_in,android.R.animator.fade_out);
                        // con replace sostituisco il layout del main con quello del fragment
                        transaction.replace(fragment_container,covEr);
                        // premendo il tasto back, torno al main ed il fragment viene tolto dallo stack
                        transaction.addToBackStack(null);
                        transaction.commit();

                        // nel dubbio disabilito i pulsanti, altrimenti si  creano conflitti
                        startScanningButtonBig.setEnabled(false);
                        startScanningButton.setEnabled(false);
                        // animazione che rende non visibile il pulsante piccolo
                        startScanningButton.startAnimation(animScaleDown);
                        startScanningButton.setVisibility(View.INVISIBLE);
                        // disabilito la lista stessa
                        listView.setEnabled(false);
                        listView.setVisibility(View.INVISIBLE);


                }
            });


        }
    }; // ************************************************ end ScanCallback ************************************************//



    /**
     * This method contains the logic behind the way the list is populated.
     * It also let the items disappear/appear on the display depending by the user interaction
     */
    public void counTer() {

        final Animation animFadeInList = android.view.animation.AnimationUtils.loadAnimation(listView.getContext(),  android.R.anim.fade_in);
        final Animation animScaleUp = android.view.animation.AnimationUtils.loadAnimation(startScanningButton.getContext(),  R.anim.scale_up);
        final Animation progressOut = android.view.animation.AnimationUtils.loadAnimation(progressBar.getContext(),  android.R.anim.fade_out);

        CountDownTimer c = new CountDownTimer(SCAN_PERIOD, 1000) {

                public void onTick(long millisUntilFinished) {
                    counter.setText("wait: " + millisUntilFinished / 1000);
                    // durante la scansione, lista invisibile
                    listView.setVisibility(View.INVISIBLE);
                    // torna visibile
                    progressBar.setVisibility(View.VISIBLE);
                }

                public void onFinish() {
                    // al termine del countdown,la progress bar circolare non è più visibile
                    progressBar.setAnimation(progressOut);
                    // deve comunque essere presente altrimenti l'animazione non è stabile
                    progressBar.setVisibility(View.INVISIBLE);
                    // al termine del countdown, appaiono con un'animazione il pulsante scan(little) e la listview
                    startScanningButton.setAnimation(animScaleUp);
                    startScanningButton.setVisibility(View.VISIBLE);
                    listView.setAnimation(animFadeInList);
                    listView.setVisibility(View.VISIBLE);
                    listView.setEnabled(true);
                    counter.setText("");

                    // numero di elementi presenti nella lista dei dispositivi scansionati es: 5
                    int numElementi = recentlySeen.size();
                    int x;

                    System.out.println("lista_rssi : "+rssiList);
                    // se entrambe le liste sono vuote,nella listView viene indicata la mancata rilevazione dei dispositivi
                    if(recentlySeen.isEmpty() && adapter.isEmpty()){
                        adapter.insert("",0);
                        adapter.insert(noDev,1);
                        listView.setAdapter(adapter);
                    }

                    // scorro la lista degli elementi scansionati
                    for(Object item : recentlySeen) {
                        if(item.equals("Nome : BlueNRG_Chat")||(item.equals("Nome : Lemset"))) {
                            x = numElementi--;  // decremento subito perchè parte da 0 nell'array
                            // ottengo l'rssi corrispondente al device
                            // basandomi sulla posizione che entrambi ricoprono nei rispettivi array
                            // es : deviceArray[3] <--> rssiArray[3]
                            String lastRssi = rssiList.get(x);
                            adapter.add(item + lastRssi);
                            listView.setAdapter(adapter);
                        }

                    }
                }

            }.start();

        }



    /**
     * This method starts the scan operation, it also invokes counter().
     * After four seconds an handler stops the scanning operation
     */
    public void startScanning() {

        // avvia il contatore
        counTer();

        // alla pressione del tasto "scan" in primis ripulisco l'adapter e la lista dei recenti
        adapter.clear();
        recentlySeen.clear();
        rssiList.clear();

        // task che avvia la scansione
        AsyncTask.execute(new Runnable() {

            @Override
            public void run() {
                // aggiunto btScanner in questa posizione per evitare crash dopo avvio se bt == off
                btScanner = btAdapter.getBluetoothLeScanner();
                btScanner.startScan(leScanCallback);
            }
        });


        // maniglia che stoppa la scansione in automatico  dopo 4 secondi
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
                                           -- FINE SEZIONE DEDICATA ALLA SCANSIONE --
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
     */


    /*
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
                                           -- INIZIO SEZIONE DEDICATA ALLA CONNESSIONE --
     *
     * --------------------------------------------------------------------------------------------------------------------
     *
     */




    /**
     *
     * Let the app connect to the selected device
     *
     * @param device
     * @return
     *
     */
    public BluetoothGatt connectToDevice(BluetoothDevice device) {
      if (mGatt == null)
      {
            mGatt = device.connectGatt(this, false, gattCallback);
      }
        return mGatt;
    }

    /**
     *
     * Let the app disconnect from the previously connected device
     *
     * @param mGatt
     *
     */
    public void discConnectToDevice(BluetoothGatt mGatt) {
        mGatt.disconnect();
    }


    /**
     * Gatt callback containing methods for a bidirectional communication
     */
    protected final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        /**
         *Detect the device connection status
         *
         * @param gatt
         * @param status
         * @param newState
         */
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

        /**
         * Controls the workflow of the two spinners and the button.
         * Contains the proper click listeners of these items
         * @param gatt
         * @param status
         */
        @Override
        public void onServicesDiscovered ( final BluetoothGatt gatt, int status){
                List<BluetoothGattService> services = gatt.getServices();
                uuId = services.get(1).getUuid();
                charUuid = services.get(1).getCharacteristics().get(0).getUuid();
                readCharacteristic(uuId, charUuid);

            // batteria
                for (BluetoothGattService service : services) {

                    if (service.getUuid().equals(BATTERY_UUID)) {
                        Log.d("SERVICE", String.valueOf(service.getUuid()));

                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(BATTERY_LEVEL);

                        if (characteristic != null) {
                            mGatt.readCharacteristic(characteristic);

                        }
                    }
                }
             ////////

                // ottengo un'istanza del fragment
                android.app.FragmentManager fm = getFragmentManager();
                final CharacteristicFragment fragment = (CharacteristicFragment) fm.findFragmentById(fragment_container);
                // animazione pulsante invia
                final Animation anim = android.view.animation.AnimationUtils.loadAnimation(fragment.invia.getContext(),  R.anim.shake);
                final Animation animDue = android.view.animation.AnimationUtils.loadAnimation(fragment.invia.getContext(),  android.R.anim.slide_in_left);
                // suggerimento dello spinner se utilizzo il menù a tendina -> non lo utilizzo ma lo lascio ugualmente
                fragment.spinnerDue.setPrompt(fragment.selezionaProgramma);


                // spinner per la selezione del profilo
                fragment.spinnerDue.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    // ----- GESTIONE ANIMAZIONI ED EVENTI SPINNER PROFILO ----- //

                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String item = (String) fragment.adapter.getItem(position);

                        // solo alla prima iterazione
                        if(x==0) {
                            // appare lo spinner della potenza
                            fragment.spinnerPower.setVisibility(View.VISIBLE);
                        }
                        x++;

                        // all'indice zero c'è un hint
                        if(item.equals(fragment.adapter.getItem(0))) {
                            Toasty.info(getApplicationContext(),"seleziona un programma",Toast.LENGTH_SHORT).show();
                            //quando l'utente rimane sull'hint, il programma vale zero, un parametro non valido
                            programma = 0;
                        }

                        fragment.invia.setProgress(0);

                    // ----- END GESTIONE ANIMAZIONI ED EVENTI SPINNER PROFILO ----- //

                        // programma scelto dall'utente dallo spinner
                        String progUno = fragment.programmaUno;
                        String progDue = fragment.programmaDue;
                        String progTre = fragment.programmaTre;
                        String progQuattro = fragment.programmaQuattro;
                        String progCinque = fragment.programmaCinque;
                        String progSei = fragment.programmaSei;
                        String progSette = fragment.programmaSette;
                        String progOtto = fragment.programmaOtto;
                        String progNove = fragment.programmaNove;
                        String progDieci = fragment.programmaDieci;
                        String progUndici = fragment.programmaUndici;
                        String progDodici = fragment.programmaDodici;
                        String progTredici = fragment.programmaTredici;
                        String progQuattordici = fragment.programmaQuattordici;
                        String progQuindici = fragment.programmaQuindici;
                        String progSedici = fragment.programmaSedici;
                        String progDiciassette = fragment.programmaDiciassette;
                        String progDiciotto = fragment.programmaDiciotto;
                        String progDiciannove = fragment.programmaDiciannove;
                        String progVenti = fragment.programmaVenti;
                        // immagini
                        int p1 = R.mipmap.p_1_23m2;int p2 = R.mipmap.p_2_23m3;int p3 = R.mipmap.p_3_22m2;
                        int p4 = R.mipmap.p_4_22m3;int p5 = R.mipmap.p_5_erp;int p6 = R.mipmap.p_6_emx;
                        int p7 = R.mipmap.p_7_23emp;int p8 = R.mipmap.p_8_22emp;int p9 = R.mipmap.p_9_23erp;
                        int p10 = R.mipmap.p_10_22erp;int p11 = R.mipmap.p_11_23m2s2;int p12 = R.mipmap.p_12_23m3s2;
                        int p13 = R.mipmap.p_13_22m2s2;int p14 = R.mipmap.p_14_22m3s2;int p15 = R.mipmap.p_15_lsm2;
                        int p16 = R.mipmap.p_16_lsm3;int p17 = R.mipmap.p_17_lsm2s2;int p18 = R.mipmap.p_18_lsm3s2;
                        int p19 = R.mipmap.p_19_r400; int p20 = R.mipmap.p_20_22dmp;



                        byte[] value = new byte[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};

                        // value è un byte,gli attribuisco il valore idoneo al click
                        setProgram(item,progUno,fragment,p1,value[0]);
                        setProgram(item,progDue,fragment,p2,value[1]);
                        setProgram(item,progTre,fragment,p3,value[2]);
                        setProgram(item,progQuattro,fragment,p4,value[3]);
                        setProgram(item,progCinque,fragment,p5,value[4]);
                        setProgram(item,progSei,fragment,p6,value[5]);
                        setProgram(item,progSette,fragment,p7,value[6]);
                        setProgram(item,progOtto,fragment,p8,value[7]);
                        setProgram(item,progNove,fragment,p9,value[8]);
                        setProgram(item,progDieci,fragment,p10,value[9]);
                        setProgram(item,progUndici,fragment,p11,value[10]);
                        setProgram(item,progDodici,fragment,p12,value[11]);
                        setProgram(item,progTredici,fragment,p13,value[12]);
                        setProgram(item,progQuattordici,fragment,p14,value[13]);
                        setProgram(item,progQuindici,fragment,p15,value[14]);
                        setProgram(item,progSedici,fragment,p16,value[15]);
                        setProgram(item,progDiciassette,fragment,p17,value[16]);
                        setProgram(item,progDiciotto,fragment,p18,value[17]);
                        setProgram(item,progDiciannove,fragment,p19,value[18]);
                        setProgram(item,progVenti,fragment,p20,value[19]);

                        //------------------>> inserisco nell'array la coppia di valori <<-------------------

                        byteValue=new byte[]{programma,potenza};

                        //------------------>> inserisco nell'array la coppia di valori <<-------------------

                    } // end spinner profilo


                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });


                // spinner per la selezione della potenza
                fragment.spinnerPower.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    // ----- GESTIONE ANIMAZIONI ED EVENTI SPINNER POTENZA -----//
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String item = (String) fragment.adapterPower.getItem(position);

                        // soltanto alla prima iterazione, compare il pulsante "invia"
                        if(j==0) {
                            fragment.invia.startAnimation(animDue);
                            fragment.invia.setVisibility(View.VISIBLE);
                            fragment.textView.setVisibility(View.VISIBLE);
                        }
                        j++;

                        // se diverso dall'hint, parte l'animazione del pulsante
                        if(item!=fragment.adapterPower.getItem(0)) {
                            fragment.invia.startAnimation(anim);
                        }else{

                            // quando l'utente rimane sull'hint, la potenza è zero, un parametro non valido
                            potenza = 0;
                        }

                        // azzero la barra circolare del progresso
                        fragment.invia.setProgress(0);

                    // ----- END GESTIONE ANIMAZIONI ED EVENTI SPINNER POTENZA -----

                        // potenza è un byte,gli attribuisco il valore idoneo al click
                        if(item.equals(fragment.potenzaQuattrocento)){potenza = 4;}
                        else if(item.equals(fragment.potenzaCinqueCinquanta)){potenza = 5;}
                        else if(item.equals(fragment.potenzaSeiCinquanta)){potenza = 6;}
                        else if(item.equals(fragment.potenzaSettecento)){potenza = 7;}
                        else if(item.equals(fragment.potenzaCinquecento)){potenza = 8;} // aggiunto in un secondo momento

                        //------------------>> inserisco nell'array la coppia di valori <<-------------------

                        byteValue=new byte[]{programma,potenza};

                        //------------------>> inserisco nell'array la coppia di valori <<-------------------

                        // gestisco l'evento click sul pulsante invia
                        fragment.invia.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // l'utente non ha scelto nulla ma è rimasto sull'hint
                                if (programma == 0 || potenza == 0)
                                {
                                    Toasty.warning(getApplicationContext(),"valore non idoneo",Toast.LENGTH_SHORT).show();
                                }
                                else
                                {

                                    // al click,la corona circolare fa un giro completo, colorandosi.
                                    fragment.invia.setProgress(100);
                                    // dopo 3 secondi si resetta
                                    fragment.invia.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            fragment.invia.setProgress(0);
                                        }
                                    },3000);


                                    // ***** Data are sent to the Bluetooth device paired with the app *****
                                    writeCharacteristic(uuId,charUuid,byteValue);
                                    // ********************************************************************** //

                                    Toasty.success(getApplicationContext(),"dati inviati al Lemset",Toast.LENGTH_SHORT).show();

                                }
                            }

                        });


                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

        }


        /**
         * Reads the characteristic and try to get the proper UUID.
         * Then calls showUuid(uuid)
         *
         * @param gatt
         * @param characteristic
         * @param status
         *
         */
        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,  final BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());

            // batteria

            try {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //ottengo un'istanza del fragment
                        android.app.FragmentManager fm = getFragmentManager();

                        CharacteristicFragment fragment = (CharacteristicFragment)fm.findFragmentById(fragment_container);
                        UUID  uuId = characteristic.getUuid();
                        System.out.println("UUUUID"+uuId);
                        //passo l'uuid al metodo definito nel fragment
                        fragment.showUuid(uuId);

                       // final int batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);


                        BluetoothGattService batteryService = mGatt.getService(BATTERY_UUID);
                        if(batteryService == null) {
                            Log.d(TAG, "Battery service not found!");
                            return;
                        }

                        BluetoothGattCharacteristic batteryLevel = batteryService.getCharacteristic(BATTERY_LEVEL);
                        if(batteryLevel == null) {
                            Log.d(TAG, "Battery level not found!");
                            return;
                        }
                        mGatt.readCharacteristic(batteryLevel);
                        Log.v(TAG, "batteryLevel = " + mGatt.readCharacteristic(batteryLevel));



                        Toasty.info(getApplicationContext(),"livello batteria : "+batteryLevel+"%",Toast.LENGTH_SHORT).show();
                        Log.d("BATTERY", "battery level: " + batteryLevel);

                    }
                });


            } catch (Exception e) {
                e.printStackTrace();
            }

        }




    }; // ************************************************ end gattCallback ************************************************//






    /**
     *
     * Set the image to show and the value of the byte.
     * Hide/Shows required items
     *
     * @param item
     * @param progNumber
     * @param fragment
     * @param p_
     * @param value
     *
     */

    public void setProgram(String item, String progNumber, final CharacteristicFragment fragment,int p_,byte value){

        if(item.equals(progNumber)){

            Intent intent = new Intent(getApplicationContext(),SelectProgramImage.class);
            intent.putExtra("valore",p_);
            startActivity(intent);

            programma = value;


        }

    }

    /**
     *
     * Checks if all the properties and the services it needs are well configured and enstablished
     * then reads the charachteristic of the paired device
     * @param gatservice_uuid
     * @param char_uuid
     * @return
     *
     */

    public boolean readCharacteristic(UUID gatservice_uuid, UUID char_uuid){

            try{
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

    /**
     *
     * Checks if all the properties and the services it needs are well configured and enstablished
     * then writes the charachteristic to the paired device
     *
     * @param gatservice_uuid
     * @param char_uuid
     * @param value
     * @return
     *
     */

    public boolean writeCharacteristic(UUID gatservice_uuid,UUID char_uuid,byte[] value){

            try{

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




} // main class
