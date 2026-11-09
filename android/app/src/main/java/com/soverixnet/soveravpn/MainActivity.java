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
import org.json.JSONObject;

public final class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(4, 11, 32);
    private static final int PANEL = Color.rgb(14, 34, 77);
    private static final int GOLD = Color.rgb(247, 210, 117);
    private static final int PALE = Color.rgb(186, 203, 237);
    private ApiClient api;
    private String sessionToken;
    private String tier = "free";
    private TextView statusText, accountText, serverText, tierText, adsText;
    private EditText emailField, passwordField;
    private Button loginButton, connectButton, vipButton;

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
        TextView serverHint = text("Cloudflare provides account and server metadata. A separate VPN endpoint carries traffic.", 12, PALE, false);
        server.addView(serverHint, block(10));

        LinearLayout account = card(); root.addView(account, block(14));
        account.addView(text("ACCOUNT", 12, GOLD, true));
        accountText = text("Guest • Sign in to view servers", 14, PALE, false); account.addView(accountText, block(8));
        emailField = edit("Email address", false); account.addView(emailField, block(12));
        passwordField = edit("Password (12+ characters)", true); account.addView(passwordField, block(10));
        loginButton = button("Sign in", true); account.addView(loginButton, block(12));
        Button register = button("Create account", false); account.addView(register, block(9));
        loginButton.setOnClickListener(v -> authorize(false)); register.setOnClickListener(v -> authorize(true));

        LinearLayout premium = card(); root.addView(premium, block(14));
        premium.addView(text("SOVERA VIP", 18, GOLD, true));
        tierText = text("Free plan • Ads supported", 14, Color.WHITE, false); premium.addView(tierText, block(9));
        premium.addView(text("VIP offers ad-free usage and premium servers after Google Play Billing and verified entitlement are activated.", 13, PALE, false), block(7));
        vipButton = button("Monthly VIP • Coming soon", false); vipButton.setEnabled(false);
        premium.addView(vipButton, block(14));
        adsText = text("FREE PLAN AD SPACE — AdMob not configured", 11, PALE, true);
        adsText.setGravity(Gravity.CENTER); root.addView(adsText, block(26));
        if (api.isConfigured()) refreshPlans();
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
                refreshAccount();
            }));
    }

    private void refreshAccount() {
        if (sessionToken == null) return;
        api.request("/v1/me", "GET", null, sessionToken, (response,error) -> runOnUiThread(() -> {
            if (error != null) { accountText.setText("Session unavailable: " + error); return; }
            tier = response.optString("tier", "free");
            accountText.setText(response.optString("email", "Signed in"));
            tierText.setText("vip".equals(tier) ? "VIP plan • Ad-free" : "Free plan • Ads supported");
            adsText.setVisibility("vip".equals(tier) ? View.GONE : View.VISIBLE);
            loadServers();
        }));
    }

    private void refreshPlans() {
        api.request("/v1/plans", "GET", null, null, (response,error) -> runOnUiThread(() -> {
            if (error != null) return;
            vipButton.setText("Monthly VIP • Available after Play setup");
        }));
    }

    private void loadServers() {
        api.request("/v1/servers", "GET", null, sessionToken, (response,error) -> runOnUiThread(() -> {
            if (error != null) { serverText.setText("Server list unavailable"); return; }
            org.json.JSONArray servers = response.optJSONArray("servers");
            if (servers == null || servers.length() == 0) { serverText.setText("No active servers"); return; }
            serverText.setText(servers.length() + " server(s) listed • Transport not configured");
        }));
    }
}
