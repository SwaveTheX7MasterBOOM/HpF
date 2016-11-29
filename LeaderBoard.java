package com.thomascapach.humanprojectpedometertest;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;


/**
 * Leader board. Nothing too fancy.
 *
 * If I was going to iterate on this project further I'd have multiple leader boards to showcase
 * leaders in different areas such as the best this week, month, day and some other fun ones.
 */
public class LeaderBoard extends AppCompatActivity
{

    private ListView list;
    private ArrayList<String> board;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leader_board);

        primeList();

        list = (ListView) findViewById(R.id.leaders);

        ArrayAdapter<String> listAdpt = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, board);

            list.setAdapter(listAdpt);
    }

    private void primeList()
    {

        ArrayList<Persona> temp = new ArrayList<Persona>();

        board = new ArrayList<String>();

        SQLiteDatabase mydatabase = SQLiteDatabase.openDatabase(getApplicationContext().getDatabasePath("step_DB").getAbsolutePath(), null, 0);

        String s = "SELECT * FROM steps_date";
        Cursor cursor =  mydatabase.rawQuery(s, null);

        if (cursor .moveToFirst())
        {

            while (cursor.isAfterLast() == false)
            {

                String name = cursor.getString(cursor.getColumnIndex("email"));
                String nam = cursor.getString(cursor.getColumnIndex("steps"));

                    temp.add(new Persona(name, Integer.parseInt(nam)));

                cursor.moveToNext();

            }

        }

        cursor.close();
        mydatabase.close();

        Collections.sort(temp);


            for(int x = 0; x < temp.size(); x++)
            {

                board.add(x, (x+1) + ". " + temp.get(x).name + " " + temp.get(x).steps);

            }

    }


    public class Persona implements Comparable<Persona>
    {

        private String name;
        private int steps;

        Persona(String name, int steps)
        {

            this.name = name;
            this.steps = steps;

        }


        @Override
        public int compareTo(Persona persona) {

            return persona.steps - steps;

        }
    }

}
