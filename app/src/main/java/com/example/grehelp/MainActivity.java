package com.example.grehelp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.grehelp.Adapters.WordListAdapter;
import com.example.grehelp.Models.DataModel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;



//======================Sheets
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Data;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.sheets.v4.SheetsScopes;

import com.google.api.services.sheets.v4.model.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

//============================

public class MainActivity extends AppCompatActivity  implements EasyPermissions.PermissionCallbacks {


    //-----------------Sheet
    GoogleAccountCredential mCredential;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Call Google Sheets API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { SheetsScopes.SPREADSHEETS_READONLY };

    ProgressDialog mProgress;
    //----------------------
    private SharedPreferences sharedPreferences;

    private boolean DUMP_FLAG = false;

    private RecyclerView recyclerView;

    private WordListAdapter wordListAdapter;

    private LinearLayoutManager linearLayoutManager =new LinearLayoutManager(this);;

    private List<DataModel> dataModelList = new ArrayList<>() ;
    private List<DataModel> temp_dataModelList = new ArrayList<>();
    private int length_dataModelList = 0 ;
    private int position;
    private int dump_position;

    private Random random = new Random();

    private Button btn_next;
    private Button btn_dump;
    private Button btn_update;
    private ImageButton btn_replay;
    private TextView tv_word;
    private TextView tv_desc;
    private TextView tv_size;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        findComponents();

        retrieveSharedPreferences();

        buildViews();

