package com.bricenangue.nextgeneration.ebuycamer;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.login.widget.ProfilePictureView;
import com.firebase.ui.FirebaseUI;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ui.ResultCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private static final int FB_SIGN_IN=0;
    private DatabaseReference root;
    private StorageReference storageRoot;
    private UserSharedPreference userSharedPreference;
    private ProgressDialog progressBar;
    private FirebaseUser user;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        userSharedPreference=new UserSharedPreference(this);

        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int code = api.isGooglePlayServicesAvailable(this);
        if (code == ConnectionResult.SUCCESS) {
            // Do Your Stuff Here

            auth =FirebaseAuth.getInstance();
            root=FirebaseDatabase.getInstance().getReference();
            storageRoot=FirebaseStorage.getInstance().getReference();
            showProgressbar();

            if(auth!=null){
                showProgressbar();
                user=auth.getCurrentUser();
                if(user != null ){
                    //user logged in

                    List<? extends UserInfo> list=user.getProviderData();
                    String providerId=list.get(1).getProviderId();

                    if(providerId.equals(getString(R.string.facebook_provider_id))) {

                        procide(user);

                    }else if (providerId.equals(getString(R.string.password_provider_id))
                            && user.isEmailVerified()){

                        procide(user);

                    }else if (providerId.equals(getString(R.string.password_provider_id))
                            && !user.isEmailVerified()) {

                        verifyMail(user);
                    }else {
                        auth.signOut();
                        dismissProgressbar();
                        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
                                .setProviders(AuthUI.EMAIL_PROVIDER,
                                        AuthUI.FACEBOOK_PROVIDER)
                                .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                                .setTheme(R.style.AppTheme)
                                .build(),FB_SIGN_IN);

                    }


                }else {

                    dismissProgressbar();

                    startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
                            .setProviders(AuthUI.EMAIL_PROVIDER,
                                    AuthUI.FACEBOOK_PROVIDER)
                            .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                            .setTheme(R.style.AppTheme)
                            .build(),FB_SIGN_IN);
                }
            }else {
                dismissProgressbar();

                startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
                        .setProviders(AuthUI.EMAIL_PROVIDER,
                                AuthUI.FACEBOOK_PROVIDER)
                        .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                        .setTheme(R.style.AppTheme)
                        .build(),FB_SIGN_IN);
            }
        } else {

            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(code, this, 0);
            if (dialog != null) {
                //This dialog will help the user update to the latest GooglePlayServices
                dialog.show();


            }
        }

    }

    private void procide(final FirebaseUser user) {
        final DatabaseReference ref=root.child(ConfigApp.FIREBASE_APP_URL_USERS).
                child(user.getUid()).child("userPublic")
                ;
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userSharedPreference.setUserNumberofAds(dataSnapshot
                        .child("numberOfAds").getValue(long.class));
                ref.child("name").setValue(user.getDisplayName());
                if (dataSnapshot.hasChild("Location")){
                    userSharedPreference.storeUserLocation(dataSnapshot.child("Location").getValue(Locations.class).getName());
                    startActivity(new Intent(MainActivity.this,CategoryActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();
                    dismissProgressbar();

                }else {

                    startActivity(new Intent(MainActivity.this,LocationsActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                    finish();
                    dismissProgressbar();

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                dismissProgressbar();
                Toast.makeText(getApplicationContext(),databaseError.getMessage() + "ici"
                ,Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void verifyMail(final FirebaseUser user) {

        final AlertDialog alertDialog =
                new AlertDialog.Builder(MainActivity.this).setMessage(
                        getString(R.string.alertDialogverifyemail)+" " +user.getEmail())
                        .create();
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "OK"
                , new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //user.sendEmailVerification();

                       procide(user);
                        alertDialog.dismiss();
                        dismissProgressbar();

                    }

                });
        alertDialog.setCancelable(false);
        alertDialog.show();

    }
    private void saveprofilePicture(FirebaseUser user){
        String facebookUserId = "";
        // find the Facebook profile and get the user's id
        for(UserInfo profile : user.getProviderData()) {
            // check if the provider id matches "facebook.com"
            if(profile.getProviderId().equals(getString(R.string.facebook_provider_id))) {
                facebookUserId = profile.getUid();
            }
        }
        // construct the URL to the profile picture, with a custom height
        // alternatively, use '?type=small|medium|large' instead of ?height=
        final String photoUrl = "https://graph.facebook.com/" + facebookUserId + "/picture?type=large";
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference();
        reference.child(ConfigApp.FIREBASE_APP_URL_USERS).child(auth.getCurrentUser().getUid())
                .child("userPublic").child("profilePhotoUri").setValue(photoUrl)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==FB_SIGN_IN){
            if(resultCode==RESULT_OK){

                boolean isfacebook = false;
                boolean ispassword =false;
                final FirebaseUser user=auth.getCurrentUser();

                assert user != null;
                for(UserInfo profile : user.getProviderData()) {
                    // check if the provider id matches "facebook.com"
                    if(profile.getProviderId().equals(getString(R.string.facebook_provider_id))) {

                        isfacebook=true;

                    }else if (profile.getProviderId().equals(getString(R.string.password_provider_id))){

                        ispassword=true;
                    }
                }
                final String email=user.getEmail();
                final String uid=user.getUid();
                final String name = user.getDisplayName();


                final DatabaseReference ref =root.child(ConfigApp.FIREBASE_APP_URL_USERS)
                        .child(uid);
                final UserPublic userfb=new UserPublic();

                final boolean emailVerified=user.isEmailVerified();
                userSharedPreference.setEmailVerified(emailVerified);
                userfb.setEmail(email);

                userfb.setName(name);

                userfb.setUniquefirebasebId(uid);

                DatabaseReference refAds=root.child(ConfigApp.FIREBASE_APP_URL_USERS_POSTS_USER).child(user.getUid());
                final boolean finalIsfacebook = isfacebook;
                final boolean finalIspassword = ispassword;
                refAds.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            //user already register
                            if(dataSnapshot.hasChildren()){
                                userfb.setNumberOfAds(dataSnapshot.getChildrenCount());
                                userSharedPreference.setUserNumberofAds(dataSnapshot.getChildrenCount());
                                final DatabaseReference refUSerPublice=ref.child("userPublic");
                                refUSerPublice.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.hasChildren()){
                                            refUSerPublice.setValue(userfb).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isComplete() && task.isSuccessful()){
                                                        if(finalIsfacebook){
                                                            procide(user);
                                                            saveprofilePicture(user);

                                                        }else if ( !emailVerified && finalIspassword){
                                                            verifyMail(user);

                                                        }else if (emailVerified && finalIspassword ){
                                                            procide(user);
                                                        }
                                                    }else {
                                                        showErrorSignInAndRelaunch("7");
                                                        dismissProgressbar();

                                                    }
                                                }
                                            });
                                        }else {
                                            refUSerPublice.setValue(userfb).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isComplete() && task.isSuccessful()){
                                                        if(finalIsfacebook){
                                                            procide(user);
                                                            saveprofilePicture(user);


                                                        }else if ( !emailVerified && finalIspassword){
                                                            user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                    verifyMail(user);

                                                                }
                                                            });
                                                        }else if (emailVerified && finalIspassword ){
                                                            procide(user);
                                                        }
                                                    }else {
                                                        showErrorSignInAndRelaunch("1");
                                                        dismissProgressbar();

                                                    }
                                                }
                                            });
                                        }

                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        showErrorSignInAndRelaunch(databaseError.getMessage()+"5");
                                        dismissProgressbar();

                                    }
                                });
                            }else {
                                userfb.setNumberOfAds(0);
                                userSharedPreference.setUserNumberofAds(0);
                                final DatabaseReference refUSerPublice=ref.child("userPublic");
                                refUSerPublice.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.hasChildren()){
                                            refUSerPublice.setValue(userfb).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isComplete() && task.isSuccessful()){
                                                        if(finalIsfacebook){
                                                            procide(user);
                                                            saveprofilePicture(user);


                                                        }else if ( !emailVerified && finalIspassword){
                                                            verifyMail(user);

                                                        }else if (emailVerified && finalIspassword ){
                                                            procide(user);
                                                        }
                                                    }else {
                                                        showErrorSignInAndRelaunch("4");
                                                        dismissProgressbar();
                                                    }
                                                }
                                            });
                                        }else {
                                            refUSerPublice.setValue(userfb).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isComplete() && task.isSuccessful()){
                                                        if(finalIsfacebook){
                                                            procide(user);
                                                            saveprofilePicture(user);


                                                        }else if ( !emailVerified && finalIspassword){
                                                            user.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                    verifyMail(user);
                                                                }
                                                            });
                                                        }else if (emailVerified && finalIspassword ){
                                                            procide(user);
                                                        }
                                                    }else {
                                                      showErrorSignInAndRelaunch("3");
                                                        dismissProgressbar();
                                                    }
                                                }
                                            });
                                        }

                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        showErrorSignInAndRelaunch(databaseError.getMessage()+"2");
                                         dismissProgressbar();
                                    }
                                });

                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            showErrorSignInAndRelaunch(databaseError.getMessage()+"1");
                           dismissProgressbar();
                        }
                    });


            }


            // Sign in canceled
            if (resultCode == RESULT_CANCELED) {
               dismissProgressbar();
                Toast.makeText(getApplicationContext()
                        ,getString(R.string.connection_to_server_cancelled)
                        ,Toast.LENGTH_SHORT).show();
                onBackPressed();
                return;
            }

            // No network
            if (resultCode == ResultCodes.RESULT_NO_NETWORK) {
                dismissProgressbar();

                showErrorSignInAndRelaunch("result no network");
                return;
            }

        }
    }


    private void showErrorSignInAndRelaunch(String message){
       // getString(R.string.mainpage_alertdialog_signin_request_false)
        Toast.makeText(getApplicationContext()
                ,message
                ,Toast.LENGTH_SHORT).show();
        startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder()
                .setProviders(AuthUI.EMAIL_PROVIDER,
                        AuthUI.FACEBOOK_PROVIDER)
                .setIsSmartLockEnabled(!BuildConfig.DEBUG)
                .setTheme(R.style.AppTheme)
                .build(),FB_SIGN_IN);

    }

    @Override
    public void onBackPressed() {
        this.finishAffinity();
        System.exit(0);

    }
