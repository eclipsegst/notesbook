package com.example.android.event;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Calendar;
import android.location.Location;
import android.widget.TextView;

import com.example.android.event.data.NotaContract;
import com.example.android.event.data.NotaDbHelper;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.location.LocationServices;


/**
 * A placeholder fragment containing a simple view.
 */
public class EventActivityFragment extends Fragment implements
        FloatingActionButton.OnCheckedChangeListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private String LOG_TAG = this.getClass().getSimpleName();

    private PausableChronometer mChronometer;
    private long mStartTime;
    private long mEndTime;
    private long mDuration;

    private FloatingActionButton mFabStart;
    private FloatingActionButton mFabPause;
    private FloatingActionButton mFabEnd;
    private int mToolbarColor;

    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    protected TextView mLatTextView;
    protected TextView mLonTextView;

    protected TextView mNowHeaderTextView;

    public EventActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_event, container, false);

        mNowHeaderTextView = (TextView) rootView.findViewById(R.id.now_header_textview);
        mChronometer = (PausableChronometer) rootView.findViewById(R.id.chronometer);

        mFabStart = (FloatingActionButton) rootView.findViewById(R.id.fab_start);
        mFabPause = (FloatingActionButton) rootView.findViewById(R.id.fab_pause);
        mFabEnd = (FloatingActionButton) rootView.findViewById(R.id.fab_end);
        mToolbarColor = ((Constants) this.getActivity().getApplication()).getToolbarColor();

        mFabStart.setBackgroundColor(getResources().getColor(mToolbarColor));
        mFabPause.setBackgroundColor(getResources().getColor(mToolbarColor));
        mFabEnd.setBackgroundColor(getResources().getColor(mToolbarColor));

        mFabStart.setOnCheckedChangeListener(this);
        mFabPause.setOnCheckedChangeListener(this);
        mFabEnd.setOnCheckedChangeListener(this);

        mFabStart.setVisibility(View.GONE);
        mFabPause.setVisibility(View.VISIBLE);
        mFabEnd.setVisibility(View.GONE);

        // start the chronometer when launching the activity
        mChronometer.start();
        Calendar c = Calendar.getInstance();
        int seconds = c.get(Calendar.SECOND);
        mStartTime = System.currentTimeMillis();
        Log.d(LOG_TAG, "start time: " + System.currentTimeMillis());
        System.out.println(utilities.getReadableDate(System.currentTimeMillis(), "dd/MM/yyyy hh:mm:ss.SSS"));

        mLatTextView = (TextView) rootView.findViewById(R.id.latitude_text);
        mLonTextView = (TextView) rootView.findViewById(R.id.longitude_text);

        Log.d(LOG_TAG, "on create");
        buildGoogleApiClient();

        return rootView;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(LOG_TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    public void onDisconnected() {
        Log.i(LOG_TAG, "Disconnected");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(LOG_TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.d(LOG_TAG, "onConnected");
        if (mLastLocation != null) {

            mLatTextView.setText("latitude: " + String.valueOf(mLastLocation.getLatitude()));
            mLonTextView.setText("longitude: " + String.valueOf(mLastLocation.getLongitude()));
        } else {
            Log.d(LOG_TAG, "No location data");
        }

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onViewCreated(view, savedInstanceState);
        int mStatusBarColor = ((Constants) getActivity().getApplication()).getStatusBarColor();

        mNowHeaderTextView.setTextColor(getResources().getColor(mStatusBarColor));

//        AppCompatActivity activity = (AppCompatActivity)getActivity();
//        Toolbar toolbarView = (Toolbar) getView().findViewById(R.id.toolbar);
//
//
//        activity.supportStartPostponedEnterTransition();
//
//        if ( null != toolbarView ) {
//            activity.setSupportActionBar(toolbarView);
//
//            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
//        }
    }


    @Override
    public void onCheckedChanged(FloatingActionButton fabView, boolean isChecked) {
        // when a FAB is toggled, log the action.
        switch (fabView.getId()){
            case R.id.fab_start:
                Log.d(LOG_TAG, String.format("FAB start was %s.", isChecked ? "checked" : "unchecked"));

                mFabStart.setVisibility(View.GONE);
                mFabPause.setVisibility(View.VISIBLE);
                mFabEnd.setVisibility(View.GONE);

                mChronometer.start();

                break;
            case R.id.fab_pause:
                Log.d(LOG_TAG, String.format("FAB pause was %s.", isChecked ? "checked" : "unchecked"));

                mFabStart.setVisibility(View.VISIBLE);
                mFabPause.setVisibility(View.GONE);
                mFabEnd.setVisibility(View.VISIBLE);

                mChronometer.stop();
                mDuration = mChronometer.getCurrentTime();
                break;
            case R.id.fab_end:
                Log.d(LOG_TAG, String.format("FAB end was %s.", isChecked ? "checked" : "unchecked"));

                mEndTime = System.currentTimeMillis();

                Log.d(LOG_TAG, "start time: " + mStartTime);
                Log.d(LOG_TAG, "end time: " + mEndTime);
                Log.d(LOG_TAG, "duration: " + mDuration);

                System.out.println(utilities.getReadableDate(System.currentTimeMillis(), "dd/MM/yyyy hh:mm:ss.SSS"));

                long categoryRowId = -1;
                categoryRowId = getCategoryDefaultRowId(getActivity());

                ContentValues newNotaValues = new ContentValues();
                newNotaValues.put(NotaContract.NotaEntry.COLUMN_CAT_KEY, categoryRowId);
                newNotaValues.put(NotaContract.NotaEntry.COLUMN_SUBJECT, "");
                newNotaValues.put(NotaContract.NotaEntry.COLUMN_NOTE, "");
                newNotaValues.put(NotaContract.NotaEntry.COLUMN_START, mStartTime);
                newNotaValues.put(NotaContract.NotaEntry.COLUMN_END, mEndTime);
                newNotaValues.put(NotaContract.NotaEntry.COLUMN_DURATION, mDuration);
                newNotaValues.put(NotaContract.NotaEntry.COLUMN_LAT, mLastLocation.getLatitude());
                newNotaValues.put(NotaContract.NotaEntry.COLUMN_LON, mLastLocation.getLongitude());

                long notaRowId = -1;

                notaRowId = insertNewNota(getActivity().getApplicationContext(), newNotaValues);
                Log.d(LOG_TAG, "notaId: " + notaRowId);
                MainActivityFragment.mNotaAdapter.notifyDataSetChanged();
                MainActivityFragment.mRecyclerView.setAdapter(MainActivityFragment.mNotaAdapter);
                Intent intent = new Intent(getActivity(), NotaActivity.class);
                intent.putExtra("notaId", notaRowId);
                startActivity(intent);

                break;

            default:
                break;
        }
    }

    private long insertNewNota(Context context, ContentValues contentValues) {
        NotaDbHelper dbHelper = new NotaDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long notaRowId = -1;
        notaRowId = db.insert(NotaContract.NotaEntry.TABLE_NAME, null, contentValues);

        return notaRowId;
    }

    private long getCategoryDefaultRowId(Context context) {
        NotaDbHelper dbHelper = new NotaDbHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor cursorCategory = context.getContentResolver().query(
                NotaContract.CategoryEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns
                NotaContract.CategoryEntry.COLUMN_NAME + " = " + "'default'", // columns for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        long categoryRowId = -1;

        if (cursorCategory.moveToFirst()) {
            // get the default category row id
            int columnNameIndex = cursorCategory.getColumnIndex(NotaContract.CategoryEntry._ID);
            categoryRowId = cursorCategory.getLong(columnNameIndex);
            Log.d(LOG_TAG, "We have a default category with row id: " + categoryRowId);
        } else {
            // create default category
            ContentValues testValuesCategory = createCategoryValues();
            categoryRowId = db.insert(NotaContract.CategoryEntry.TABLE_NAME, null, testValuesCategory);
            Log.d(LOG_TAG, "We don't have a default category. So we create the default category with row id: " + categoryRowId);
        }
        return categoryRowId;
    }

    /**
     * @return ContentValues representing the default category
     */
    private ContentValues createCategoryValues() {

        ContentValues testValuesCategory = new ContentValues();
        testValuesCategory.put(NotaContract.CategoryEntry.COLUMN_NAME, "default");
        testValuesCategory.put(NotaContract.CategoryEntry.COLUMN_TOTAL_TIME, 0);

        return testValuesCategory;
    }
}
