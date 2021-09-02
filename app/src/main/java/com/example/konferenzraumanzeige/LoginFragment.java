package com.example.konferenzraumanzeige;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.konferenzraumanzeige.databinding.FragmentLoginBinding;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalServiceException;

import java.util.Objects;

/**
 * If someone clicks the Login Button of the Fragment it opens the Microsoft Login screen where the user needs to provide a Microsoft Account with a calendar.
 * When the Login succeeds the Calendar Fragment is opened.
 */

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AuthenticationHelper mAuthHelper = null;
    private Handler mHandler;
    private Information mInf;

    public LoginFragment() {
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInf = Information.getInstance();
        String loginText = this.getString(R.string.loginRequest);
        mHandler = new Handler();
        if (getArguments() != null) {
            mInf.setUserName(loginText);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        setButtons();
        return binding.getRoot();
    }

    private void setButtons() {
        Button mButton = binding.button;
        ImageButton mIconButton = binding.buttonSignIn;
        mButton.setOnClickListener(view -> doInteractiveSignIn());
        mIconButton.setOnClickListener(view -> doInteractiveSignIn());
    }

    private void doInteractiveSignIn() {
        AuthenticationHelper.getInstance(requireContext().getApplicationContext())
                .thenAccept(authHelper -> mAuthHelper=authHelper)
                .exceptionally(exception -> {
                    Log.e("AUTH", "Error creating auth helper", exception);
                    return null;
                });
        mAuthHelper.acquireTokenInteractively(getActivity())
                .thenAccept(this::handleSignInSuccess)
                .exceptionally(exception -> {
                    handleSignInFailure(exception);
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
                    mInf.setUserTimeZone(Objects.requireNonNull(user.mailboxSettings).timeZone);
                    mInf.setSignedIn(true);
                    mHandler.post(() -> Navigation.findNavController(requireActivity().findViewById(R.id.nav_host_fragment)).navigate(R.id.action_homeFragment_to_calendarFragment));                    //setSignedInState(true);
                })
                .exceptionally(exception -> {
                    Log.e("AUTH", "Error getting /me", exception);
                    mInf.setSignedIn(false);
                    //setSignedInState(false);
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
}