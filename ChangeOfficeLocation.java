package com.thomascapach.humanprojectpedometertest;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * if the user never set a location at signup or they change jobs
 *
 * also useful for testing the 1 hour stand up requirement as it uses this location and the phones
 * current location to know when to notify the user they should stand up.
 *
 */
public class ChangeOfficeLocation extends AppCompatActivity {


    private Button button;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    private Place office;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chnage_office_location);


        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();


        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);


            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {

                @Override
                public void onPlaceSelected(Place place)
                {

                    office = place;

                }

                @Override
                public void onError(Status status)
                {

                }

            });

        button = (Button) findViewById(R.id.button2);

            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {

                    updateWork();

                }
            });

    }


    public void updateWork()
    {

        SQLiteDatabase mydatabase = SQLiteDatabase.openDatabase(getApplicationContext().getDatabasePath("step_DB").getAbsolutePath(), null, 0);

        String s = "SELECT * FROM office_location WHERE  email='"+ firebaseUser.getEmail() +"'";

        Cursor resultSet =  mydatabase.rawQuery(s, null);


            if(resultSet.getCount() > 0)
            {

                mydatabase.execSQL("UPDATE office_location SET address='"+ office.getAddress() +"' WHERE email='"+ firebaseUser.getEmail() +"'");

            }
            else
            {

                mydatabase.execSQL("INSERT INTO office_location VALUES('"+ firebaseUser.getEmail() +"','"+ office.getAddress() +")");

            }


        mydatabase.close();
        resultSet.close();

            finish();

    }

}
