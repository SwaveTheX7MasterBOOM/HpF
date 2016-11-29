package com.thomascapach.humanprojectpedometertest;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;


/**
 *
 *ASSUMPTIONS HERE
 *
 * I don't really have too many. The test was mostly straight forward so I'll be mostly providing
 * a brief description of the inner workings.
 *
 * Signin authorization and user registration are done using google's firebase. I used it because
 * it is eay to implement and provides scalability *
 *
 * User data such as the  distance or steps  traveled as well as the users work location is stored
 * locally in a simple sqllite database. I use the phones step detection capabilities to approximate
 * distance on the scale of about a foot per step. I had already showcase some usage of the location
 * capabilities of android with the standup reminder described below so I went with the sensors.
 *
 * Milestone feedback is provided via a notification
 *
 * The standup reminder works with the phones current location in addition to the stored location provided by the user.
 * IF the user is at the work location for  an hour they will receive a notification
 *
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, SensorEventListener {


    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    private SQLiteDatabase stepDatabase;

    protected GoogleApiClient mGoogleApiClient;

    private static Location mLastLocation;

    final static int FINE_LOCATION_PERMISSION_REQUEST = 0;

    private SensorManager mSensorManager;

    private Sensor mStepDetectorSensor;

    private static int stepCounter = 0;

    private ImageButton twit;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

            setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

            drawer.setDrawerListener(toggle);

            toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

            navigationView.setNavigationItemSelectedListener(this);

        stepDatabase = openOrCreateDatabase("step_DB", MODE_PRIVATE, null);

            stepDatabase.execSQL("CREATE TABLE IF NOT EXISTS office_location(email VARCHAR, address VARCHAR, PRIMARY KEY(email));");
            stepDatabase.execSQL("CREATE TABLE IF NOT EXISTS steps_date(email VARCHAR, date VARCHAR, steps INTEGER, PRIMARY KEY(email, date));");

        stepDatabase.close();

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        buildGoogleApiClient();

        if (firebaseUser == null)
        {

            LogIn();

        }
        else
        {

            //show stats

            NavigationView navigationView2 = (NavigationView) findViewById(R.id.nav_view);

            View hView = navigationView.getHeaderView(0);

            TextView nav_user = (TextView) hView.findViewById(R.id.user_email_menu);

                nav_user.setText(firebaseUser.getEmail());


            twit = (ImageButton) findViewById(R.id.twitter_button);

                twit.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v)
                    {

                        String tweetUrl = String.format("https://twitter.com/intent/tweet?text=%s&url=%s", URLEncoder.encode("Look how far I can go "+ stepCounter+ "ft #crushingIT"), URLEncoder.encode("thomascapach.com"));

                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweetUrl));

                        List<ResolveInfo> matches = getPackageManager().queryIntentActivities(intent, 0);

                            for (ResolveInfo info : matches)
                            {

                                if (info.activityInfo.packageName.toLowerCase().startsWith("com.twitter"))
                                {

                                    intent.setPackage(info.activityInfo.packageName);

                                }

                            }

                        startActivity(intent);

                    }
                });


            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

            mStepDetectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);


                mSensorManager.registerListener(this, mStepDetectorSensor, SensorManager.SENSOR_DELAY_FASTEST);




            SQLiteDatabase mydatabase = SQLiteDatabase.openDatabase(getApplicationContext().getDatabasePath("step_DB").getAbsolutePath(), null, 0);

            String s = "SELECT steps FROM steps_date WHERE date='"+DateFormat.getDateInstance().format(System.currentTimeMillis())+"' AND email ='"+ firebaseUser.getEmail() +"'";

            Cursor resultSet =  mydatabase.rawQuery(s, null);

               if(resultSet.getCount()> 0)
               {

                    resultSet.moveToFirst();

                       stepCounter = resultSet.getInt(0);

               }



            mydatabase.close();
            resultSet.close();


            TextView t = (TextView) findViewById(R.id.textView3);

                t.setText("Distance Today: " + (stepCounter) +"ft");

            t = (TextView) findViewById(R.id.textView4);

                t.setText("Distance to milestone: "+ (10 - (stepCounter % 10)));


            scheduleNotification(getNotification("Remember to stretch your legs!", 0), 0);

        }

    }


    public void onSensorChanged(SensorEvent event)
    {

        Sensor sensor = event.sensor;

        float[] values = event.values;

        int value = -1;

        if (values.length > 0)
        {

            value = (int) values[0];

        }


        if (sensor.getType() == Sensor.TYPE_STEP_DETECTOR)
        {

            TextView t = (TextView) findViewById(R.id.textView3);

                t.setText("Distance: " + (stepCounter +=value) +"ft");

            t = (TextView) findViewById(R.id.textView4);

                t.setText("Distance to milestone: "+ (1000 - (stepCounter % 1000)));
        }


        if(stepCounter % 1000 == 0)
        {

            scheduleNotification(getNotification("You have reached a milestone check it out!", 1), 1);

        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient()
    {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

    }


    public static boolean compareLocationAndStoredAddress(Context context) {

        Geocoder coder = new Geocoder(context);

        List<Address> address;

        LatLng p1 = null;

        String strAddress;

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        SQLiteDatabase mydatabase = SQLiteDatabase.openDatabase(context.getDatabasePath("step_DB").getAbsolutePath(), null, 0);

        Cursor resultSet =  mydatabase.rawQuery("SELECT * FROM office_location WHERE email ='"+ firebaseUser.getEmail() +"'", null);

            resultSet.moveToFirst();

        strAddress = resultSet.getString(1);

        mydatabase.close();
        resultSet.close();

        float[] results = new float[1];

        try
        {

            address = coder.getFromLocationName(strAddress, 5);

            if (address == null)
            {

                return false;

            }

            Address location = address.get(0);


            Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                    mLastLocation.getLatitude(), mLastLocation.getLongitude(), results);



        }
        catch (Exception ex)
        {

            ex.printStackTrace();

        }


        if(results[0] < 1000)
        {

            return true;

        }
        else
        {

            return false;

        }

    }


    public void onLocationChanged(Location location)
    {

        mLastLocation = location;

    }

    private boolean CheckFineLocationPermission()
    {

        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {

            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

                return true;

        }
        else
        {

            ActivityCompat.requestPermissions(this, new String[]{ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION_REQUEST);

                return false;

        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults)
    {

        if (requestCode == FINE_LOCATION_PERMISSION_REQUEST)
        {

            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {

                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }

                    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            }

        }


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }


    @Override
    public void onConnected(Bundle connectionHint)
    {

        if (mLastLocation != null)
        {


        }
        else
        {

        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult result)
    {

    }


    @Override
    public void onConnectionSuspended(int cause)
    {

        mGoogleApiClient.connect();

    }

    @Override
    protected void onStart()
    {
        super.onStart();

            mGoogleApiClient.connect();

    }

    @Override
    protected void onResume()
    {

        super.onResume();

    }

    @Override
    protected void onStop()
    {

        super.onStop();

        if (mGoogleApiClient.isConnected())
        {

            mGoogleApiClient.disconnect();

        }



        SQLiteDatabase mydatabase = SQLiteDatabase.openDatabase(getApplicationContext().getDatabasePath("step_DB").getAbsolutePath(), null, 0);

        String s = "SELECT * FROM steps_date WHERE date='"+DateFormat.getDateInstance().format(System.currentTimeMillis())+"' AND email='"+ firebaseUser.getEmail() +"'";

        Cursor resultSet =  mydatabase.rawQuery(s, null);

            if(resultSet.getCount() > 0)
            {

                mydatabase.execSQL("UPDATE steps_date SET steps="+ stepCounter +" WHERE date='"+DateFormat.getDateInstance().format(System.currentTimeMillis())+"' AND email='"+ firebaseUser.getEmail() +"'");

            }
            else
            {

                mydatabase.execSQL("INSERT INTO steps_date VALUES('"+ firebaseUser.getEmail() +"','"+ DateFormat.getDateInstance().format(System.currentTimeMillis()) +"',"+ stepCounter +");");

            }


        mydatabase.close();
        resultSet.close();

    }
    @Override
    protected void onDestroy()
    {

        super.onDestroy();

        if(mSensorManager != null)
        {

            mSensorManager.unregisterListener(this, mStepDetectorSensor);

        }

    }

    private void scheduleNotification(Notification notification, int type)
    {

        Intent notificationIntent = new Intent(this, NotificationPublisher.class);

            notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, 1);
            notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);

            if(type == 0)
            {

                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, AlarmManager.INTERVAL_HOUR, AlarmManager.INTERVAL_HOUR, pendingIntent);

            }
            else if(type == 1)
            {

                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, 1000, pendingIntent);

            }
    }

    private Notification getNotification(String content, int type)
    {
        Notification.Builder builder = new Notification.Builder(this);

            builder.setContentTitle("HEY!");
            builder.setContentText(content);

        if(type == 0)
        {

            builder.setSmallIcon(R.drawable.ic_airline_seat_legroom_normal_black_24dp);

        }
        else if(type == 1)
        {

            builder.setSmallIcon(R.drawable.ic_menu_share);

        }

            return builder.build();

    }


    private void LogIn()
    {

        Intent intent = new Intent(this, LoginActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);

    }

    @Override
    public void onBackPressed()
    {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

            if (drawer.isDrawerOpen(GravityCompat.START))
            {

                drawer.closeDrawer(GravityCompat.START);

            }
            else
            {

                super.onBackPressed();

            }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

            return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

                return super.onOptionsItemSelected(item);

    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item)
    {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

            if (id == R.id.sign_out)
            {

                firebaseAuth.signOut();

                    LogIn();

            }
            else if (id == R.id.leader_board)
            {

                Intent intent = new Intent(this, LeaderBoard.class);



                startActivity(intent);


            }
            else if (id == R.id.new_local)
            {

                Intent intent = new Intent(this, ChangeOfficeLocation.class);



                startActivity(intent);


            }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

            drawer.closeDrawer(GravityCompat.START);

                return true;

    }


}
