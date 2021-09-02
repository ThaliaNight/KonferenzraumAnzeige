package com.example.konferenzraumanzeige;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;

import java.util.Objects;

/**
 * The fragment tries to exchange a Token silently and opens the Calendar Fragment if it succeeds.
 * If the Silent exchange doesn't work it opens a Login Screen.
 */


public class LoadingFragment extends Fragment {

    private static final String SAVED_IS_SIGNED_IN = "isSignedIn";
    private static final String SAVED_USER_NAME = "userName";
    private static final String SAVED_USER_EMAIL = "userEmail";
    private static final String SAVED_USER_TIMEZONE = "userTimeZone";

    private Handler mHandler;
    private AuthenticationHelper mAuthHelper;
    private Information mInf;

    public LoadingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mInf = Information.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mInf.setSignedIn(savedInstanceState.getBoolean(SAVED_IS_SIGNED_IN));
            mInf.setUserName(savedInstanceState.getString(SAVED_USER_NAME));
            mInf.setUserEmail(savedInstanceState.getString(SAVED_USER_EMAIL));
            mInf.setUserTimeZone(savedInstanceState.getString(SAVED_USER_TIMEZONE));

        }
        AuthenticationHelper.getInstance(requireContext().getApplicationContext())
                .thenAccept(authHelper -> {
                    mAuthHelper = authHelper;
                    if (!mInf.getSignedIn()) {
                        doSilentSignIn();
                    }
                })
                .exceptionally(exception -> {
                    Log.e("AUTH", "Error creating auth helper", exception);
                    return null;
                });
        return inflater.inflate(R.layout.fragment_loading, container, false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_IS_SIGNED_IN, mInf.getSignedIn());
        outState.putString(SAVED_USER_NAME, mInf.getUserName());
        outState.putString(SAVED_USER_EMAIL, mInf.getUserEmail());
        outState.putString(SAVED_USER_TIMEZONE, mInf.getUserTimeZone());
    }


    private void doSilentSignIn() {
        mAuthHelper.acquireTokenSilently()
                .thenAccept(this::handleSignInSuccess)
                .exceptionally(exception -> {
                    // Check the type of exception and handle appropriately
                    Throwable cause = exception.getCause();
                    if (cause instanceof MsalUiRequiredException) {
                        Log.d("AUTH", "Interactive login required");
                        doInteractiveSignIn();
                    } else if (cause instanceof MsalClientException) {
                        MsalClientException clientException = (MsalClientException)cause;
                        if (Objects.equals(clientException.getErrorCode(), "no_current_account") ||
                                Objects.equals(clientException.getErrorCode(), "no_account_found")) {
                            Log.d("AUTH", "No current account, interactive login required");
                            doInteractiveSignIn();
                        }
                    } else {
                        handleSignInFailure(cause);
                    }
                    return null;
                });
    }

    private void handleSignInSuccess(IAuthenticationResult authenticationResult) {
        // Log the token for debug purposes
        String accessToken = authenticationResult.getAccessToken();
        Log.d("AUTH", String.format("Access token: %s", accessToken));

        // Get Graph client and get user
        GraphHelper graphHelper = GraphHelper.getInstance();
        graphHelper.getUser()
                .thenAccept(user -> {
                    mInf.setUserName(user.displayName);
                    mInf.setUserEmail(user.mail == null ? user.userPrincipalName : user.mail);
                    assert user.mailboxSettings != null;
                    mInf.setUserTimeZone(user.mailboxSettings.timeZone);
                    mInf.setSignedIn(true);
                    mHandler.post(() -> Navigation.findNavController(requireActivity().findViewById(R.id.nav_host_fragment)).navigate(R.id.action_loadingFragment_to_calendarFragment));
                })
                .exceptionally(exception -> {
                    Log.e("AUTH", "Error getting /me", exception);
                    mInf.setSignedIn(false);
                    return null;
                });
    }

    private void handleSignInFailure(Throwable exception) {
        if (exception instanceof MsalServiceException) {
            // Exception when communicating with the auth server, likely config issue
            Log.e("AUTH", "Service error authenticating", exception);
        } else if (exception instanceof MsalClientException) {
            // Exception inside MSAL, more info inside MsalError.java
            Log.e("AUTH", "Client error authenticating", exception);
        } else {
            Log.e("AUTH", "Unhandled exception authenticating", exception);
        }
    }

    private void doInteractiveSignIn() {
        Navigation.findNavController(requireActivity().findViewById(R.id.nav_host_fragment)).navigate(R.id.action_loadingFragment_to_homeFragment);
    }
}