        clickListeners();

    }

    private int wordsToNotify(){
        int num = 0 ;
        int temp = length_dataModelList - dataModelList.size();
        if(temp > 120){
                num = 120 - (temp - 120); // 240 - temp
        }else if( length_dataModelList - dataModelList.size() < 120){
            num = 240 - temp;
        }
        
        return num;
    }


    private void init(){

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Sheets API ...");

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());


        sharedPreferences = getSharedPreferences("App_settings", MODE_PRIVATE);

    }

    private void retrieveSharedPreferences(){
        Gson gson = new Gson();
        String json = sharedPreferences.getString("DATA_LIST", null);
        Type type = new TypeToken<ArrayList<DataModel>>(){}.getType();
        dataModelList = gson.fromJson(json, type);

        if(dataModelList != null){
            btn_dump.setEnabled(true);
            btn_replay.setEnabled(true);
            btn_next.setEnabled(true);

            tv_size.setText(String.valueOf(dataModelList.size()));
            Toast.makeText(this, "Retrieved shared preference.", Toast.LENGTH_SHORT).show();
        }else{
            dataModelList = new ArrayList<>();
        }

    }

    private void packageSharedPreferences(){
        SharedPreferences.Editor editor  = sharedPreferences.edit();
        Gson gson = new Gson();

        String json = gson.toJson(dataModelList);

        editor.putString("DATA_LIST", json);
        editor.commit();
        Toast.makeText(this, "Packaged Shared Preferences", Toast.LENGTH_SHORT).show();

    }

    private void findComponents() {
        btn_dump = findViewById(R.id.button_dump);
        btn_dump.setEnabled(false);
        btn_next = findViewById(R.id.button_next);
        btn_next.setEnabled(false);
        btn_update = findViewById(R.id.button_update);
        btn_replay = findViewById(R.id.button_replay);
        btn_replay.setEnabled(false);
        tv_word = findViewById(R.id.textView_word);
        tv_desc = findViewById(R.id.textView_desc);
        tv_size = findViewById(R.id.textview_size);
        tv_size.setText("---");

        recyclerView = findViewById(R.id.recyclerView_words);
        recyclerView.setLayoutManager(linearLayoutManager);
    }


    private void buildViews() {
        wordListAdapter = new WordListAdapter(this, dataModelList);
        recyclerView.setAdapter(wordListAdapter);
    }

    private void clickListeners() {
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_desc.setText("---");
                tv_size.setText(String.valueOf(dataModelList.size()));
                DUMP_FLAG = false;

                if(!dataModelList.isEmpty()) {
                    dataModelList.remove(position);
                    tv_word.setText(dataModelList.get(position).getWord() );
                    wordListAdapter.notifyDataSetChanged();
                }else{
                    Toast.makeText(getApplicationContext(), "DRY NOW!!", Toast.LENGTH_SHORT).show();
                }

                dump_position = position;
                position = random.nextInt(dataModelList.size());

            }
        });

        btn_dump.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(DUMP_FLAG == false) {
                    tv_desc.setText(dataModelList.get(dump_position).getDesc());
                    dataModelList.add(dataModelList.get(dump_position));
                    DUMP_FLAG = true;
                }
            }
        });

        btn_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataModelList.clear();
                wordListAdapter.notifyDataSetChanged();
                getResultsFromApi();
            }
        });

        btn_replay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataModelList = new ArrayList<>(temp_dataModelList);
                tv_size.setText( String.valueOf(dataModelList.size()));
                wordListAdapter.notifyDataSetChanged();
                Toast.makeText(getApplicationContext(), "REPLINSHED!!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            Toast.makeText(this, "No network connection available", Toast.LENGTH_SHORT).show();
        } else {
            new MakeRequestTask(mCredential).execute();
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private class MakeRequestTask extends AsyncTask<Void, Void, List<DataModel>> {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;


        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = new NetHttpTransport();//AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Sheets API Android Quickstart")
                    .build();
        }

        /**
         * Background task to call Google Sheets API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<DataModel> doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                e.printStackTrace();
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of names and majors of students in a sample spreadsheet:
         * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
         * @return List of names and majors
         * @throws IOException
         */
        private List<DataModel> getDataFromApi() throws IOException {
            String spreadsheetId = "10cg7bbfh0QaWO--ZLv0Ot6A7UHjq6oWO-trFhO86HkA";
            String range = "GWM!A1:C";

            ValueRange response = this.mService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();

            List<List<Object>> values = response.getValues();
            length_dataModelList = values.size();

            Log.d("DataFrom API length: ", String.valueOf(values.size()) );
            if (values != null) {
                //results.add("Name, Major");
                for (List row : values) {
                    if(row.size() > 2) {
                        dataModelList.add(new DataModel(String.valueOf(row.get(1)), String.valueOf(row.get(2)) ));
                    }else{
                        dataModelList.add(new DataModel(String.valueOf(row.get(1)), "" ));
                    }

                }
            }
            return dataModelList;
        }


        @Override
        protected void onPreExecute() {
            //mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<DataModel> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                Toast.makeText(getApplicationContext() ,"No results returned." , Toast.LENGTH_SHORT).show();
            } else {
                wordListAdapter.notifyDataSetChanged();
                Toast.makeText(getApplicationContext() ,"Data retrieved using the Google Sheets API:" , Toast.LENGTH_SHORT).show();

                temp_dataModelList = new ArrayList<>(dataModelList);
                length_dataModelList = dataModelList.size();

                position = random.nextInt(dataModelList.size());
                dump_position = position;
                tv_word.setText( dataModelList.get(position).getWord() );
                tv_size.setText(String.valueOf(length_dataModelList) );

                btn_dump.setEnabled(true);
                btn_next.setEnabled(true);
                btn_replay.setEnabled(true);
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    Log.e("MainActivity: The following error occured\n", mLastError.getMessage());
                }
            } else {
                Toast.makeText(MainActivity.this, "Request cancelled.", Toast.LENGTH_SHORT).show();
            }
        }
    }



    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
//                    mOutputText.setText(
//                            "This app requires Google Play Services. Please install " +
//                                    "Google Play Services on your device and relaunch this app.");
                    Toast.makeText(this, "This app requires Google Play Services. Please install " +
                            "Google Play Services on your device and relaunch this app.", Toast.LENGTH_SHORT).show();
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        packageSharedPreferences();
    }

    @Override
    protected void onPause() {
        super.onPause();

        packageSharedPreferences();
    }
}