package com.example.macbook.autocontrol;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;

import com.example.macbook.autocontrol.service.SemComunication;

public class MainActivity extends AppCompatActivity {

    private Button btnAtras,btnAdelante,btnDerecha,btnIzquierda,btnFrenar;
    private RatingBar ratingVelocidad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnAtras = (Button) findViewById(R.id.AtrasButton);
        btnFrenar = (Button) findViewById(R.id.FrenarButton);
        btnAdelante = (Button) findViewById(R.id.AdelanteButton);
        btnDerecha = (Button) findViewById(R.id.DerechaButton);
        btnIzquierda = (Button) findViewById(R.id.IzquierdaButton);
        ratingVelocidad = (RatingBar) findViewById(R.id.VelocidadRating);

        final SemComunication sem=new SemComunication();
        Log.i("Se creo","");
        //SemComunication.create();
        //sem.run();
        sem.createClient();
        Log.i("Se creo", "");


        btnAtras.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        //SemComunication.requestAtras();
                    }
                });
        btnFrenar.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        SemComunication.requestFrenar();
                    }
                });

        btnAdelante.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        //SemComunication.requestAdelate();
                    }
                });
        btnDerecha.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        //SemComunication.requestDerecha();
                    }
                });
        btnIzquierda.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View view) {
                        //SemComunication.requestIzquierda();
                        sem.requestFull();
                    }
                });


        ratingVelocidad.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {

            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating,
                                        boolean fromUser) {
                SemComunication.requestVelocidad(rating);
            }
        });


    }

}

