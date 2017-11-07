package com.menowattge.myled;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

public class SelectProgramImage extends AppCompatActivity {

    ImageView image;
    ImageButton buttonClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_program_image);

        image = (ImageView) findViewById(R.id.imageView3);
        buttonClose = (ImageButton) findViewById(R.id.imageButton2);
        int valueFromMain = getIntent().getIntExtra("valore",0);

        image.setImageResource(valueFromMain);
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }


}
