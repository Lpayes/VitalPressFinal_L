package com.example.vitalpreesoficial;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;

/**
 * Helper sencillo para iniciar Google Sign-In y obtener un access token con los scopes
 * necesarios (cloud-platform y gmail.send).
 */
public class OAuthHelper {
    public interface TokenCallback {
        void onToken(String accessToken);
        void onError(String error);
        void onRecoverable(Intent recoverIntent);
    }

    public static Intent getSignInIntent(Activity activity, String clientId) {
        // Solicitar scopes: cloud-platform para Document AI y gmail.send para enviar correos
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope("https://www.googleapis.com/auth/cloud-platform"), new Scope("https://www.googleapis.com/auth/gmail.send"))
                .build();

        GoogleSignInClient client = GoogleSignIn.getClient(activity, gso);
        return client.getSignInIntent();
    }

    public static void handleSignInResult(int requestCode, int expectedRequestCode, Intent data, Activity activity, TokenCallback callback) {
        if (requestCode != expectedRequestCode) return;

        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult();
            if (account == null) {
                callback.onError("GoogleSignInAccount is null");
                return;
            }

            // Obtener token en background
            new FetchTokenTask(activity, account, callback).execute();
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    private static class FetchTokenTask extends AsyncTask<Void, Void, String> {
        private final Activity activity;
        private final GoogleSignInAccount account;
        private final TokenCallback callback;
        private Exception exception;

        FetchTokenTask(Activity activity, GoogleSignInAccount account, TokenCallback callback) {
            this.activity = activity;
            this.account = account;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // Pedir ambos scopes en la cadena oauth2:...
                String scope = "oauth2:https://www.googleapis.com/auth/cloud-platform https://www.googleapis.com/auth/gmail.send";
                String token = GoogleAuthUtil.getToken(activity, account.getAccount(), scope);
                return token;
            } catch (UserRecoverableAuthException urae) {
                this.exception = urae;
                return null;
            } catch (Exception e) {
                this.exception = e;
                return null;
            }
        }

        @Override
        protected void onPostExecute(String token) {
            if (token != null) {
                callback.onToken(token);
            } else if (exception instanceof UserRecoverableAuthException) {
                callback.onRecoverable(((UserRecoverableAuthException) exception).getIntent());
            } else if (exception != null) {
                callback.onError(exception.getMessage());
            } else {
                callback.onError("Unknown error obtaining token");
            }
        }
    }
}
