package com.soverixnet.soveravpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Build;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

public final class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(4, 11, 32);
    private static final int PANEL = Color.rgb(14, 34, 77);
    private static final int GOLD = Color.rgb(247, 210, 117);
    private static final int PALE = Color.rgb(186, 203, 237);
    private ApiClient api;
    private SessionStore sessions;
    private String sessionToken;
    private String tier = "free";
    private TextView statusText, accountText, serverText, tierText;
    private EditText emailField, passwordField;
    private Button loginButton, registerButton, connectButton, vipButton, logoutButton;
    private LinearLayout serversContainer;

    private int dp(float value) { return (int) (getResources().getDisplayMetrics().density * value + .5f); }

    private GradientDrawable shape(int color, int radius, int strokeColor) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color); bg.setCornerRadius(dp(radius));
        if (strokeColor != 0) bg.setStroke(dp(1), strokeColor);
        return bg;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view;
    }

    private LinearLayout.LayoutParams block(int marginTop) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(marginTop); return lp;
    }

    private Button button(String value, boolean primary) {
        Button view = new Button(this);
        view.setText(value); view.setAllCaps(false);
        view.setTextColor(primary ? NAVY : Color.WHITE);
        view.setBackground(shape(primary ? GOLD : PANEL, 14, primary ? 0 : 0xFF385EAE));
        view.setMinHeight(dp(52));
        return view;
    }

    private LinearLayout card() {
        LinearLayout view = column(); view.setPadding(dp(20), dp(18), dp(20), dp(18));
        view.setBackground(shape(PANEL, 20, 0xFF2F518A)); return view;
    }

    private EditText edit(String hint, boolean password) {
        EditText view = new EditText(this);
        view.setSingleLine(true); view.setTextColor(Color.WHITE); view.setHintTextColor(0xFF98AAD0);
        view.setTextSize(15); view.setHint(hint); view.setPadding(dp(14), dp(12), dp(14), dp(12));
        view.setInputType(password ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        view.setBackground(shape(0xFF071938, 10, 0xFF4669A0));
        return view;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        api = new ApiClient(BuildConfig.SOVERA_API_BASE_URL);
        sessions = new SessionStore(this);
        sessionToken = sessions.load();
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true); scroll.setBackgroundColor(NAVY);
        LinearLayout root = column(); root.setPadding(dp(20), dp(12), dp(20), dp(28));
        scroll.addView(root);
        if (Build.VERSION.SDK_INT >= 30) {
            scroll.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        }
        setContentView(scroll);

        LinearLayout hero = column(); hero.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.sovera_logo); logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(220), dp(220));
        hero.addView(logo, logoParams);
        TextView subtitle = text("PRIVATE • SECURE • GLOBAL", 12, GOLD, true); subtitle.setGravity(Gravity.CENTER);
        hero.addView(subtitle); root.addView(hero);

        LinearLayout connection = card(); root.addView(connection, block(20));
        statusText = text("NOT CONNECTED", 23, GOLD, true); statusText.setGravity(Gravity.CENTER);
        connection.addView(statusText);
        TextView note = text("A real VPN server must be configured before connecting.", 13, PALE, false);
        note.setGravity(Gravity.CENTER); connection.addView(note, block(8));
        connectButton = button("CONNECT", true); connectButton.setEnabled(false);
        connectButton.setAlpha(.5f); connection.addView(connectButton, block(16));
        connectButton.setOnClickListener(v -> alert("VPN transport", "An authorized VPN endpoint and protocol engine have not been integrated yet."));

        LinearLayout server = card(); root.addView(server, block(14));
        server.addView(text("SERVER LOCATION", 12, GOLD, true));
        serverText = text("No active servers", 18, Color.WHITE, true);
        server.addView(serverText, block(8));
        TextView serverHint = text("Cloudflare manages server information. A separate encrypted VPN gateway is required for traffic.", 12, PALE, false);
        server.addView(serverHint, block(10));
        serversContainer = column(); server.addView(serversContainer, block(10));

        LinearLayout account = card(); root.addView(account, block(14));
        account.addView(text("ACCOUNT", 12, GOLD, true));
        accountText = text("Guest • Sign in to view servers", 14, PALE, false); account.addView(accountText, block(8));
        emailField = edit("Email address", false); account.addView(emailField, block(12));
        passwordField = edit("Password (12+ characters)", true); account.addView(passwordField, block(10));
        loginButton = button("Sign in", true); account.addView(loginButton, block(12));
        registerButton = button("Create account", false); account.addView(registerButton, block(9));
        loginButton.setOnClickListener(v -> authorize(false)); registerButton.setOnClickListener(v -> authorize(true));
        logoutButton = button("Sign out", false); logoutButton.setVisibility(View.GONE);
        account.addView(logoutButton, block(9)); logoutButton.setOnClickListener(v -> logout());

        LinearLayout premium = card(); root.addView(premium, block(14));
        premium.addView(text("SOVERA VIP", 18, GOLD, true));
        tierText = text("Free plan • No ads at launch", 14, Color.WHITE, false); premium.addView(tierText, block(9));
        premium.addView(text("VIP will offer premium server access after Google Play Billing and verified entitlement are activated. All plans are currently ad-free.", 13, PALE, false), block(7));
        vipButton = button("Monthly VIP • $3/month (planned)", false); vipButton.setEnabled(false);
        premium.addView(vipButton, block(14));
        if (api.isConfigured()) refreshPlans();
        if (sessionToken != null) refreshAccount();
    }

    private void alert(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show();
    }

    private void authorize(boolean register) {
        if (!api.isConfigured()) { alert("API not ready", "Cloudflare management API has not been deployed or configured."); return; }
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString();
        if (email.isEmpty() || password.length() < 12) { alert("Account", "Enter a valid email and password with at least 12 characters."); return; }
        JSONObject body = new JSONObject();
        try { body.put("email", email); body.put("password", password); } catch (Exception ignored) { return; }
        loginButton.setEnabled(false);
        api.request(register ? "/v1/auth/register" : "/v1/auth/login", "POST", body, null, (response,error) ->
            runOnUiThread(() -> {
                loginButton.setEnabled(true);
                if (error != null) { alert("Account", error); return; }
                if (register) { alert("Account created", "Your account is ready. Please sign in."); return; }
                sessionToken = response.optString("token", null);
                passwordField.setText("");
                try { sessions.save(sessionToken); }
                catch (Exception saveError) {
                    sessionToken = null;
                    alert("Secure session", "Could not securely save login. Please check your device security settings.");
                    return;
                }
                refreshAccount();
            }));
    }

    private void logout() {
        final String oldToken = sessionToken;
        sessionToken = null;
        sessions.clear();
        tier = "free";
        accountText.setText("Guest • Sign in to view servers");
        tierText.setText("Free plan • No ads at launch");
        logoutButton.setVisibility(View.GONE);
        emailField.setVisibility(View.VISIBLE);
        passwordField.setVisibility(View.VISIBLE);
        loginButton.setVisibility(View.VISIBLE);
        registerButton.setVisibility(View.VISIBLE);
        serverText.setText("Sign in to view servers");
        serversContainer.removeAllViews();
        if (oldToken != null && api.isConfigured()) {
            api.request("/v1/auth/logout", "POST", null, oldToken, (response, error) -> {
                if (error != null) runOnUiThread(() -> alert("Signed out on device", "The server could not be reached to revoke the session; it will expire automatically."));
            });
        }
    }

    private void refreshAccount() {
        if (sessionToken == null) return;
        api.request("/v1/me", "GET", null, sessionToken, (response,error) -> runOnUiThread(() -> {
            if (error != null) { accountText.setText("Session unavailable: " + error); return; }
            tier = response.optString("tier", "free");
            accountText.setText(response.optString("email", "Signed in"));
            tierText.setText("vip".equals(tier) ? "VIP plan • No ads" : "Free plan • No ads at launch");
            logoutButton.setVisibility(View.VISIBLE);
            emailField.setVisibility(View.GONE);
            passwordField.setVisibility(View.GONE);
            loginButton.setVisibility(View.GONE);
            registerButton.setVisibility(View.GONE);
            loadServers();
        }));
    }

    private void refreshPlans() {
        api.request("/v1/plans", "GET", null, null, (response,error) -> runOnUiThread(() -> {
            if (error != null) return;
            vipButton.setText("Monthly VIP • $3/month (Play setup pending)");
        }));
    }

    private void loadServers() {
        api.request("/v1/servers", "GET", null, sessionToken, (response,error) -> runOnUiThread(() -> {
            if (error != null) { serverText.setText("Server list unavailable"); return; }
            JSONArray servers = response.optJSONArray("servers");
            serversContainer.removeAllViews();
            if (servers == null || servers.length() == 0) { serverText.setText("No available server locations yet"); return; }
            serverText.setText(servers.length() + " location(s) • Connections unavailable");
            for (int i = 0; i < servers.length(); i++) {
                JSONObject location = servers.optJSONObject(i);
                if (location == null) continue;
                LinearLayout item = column();
                item.setPadding(dp(13), dp(12), dp(13), dp(12));
                item.setBackground(shape(0xFF071938, 12, 0xFF385EAE));
                String name = location.optString("name", "Server");
                String country = location.optString("country_code", "");
                item.addView(text(name + " • " + country, 15, Color.WHITE, true));
                item.addView(text(location.optString("protocol", "VPN").toUpperCase(java.util.Locale.ROOT)
                    + "  •  " + (location.optBoolean("vip_only") ? "VIP" : "FREE")
                    + "  •  Not connected", 12, PALE, false), block(5));
                serversContainer.addView(item, block(8));
            }
        }));
    }
}