/**
    @Override
    public void setUsername(final String username) {
        final FirebaseUser user = auth.getCurrentUser();
        if(user!=null){
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(username).build();
            user.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()){
                        user.reload();
                        user.sendEmailVerification()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getApplicationContext()
                                                    ,getString(R.string.string_toast_text_success) + " email",Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                        Toast.makeText(getApplicationContext()
                                ,getString(R.string.string_toast_text_success) + " displayname",Toast.LENGTH_SHORT).show();

                        final String email=user.getEmail();
                        final String uid=user.getUid();
                        final String name = username;


                        final DatabaseReference ref =root.child(ConfigApp.FIREBASE_APP_URL_USERS)
                                .child(uid);
                        final UserPublic userfb=new UserPublic();

                        userfb.setEmail(email);
                        userfb.setName(name);

                        userfb.setUniquefirebasebId(uid);

                        DatabaseReference refAds=root.child(ConfigApp.FIREBASE_APP_URL_USERS_POSTS_USER).child(user.getUid());
                        refAds.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if(dataSnapshot.hasChildren()){
                                    userfb.setNumberOfAds(dataSnapshot.getChildrenCount());
                                    userSharedPreference.setUserNumberofAds(dataSnapshot.getChildrenCount());
                                    ref.child("userPublic").setValue(userfb).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                Toast.makeText(getApplicationContext(),
                                                        getString(R.string.mainpage_toast_loggingAs)+" "+ auth.getCurrentUser().getEmail()
                                                        ,Toast.LENGTH_SHORT).show();

                                                startActivity(new Intent(MainActivity.this,LocationsActivity.class)
                                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                                finish();
                                                if (progressBar!=null){
                                                    progressBar.dismiss();
                                                }
                                            }else {
                                                if (progressBar!=null){
                                                    progressBar.dismiss();
                                                }
                                            }
                                        }
                                    });
                                }else {
                                    userfb.setNumberOfAds(0);
                                    userSharedPreference.setUserNumberofAds(0);
                                    ref.child("userPublic").setValue(userfb).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if(task.isSuccessful()){
                                                Toast.makeText(getApplicationContext(),
                                                        getString(R.string.mainpage_toast_loggingAs)+" "+ auth.getCurrentUser().getEmail()
                                                        ,Toast.LENGTH_SHORT).show();

                                                startActivity(new Intent(MainActivity.this,LocationsActivity.class)
                                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                                finish();
                                                if (progressBar!=null){
                                                    progressBar.dismiss();
                                                }
                                            }else {
                                                if (progressBar!=null){
                                                    progressBar.dismiss();
                                                }
                                            }
                                        }
                                    });
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                if (progressBar!=null){
                                    progressBar.dismiss();
                                }
                            }
                        });
                    }else {
                        if (progressBar!=null){
                            progressBar.dismiss();
                        }
                        Toast.makeText(getApplicationContext()
                                ,getString(R.string.string_toast_text_error) + " displayname",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

**/
    @Override
    protected void onStop() {
        super.onStop();
       dismissProgressbar();
    }

    private void showProgressbar(){
        progressBar = new ProgressDialog(this);
        progressBar.setCancelable(false);
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.show();
    }

    private void dismissProgressbar(){
        if (progressBar!=null){
            progressBar.dismiss();
        }
    }
    @Override
    protected void onStart() {
        super.onStart();

    }
}

