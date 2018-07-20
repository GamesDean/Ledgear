package com.menowattge.myled;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.zxing.Result;

import es.dmoral.toasty.Toasty;
import me.dm7.barcodescanner.zxing.ZXingScannerView;


public class QrCodeActivity extends AppCompatActivity  implements ZXingScannerView.ResultHandler{

    private ZXingScannerView mScannerView;

    private String  menowattCode = "User : Operator\n"+"Pass :  Ledgear";


    public void CheckPermission() {


        if (this.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Questa app deve poter accedere alla fotocamera");
            builder.setMessage("Consentire all'app l'accesso alla fotocamera affinchè possa scansionare il qrcode.");
            builder.setPositiveButton(android.R.string.ok, null);

            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 1);


                }
            });
            builder.show();
        }




    }



        @Override
    protected void onCreate(Bundle state) {

        super.onCreate(state);

       // CheckPermission();

        // Programmatically initialize the scanner view
        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);
        mScannerView.startCamera();



    }



    @Override
    public void onResume() {
        super.onResume();
        CheckPermission();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause() {

        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void handleResult(Result rawResult) {
        // Qui è possibile gestire il risultato
         Log.v("risultato", rawResult.getText());
         Log.v("risultato_qrcodeformat", rawResult.getBarcodeFormat().toString()); // Prints the scan format (qrcode, pdf417 etc.)


        if(rawResult.getText().equals(menowattCode)){
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            // Toast.makeText(getApplicationContext(),"codice valido : login in corso",Toast.LENGTH_SHORT).show();
            Toasty.success(getApplicationContext(),"codice valido",Toast.LENGTH_SHORT).show();
            startActivity(intent);
        }
        else{
            Toasty.error(getApplicationContext(),"codice non valido",Toast.LENGTH_SHORT).show();        }

        // If you would like to resume scanning, call this method below:
        mScannerView.resumeCameraPreview(this);
    }
}

