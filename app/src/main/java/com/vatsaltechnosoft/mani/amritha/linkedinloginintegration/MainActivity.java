package com.vatsaltechnosoft.mani.amritha.linkedinloginintegration;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.linkedin.platform.APIHelper;
import com.linkedin.platform.DeepLinkHelper;
import com.linkedin.platform.LISessionManager;
import com.linkedin.platform.errors.LIApiError;
import com.linkedin.platform.errors.LIAuthError;
import com.linkedin.platform.errors.LIDeepLinkError;
import com.linkedin.platform.listeners.ApiListener;
import com.linkedin.platform.listeners.ApiResponse;
import com.linkedin.platform.listeners.AuthListener;
import com.linkedin.platform.listeners.DeepLinkListener;
import com.linkedin.platform.utils.Scope;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private ImageView userImageView;
    private TextView userDetails;
    private Button signInButton, logoutButton;

    // Build the list of member permissions our LinkedIn session requires
    private static Scope buildScope() {

        return Scope.build(Scope.R_BASICPROFILE, Scope.R_EMAILADDRESS, Scope.W_SHARE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*getPackageHash();*/

        initViews();


    }

    /**
     * init the views by finding the ID of all views
     */
    private void initViews() {
        signInButton = findViewById(R.id.linkedin_login_button);
        logoutButton = findViewById(R.id.logout_button);
        userImageView = findViewById(R.id.user_profile_image_view);
        userDetails = findViewById(R.id.user_details_label);
    }

    private void getPackageHash() {
        try {

            @SuppressLint("PackageManagerGetSignatures") PackageInfo info = getPackageManager().getPackageInfo(
                    "com.vatsaltechnosoft.mani.amritha.linkedinloginintegration",
                    PackageManager.GET_SIGNATURES);
            for (android.content.pm.Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());

                Log.d(TAG, "Hash  : " + Base64.encodeToString(md.digest(), Base64.NO_WRAP));//Key hash is printing in Log
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, e.getMessage(), e);
        }

    }

    /**
     * on Sign In button do LinkedIn Authentication
     *
     * @param view
     */
    public void signInWithLinkedIn(View view) {
        // check if user is already authenticated or not and session is valid or not
        if (!LISessionManager.getInstance(this).getSession().isValid()) {
            //if not valid then start authentication
            LISessionManager.getInstance(getApplicationContext()).init(this, buildScope()//pass the build scope here
                    , new AuthListener() {
                        @Override
                        public void onAuthSuccess() {
                            // Authentication was successful.

                            Toast.makeText(MainActivity.this, "Successfully authenticated with LinkedIn.", Toast.LENGTH_SHORT).show();

                            //on successful authentication fetch basic profile data of user
                            fetchBasicProfileData();
                        }

                        @Override
                        public void onAuthError(LIAuthError error) {
                            // Handle authentication errors
                            Log.e(TAG, "Auth Error :" + error.toString());
                            Toast.makeText(MainActivity.this, "Failed to authenticate with LinkedIn. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }, true);//if TRUE then it will show dialog if
            // any device has no LinkedIn app installed to download app else won't show anything
        } else {
            Toast.makeText(this, "You are already authenticated.", Toast.LENGTH_SHORT).show();

            //if user is already authenticated fetch basic profile data for user
            fetchBasicProfileData();
        }
    }

    /**
     * method to fetch basic profile data
     */
    private void fetchBasicProfileData() {

        String url = "https://api.linkedin.com/v1/people/~:(id,first-name,last-name,headline,public-profile-url,picture-url,email-address,picture-urls::(original))";

        APIHelper apiHelper = APIHelper.getInstance(getApplicationContext());
        apiHelper.getRequest(this, url, new ApiListener() {
            @Override
            public void onApiSuccess(ApiResponse apiResponse) {
                // Success!
                Log.d(TAG, "API Res : " + apiResponse.getResponseDataAsString() + "\n" + apiResponse.getResponseDataAsJson().toString());
                Toast.makeText(MainActivity.this, "Successfully fetched LinkedIn profile data.", Toast.LENGTH_SHORT).show();

                //update UI on successful data fetched
                updateUI(apiResponse);
            }

            @Override
            public void onApiError(LIApiError liApiError) {
                // Error making GET request!
                Log.e(TAG, "Fetch profile Error   :" + liApiError.getLocalizedMessage());
                Toast.makeText(MainActivity.this, "Failed to fetch basic profile data. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * method to update UI
     *
     * @param apiResponse after fetching basic profile data
     */
    private void updateUI(ApiResponse apiResponse) {
        try {
            if (apiResponse != null) {
                JSONObject jsonObject = apiResponse.getResponseDataAsJson();

                //display user basic details
                userDetails.setText("Name : " + jsonObject.getString("firstName") + " " + jsonObject.getString("lastName") + "\nHeadline : " + jsonObject.getString("headline") + "\nEmail Id : " + jsonObject.getString("emailAddress"));

                //use the below string value to display small profile picture
                String smallPicture = jsonObject.getString("pictureUrl");

                //use the below json parsing for different profile pictures and big size images
                JSONObject pictureURLObject = jsonObject.getJSONObject("pictureUrls");
                if (pictureURLObject.getInt("_total") > 0) {
                    //get array of picture urls
                    JSONArray profilePictureURLArray = pictureURLObject.getJSONArray("values");
                    if (profilePictureURLArray != null && profilePictureURLArray.length() > 0) {
                        // get 1st image link and display using picasso
                        Picasso.with(this).load(profilePictureURLArray.getString(0))
                                .placeholder(R.mipmap.ic_launcher_round)
                                .error(R.mipmap.ic_launcher_round)
                                .into(userImageView);
                    }
                } else {
                    // if no big image is available then display small image using picasso
                    Picasso.with(this).load(smallPicture)
                            .placeholder(R.mipmap.ic_launcher_round)
                            .error(R.mipmap.ic_launcher_round)
                            .into(userImageView);
                }

                //show hide views
                signInButton.setVisibility(View.GONE);
                logoutButton.setVisibility(View.VISIBLE);
                userDetails.setVisibility(View.VISIBLE);
                userImageView.setVisibility(View.VISIBLE);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doLogout(View view) {
        //clear session on logout click
        LISessionManager.getInstance(this).clearSession();

        //show hide views
        signInButton.setVisibility(View.VISIBLE);
        logoutButton.setVisibility(View.GONE);
        userDetails.setVisibility(View.GONE);
        userImageView.setVisibility(View.GONE);

        //show toast
        Toast.makeText(this, "Logout successfully.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        LISessionManager.getInstance(getApplicationContext()).onActivityResult(this, requestCode, resultCode, data);

        DeepLinkHelper deepLinkHelper = DeepLinkHelper.getInstance();
        deepLinkHelper.onActivityResult(this, requestCode, resultCode, data);

    }

}
