package activity.mychat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import crypto.AESHelper;
import crypto.Crypto;
import crypto.RSA;
import database.SQLiteHelper;
import items.contactItem;
import items.contactListViewAdapter;


public class Chat_activity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    ActionBarDrawerToggle drawerToggle;

    private ArrayList<String> chatMessage = new ArrayList<String>();
    private ArrayList<String> chatDate = new ArrayList<String>();
    private ArrayList<String> chatVerified = new ArrayList<String>();
    private ArrayList<String> chatSender = new ArrayList<String>();
    private List<Message> messageItems;
    private MessagesListAdapter mAdapter;

    private ListView chatlist;
    private EditText texttosend;
    private Button btnsend;
    private ListView chatListView;

    private String resp;
    private Long userid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        chatlist = (ListView)findViewById(R.id.chatlistView);
        texttosend = (EditText)findViewById(R.id.texttosend);
        btnsend = (Button)findViewById(R.id.btnsend);
        chatListView = (ListView)findViewById(R.id.chatlistView);

        userid = getIntent().getExtras().getLong("userid");

        Toolbar toolbar1 = (Toolbar) findViewById(R.id.toolbar1);
        setSupportActionBar(toolbar1);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar1, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        btnsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptMessage(texttosend.getText().toString());
            }
        });

        Bundle b = getIntent().getExtras();
        if(b != null){
            userid = b.getLong("userid");
        }else{
            userid = 0L;
        }

        openAndQueryDatabase();
        displayResultList();
    }

    private void openAndQueryDatabase(){
        try {

            chatListView.removeAllViewsInLayout();
            chatMessage.clear();
            chatVerified.clear();
            chatDate.clear();
            chatSender.clear();

            String[] data = new String[1];
            data[0] = Long.toString(userid);

            String selectSearch = "SELECT * " +
                    "FROM chatlist " +
                    "WHERE CHAT_ID = ? ";

            Cursor c = Main_activity.newDB.rawQuery(selectSearch, data);

            //"ON user.USER_ID = chat.CHAT_ID " +
            //"ORDER BY chat.CHAT_DATE DESC ", null);
            if (c != null ) {
                if  (c.moveToFirst()) {
                    do {
                        String tmpchatMessage = c.getString(c.getColumnIndex("CHAT_MESSAGE"));
                        String tmpchatDate  = c.getString(c.getColumnIndex("CHAT_DATE"));
                        String tmpchatsender = c.getString(c.getColumnIndex("CHAT_SENDER_ID"));

                        chatDate.add(tmpchatDate);
                        chatMessage.add(tmpchatMessage);
                        chatVerified.add("false");
                        chatSender.add(tmpchatsender);

                    }while (c.moveToNext());
                }
            }

            c.close();
        } catch (SQLiteException se ) {
            Log.e(getClass().getSimpleName(), "Could not create or Open the database");
        } finally {

            try {
                chatListView.removeAllViewsInLayout();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    private void displayResultList() {

        messageItems = new ArrayList<Message>();
        messageItems.clear();

        for (int i = 0; i < chatMessage.size(); i++) {
            Message item = new Message(chatDate.get(i), chatMessage.get(i), chatVerified.get(i), chatSender.get(i));
            messageItems.add(item);
        }

        mAdapter = new MessagesListAdapter(this, messageItems);
        chatListView.setAdapter(mAdapter);
    }

    private void encryptMessage(String message){

            String key = getPublicKey();

                try {

                    //fehler in RSA encodetobase64

                    String rand = random();
                    String tmpmessage = AESHelper.encrypt(rand, message);
                    String encryptedkey = RSA.encryptWithKey(key, rand);

                    //String encryptedmessage = "---Begin Message---\n" + tmpmessage + "\n---End Message---\n" + "---Beginn Key---\n" + encryptedkey + "---End Key---";

                    Toast.makeText(getApplicationContext(), encryptedkey , Toast.LENGTH_LONG).show();
                    new sendMessage().execute(tmpmessage, Long.toString(userid));

                }catch (Exception e) {
                    e.printStackTrace();
                }

    }

    private String getPublicKey(){

        String[] data = new String[1];
        data[0] = Long.toString(userid);

        String selectKey = "SELECT userlist.USER_PUBLICKEY " +
                "FROM userlist " +
                "WHERE userlist.USER_ID = ? ";

        Cursor c = Main_activity.newDB.rawQuery(selectKey, data);
        String tmpkey = "---";

        if (c != null ) {
            if (c.moveToFirst()) {
                tmpkey = c.getString(c.getColumnIndex("USER_PUBLICKEY"));
                return tmpkey;
            }
        }

        if(tmpkey.equals("---")){
            noKey();
            return "---";
        }else {
            return tmpkey;
        }
    }

    private void noKey(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Secure Chat");
        builder.setMessage("User has no Key\nCan´t send Message");

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });


        builder.setCancelable(false);
        builder.show();
    }

    public String random() {
        char[] chars1 = "ABCDEF012GHIJKL345MNOPQR678STUVWXYZ9".toCharArray();
        StringBuilder sb1 = new StringBuilder();
        Random random1 = new Random();
        for (int i = 0; i < 256; i++)
        {
            char c1 = chars1[random1.nextInt(chars1.length)];
            sb1.append(c1);
        }
        return sb1.toString();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_chats) {

        } else if (id == R.id.nav_new_cotact) {

            searchforuser();
        } else if (id == R.id.nav_newkey) {

            revokekey();
        } else if (id == R.id.nav_logout) {
            logout();
        }else if (id == R.id.nav_deleteacc) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void revokekey(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Secure Chat");
        builder.setMessage("Enter your Revoke Key:");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new revokekey().execute(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void logout(){

        AlertDialog.Builder msgBox = new AlertDialog.Builder(this);

        msgBox.setTitle("Secure Chat");
        msgBox.setMessage("Logout will Delete all your Data");
        msgBox.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                Main_activity.editor.clear();
                Main_activity.editor.putBoolean("login", false);
                Main_activity.editor.commit();

                SQLiteHelper.cleanTable(Main_activity.newDB);

                openlogin();
            }
        });

        msgBox.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        msgBox.setCancelable(false);
        // create alert dialog
        AlertDialog alertDialog = msgBox.create();
        // show it
        alertDialog.show();
    }

    private void openlogin(){

        Intent i = new Intent(this, Login_activity.class);
        startActivity(i);
        finish();
    }

    private void searchforuser(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Secure Chat");
        builder.setMessage("Search new User:");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!input.getText().toString().equals("")) {
                    checkifuserexists(input.getText().toString());

                } else {
                    Toast.makeText(getApplicationContext(), "No empty Username", Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void checkifuserexists(String name){

        String[] data = new String[1];
        data[0] = name;

        String selectSearch = "SELECT userlist.USER_NAME " +
                "FROM userlist " +
                "WHERE userlist.USER_NAME = ? ";

        Cursor c = Main_activity.newDB.rawQuery(selectSearch, data);

        int count = c.getCount();
        if (count == 0) {

            new searchcontact().execute(name);
        }else{
            Toast.makeText(getApplicationContext(), "User already exist", Toast.LENGTH_LONG).show();
        }
        c.close();
    }

    private void createnewkey(){
        Intent i = new Intent(this, NewKey_activity.class);
        startActivityForResult(i, 1);
    }

    private void differentkey(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Secure Chat");
        builder.setMessage("Your Key is Wrong!\nPlease enter Revoke Key:");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new revokekey().execute(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Main_activity.editor.clear();
                Main_activity.editor.commit();

                openlogin();
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    private class revokekey extends AsyncTask<String, Integer, Double> {

        protected Double doInBackground(String... params) {
            // TODO Auto-generated method stub
            postData(params[0]);
            return null;
        }

        protected void onPostExecute(Double result){
            //Toast.makeText(getApplicationContext(), "command sent", Toast.LENGTH_LONG).show();
        }
        protected void onProgressUpdate(Integer... progress){
        }

        public void postData(String valueIWantToSend1) {


            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/revokekey.php");

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", Main_activity.user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", Main_activity.userpasswordhash));
                nameValuePairs.add(new BasicNameValuePair("userrevokekey", Crypto.hashpassword(valueIWantToSend1, Main_activity.userpassword)));
                nameValuePairs.add(new BasicNameValuePair("key", "16485155612574852"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();

                String line = null;
                try {
                    while ((line = reader.readLine()) != null) {
                        sb.append((line));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                resp = sb.toString();
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }

            runOnUiThread(new Runnable() {
                public void run() {

                    String[] splitResult = String.valueOf(resp).split("::");

                    if(splitResult[0].equals("login_false")) {

                        Toast.makeText(getApplicationContext(), "Login not Successful", Toast.LENGTH_LONG).show();
                        openlogin();

                    }else if(splitResult[0].equals("login_true")){

                        if(splitResult[1].equals("revokekey_true")) {

                            Toast.makeText(getApplicationContext(), "Revoke Key correct", Toast.LENGTH_LONG).show();

                            if(splitResult[2].equals("delete_true")) {

                                Main_activity.editor.putString("RSA_PUBLIC_KEY", "");
                                Main_activity.editor.putString("RSA_PRIVATE_KEY", "");
                                Main_activity.editor.putBoolean("key", false);
                                Main_activity.editor.commit();
                                createnewkey();
                            }else {
                                Toast.makeText(getApplicationContext(), "Error Please try again", Toast.LENGTH_LONG).show();
                            }

                        }else{

                            Toast.makeText(getApplicationContext(), "Revoke Key false", Toast.LENGTH_LONG).show();
                            differentkey();
                        }


                    }else {

                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }

    private class searchcontact extends AsyncTask<String, Integer, Double> {

        protected Double doInBackground(String... params) {
            // TODO Auto-generated method stub
            postData(params[0]);
            return null;
        }

        protected void onPostExecute(Double result){
            //Toast.makeText(getApplicationContext(), "command sent", Toast.LENGTH_LONG).show();
        }
        protected void onProgressUpdate(Integer... progress){
        }

        public void postData(String valueIWantToSend1) {


            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/newcontact.php");

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", Main_activity.user.getString("USER_NAME","")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", Main_activity.userpasswordhash));
                nameValuePairs.add(new BasicNameValuePair("usercontact", valueIWantToSend1));
                nameValuePairs.add(new BasicNameValuePair("key", "16485155612574852"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();

                String line = null;
                try {
                    while ((line = reader.readLine()) != null) {
                        sb.append((line));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                resp = sb.toString();
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }

            runOnUiThread(new Runnable() {
                public void run() {

                    String[] splitResult = String.valueOf(resp).split("::");
                    if (splitResult[0].equals("login_true")) {

                        if (!splitResult[1].equals("no_user")) {

                            try {
                                Main_activity.datasourceUser.createUserEntry(splitResult[1], splitResult[2], splitResult[3]);
                                Main_activity.datasourceChat.createChatEntry(Long.parseLong(splitResult[1]), Main_activity.user.getString("USER_ID", "0"), splitResult[1], "", "true", "0", "true");
                                Toast.makeText(getApplicationContext(), "Add new User", Toast.LENGTH_LONG).show();

                            }finally {

                            }

                        }else{
                            searchforuser();
                            Toast.makeText(getApplicationContext(), "No User" , Toast.LENGTH_LONG).show();
                        }

                    }else {

                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }

    private class sendMessage extends AsyncTask<String, Integer, Double> {

        protected Double doInBackground(String... params) {
            // TODO Auto-generated method stub

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDateandTime = sdf.format(Calendar.getInstance().getTime());

            postData(params[0],params[1],currentDateandTime);
            return null;
        }

        protected void onPostExecute(Double result){
            //Toast.makeText(getApplicationContext(), "command sent", Toast.LENGTH_LONG).show();
        }
        protected void onProgressUpdate(Integer... progress){
        }

        public void postData(final String message,final String inserid,final String date) {


            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://schisskiss.no-ip.biz/SecureChat/chatmessage.php");

            try {

                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("username", Main_activity.user.getString("USER_NAME", "")));
                nameValuePairs.add(new BasicNameValuePair("userpassword", Main_activity.userpasswordhash));
                nameValuePairs.add(new BasicNameValuePair("message", message));
                nameValuePairs.add(new BasicNameValuePair("date", date));
                nameValuePairs.add(new BasicNameValuePair("receiverid", Long.toString(userid)));
                nameValuePairs.add(new BasicNameValuePair("key", "16485155612574852"));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();

                String line = null;
                try {
                    while ((line = reader.readLine()) != null) {
                        sb.append((line));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                resp = sb.toString();
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
            } catch (IOException e) {
                // TODO Auto-generated catch block
            }

            runOnUiThread(new Runnable() {
                public void run() {

                    String[] splitResult = String.valueOf(resp).split("::");

                    if(splitResult[0].equals("login_false")) {

                        openlogin();
                        finish();

                    }else if(splitResult[0].equals("login_true")){

                        if(splitResult[1].equals("message_send")){

                            Main_activity.datasourceChat.createChatEntry(userid, Main_activity.user.getString("USER_ID","0"),
                                    Long.toString(userid), message, "true", date, "true");

                            texttosend.setText("");

                            openAndQueryDatabase();
                            displayResultList();

                        }

                    }else {

                        Toast.makeText(getApplicationContext(), "Error" , Toast.LENGTH_LONG).show();
                    }
                }
            });


        }

    }
}
