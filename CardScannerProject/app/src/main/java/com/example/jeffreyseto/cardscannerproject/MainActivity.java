package com.example.jeffreyseto.cardscannerproject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cardscanner.CardScannerActivity;

public class MainActivity extends AppCompatActivity {

    Button cameraButton;
    TextView programView;
    ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        programView = (TextView) findViewById(R.id.programView);
        image = (ImageView) findViewById(R.id.imageView);

        cameraButton = (Button) findViewById(R.id.cameraButton);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CardScannerActivity.class);
                intent.putExtra(CardScannerActivity.EXTRA_SCAN_INSTRUCTIONS, "Center card here\nIt will scan automatically");
                startActivityForResult(intent,1);
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode == CardScannerActivity.RESULT_CODE_SUCCESS){
            image.setImageBitmap((Bitmap) data.getParcelableExtra(CardScannerActivity.EXTRA_CARD_IMAGE));
        } else {
            System.out.println("ERRROOOOORRRRR");
        }
    }
}
