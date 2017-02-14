package eu.nioc.tumblrbrowse.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;
import com.github.scribejava.core.oauth.OAuth10aService;
import com.google.gson.Gson;
import com.tumblr.jumblr.JumblrClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import eu.nioc.tumblrbrowse.BuildConfig;
import eu.nioc.tumblrbrowse.R;
import eu.nioc.tumblrbrowse.utils.OauthTumblrApi;

/**
 * Handle account management (add account with OAuth 1.0a)
 */
public class AccountActivity extends AppCompatActivity {

    private Boolean isWebviewOpen = false;
    private ArrayList accounts;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        //setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_account);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.account_activity_title);
        getSupportActionBar().setSubtitle(R.string.account_activity_choose_subtitle);

        //@FIXME: switch to async task
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //get configured accounts
        accounts = getStoredAccounts();

        final ListView accountListView = (ListView) findViewById(R.id.accounts);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, accounts);
        accountListView.setAdapter(adapter);
        accountListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //save current blog
                String selectedAccount = (String) accountListView.getItemAtPosition(position);
                SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("currentBlog", selectedAccount);
                editor.commit();

                //notify user
                Toast.makeText(AccountActivity.this, getString(R.string.alert_current_blog, selectedAccount), Toast.LENGTH_SHORT).show();

                //move to main activity in a new task (clear history stack)
                Intent intent = new Intent(AccountActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
        accountListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                //remove blog
                final String selectedAccount = (String) accountListView.getItemAtPosition(position);
                new AlertDialog.Builder(view.getContext())
                        //prepare dialog
                        .setIcon(R.drawable.ic_warning)
                        .setTitle(R.string.delete_account_title)
                        .setMessage(getString(R.string.delete_account_message, selectedAccount))
                        //"Yes" button will remove blog
                        .setPositiveButton(R.string.delete_account_positive_label, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                removeAccount(selectedAccount);
                            }
                        })
                        //"No" button will simply close dialog
                        .setNegativeButton(R.string.delete_account_negative_label, null)
                        //show dialog
                        .show();
                return false;
            }
        });

        if (accounts.size() == 0) {
            //there is no account set, open Tumblr connection webview for adding one
            addAccount();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.account_toolbar, menu);
        //show/hide buttons according to webview visibility
        menu.findItem(R.id.close_webview).setVisible(isWebviewOpen);
        menu.findItem(R.id.add_account).setVisible(!isWebviewOpen);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_account:
                addAccount();
                return true;

            case R.id.close_webview:
                hideWebview();
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    /**
     * Retrieve previous connected accounts stored in application memory
     * @return existing blogs name as string collection
     */
    private ArrayList<String> getStoredAccounts() {
        SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), 0);
        String strAccounts = settings.getString("accounts", "[]");
        Gson gson = new Gson();
        return new ArrayList<>(Arrays.asList(gson.fromJson(strAccounts, String[].class)));
    }

    /**
     * Start logic for adding a new account
     */
    private void addAccount() {

        //notify toolbar that webview is opened for switching buttons
        isWebviewOpen = true;
        getSupportActionBar().setSubtitle(R.string.account_activity_add_subtitle);
        invalidateOptionsMenu();

        //instantiate Oauth service
        final OAuth10aService service = new ServiceBuilder()
                .apiKey(BuildConfig.TUMBLR_API_CONSUMER_KEY)
                .apiSecret(BuildConfig.TUMBLR_API_CONSUMER_SECRET)
                .callback("https://www.tumblr.com/")
                .build(OauthTumblrApi.instance());

        //setup webview for hosting the Tumblr GUI which require JavaScript
        WebView webview = (WebView)findViewById(R.id.webview);
        webview.setVisibility(View.VISIBLE);
        webview.getSettings().setJavaScriptEnabled(true);
        try {
            final OAuth1RequestToken requestToken = service.getRequestToken();
            webview.setWebViewClient(new WebViewClient() {
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    //parse url for getting token and verifier
                    String oauthTokenFragment = "oauth_token";
                    String oauthVerifierFragment = "oauth_verifier";

                    Uri uri = Uri.parse(url);
                    String oauthToken = uri.getQueryParameter(oauthTokenFragment);
                    String oauthVerifier = uri.getQueryParameter(oauthVerifierFragment);

                    if (oauthToken != null && oauthVerifier != null) {
                        //Tumblr provided both token and verifier, stop browsing and request a token secret
                        hideWebview();
                        try {
                            OAuth1AccessToken accessToken = service.getAccessToken(requestToken, oauthVerifier);
                            saveToken(accessToken.getToken(), accessToken.getTokenSecret());

                        } catch (IOException | InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });

            //load Tumblr connect page
            webview.loadUrl(service.getAuthorizationUrl(requestToken));

        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stop navigation, close webview and refresh toolbar
     */
    private void hideWebview() {
        WebView webview = (WebView)findViewById(R.id.webview);
        webview.stopLoading();
        webview.getSettings().setJavaScriptEnabled(false);
        webview.setVisibility(View.GONE);
        isWebviewOpen = false;
        getSupportActionBar().setSubtitle(R.string.account_activity_choose_subtitle);
        invalidateOptionsMenu();
    }

    /**
     * Retrieve and store blog information after authentication
     * @param token provided token
     * @param tokenSecret provided token secret
     */
    private void saveToken(String token, String tokenSecret) {
        JumblrClient client = new JumblrClient(BuildConfig.TUMBLR_API_CONSUMER_KEY, BuildConfig.TUMBLR_API_CONSUMER_SECRET);
        client.setToken(token, tokenSecret);
        //retrieve blog information (name, followings)
        String name = client.user().getName();
        Integer following = client.user().getFollowingCount();

        if (name != null) {
            ArrayList storedAccounts = getStoredAccounts();
            if(!storedAccounts.contains(name)) {
                //add account to stored accounts
                storedAccounts.add(name);
                SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), 0);
                SharedPreferences.Editor editor = settings.edit();
                Gson gson = new Gson();
                editor.putString("accounts", gson.toJson(storedAccounts));
                editor.commit();
                //update accounts list
                this.accounts.clear();
                this.accounts.addAll(storedAccounts);
                this.adapter.notifyDataSetChanged();
            }

            //store account information in dedicated memory
            SharedPreferences settings = getSharedPreferences(name, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("oauthToken", token);
            editor.putString("oauthVerifier", tokenSecret);
            editor.putInt("following", following);
            editor.commit();

            //notify user
            Toast.makeText(AccountActivity.this, getString(R.string.alert_add_blog_success, name), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Remove all the account stored data and update accounts list
     * @param accountToRemove account name to be removed
     */
    private void removeAccount(String accountToRemove) {
        ArrayList storedAccounts = getStoredAccounts();
        if (storedAccounts.contains(accountToRemove)) {
            //remove account from stored accounts and save list
            storedAccounts.remove(accountToRemove);
            SharedPreferences settings = getSharedPreferences(getString(R.string.app_name), 0);
            SharedPreferences.Editor editor = settings.edit();
            Gson gson = new Gson();
            editor.putString("accounts", gson.toJson(storedAccounts));
            editor.apply();
            //update accounts list
            this.accounts.clear();
            if (storedAccounts.size() > 0) {
                this.accounts.addAll(storedAccounts);
            }
            this.adapter.notifyDataSetChanged();
        }
        //remove account stored data
        SharedPreferences settings = getSharedPreferences(accountToRemove, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.clear();
        editor.apply();
    }
}
