package com.myuang.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class SplashActivity extends BaseActivity {
    private static final int REQUEST_LOCATION = 41;
    private static final long LOCATION_WAIT_MS = 2200L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean languageResolved;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private TextView statusText;
    private TextView descriptionText;
    private TextView languageText;
    private ImageView imageLocation;
    private TextView textFlag;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        statusText = findViewById(R.id.textSplashStatus);
        descriptionText = findViewById(R.id.textSplashDescription);
        languageText = findViewById(R.id.textSplashLanguage);
        imageLocation = findViewById(R.id.imageSplashLocation);
        textFlag = findViewById(R.id.textSplashFlag);
        progressBar = findViewById(R.id.progressSplash);

        startPulseAnimation();
        startLocationLanguageDetection();
    }

    private void startPulseAnimation() {
        if (imageLocation == null) return;
        imageLocation.animate()
                .scaleX(1.15f)
                .scaleY(1.15f)
                .alpha(0.7f)
                .setDuration(700)
                .withEndAction(() -> {
                    if (!languageResolved && imageLocation != null) {
                        imageLocation.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .alpha(1.0f)
                                .setDuration(700)
                                .withEndAction(this::startPulseAnimation)
                                .start();
                    }
                })
                .start();
    }

    private void startLocationLanguageDetection() {
        if (!hasLocationPermission()) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_LOCATION);
            return;
        }
        detectLanguageFromLocation();
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void detectLanguageFromLocation() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location lastLocation = getBestLastKnownLocation();
        if (lastLocation != null) {
            resolveCountryFromLocation(lastLocation);
            return;
        }

        requestSingleLocationUpdate();
        handler.postDelayed(() -> finishWithCountry(resolveFallbackCountry()), LOCATION_WAIT_MS);
    }

    private Location getBestLastKnownLocation() {
        if (locationManager == null) {
            return null;
        }
        Location bestLocation = null;
        try {
            List<String> providers = locationManager.getProviders(true);
            if (providers != null) {
                for (String provider : providers) {
                    Location location = locationManager.getLastKnownLocation(provider);
                    if (location != null && (bestLocation == null || location.getTime() > bestLocation.getTime())) {
                        bestLocation = location;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return bestLocation;
    }

    private void requestSingleLocationUpdate() {
        if (locationManager == null) {
            return;
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                removeLocationUpdates();
                resolveCountryFromLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, locationListener, Looper.getMainLooper());
            } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, Looper.getMainLooper());
            } else {
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                String provider = locationManager.getBestProvider(criteria, true);
                if (provider != null) {
                    locationManager.requestSingleUpdate(provider, locationListener, Looper.getMainLooper());
                }
            }
        } catch (Exception ignored) {
            finishWithCountry(resolveFallbackCountry());
        }
    }

    private void removeLocationUpdates() {
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (Exception ignored) {
            }
        }
    }

    private void resolveCountryFromLocation(Location location) {
        new Thread(() -> {
            String countryCode = null;
            try {
                Geocoder geocoder = new Geocoder(SplashActivity.this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    countryCode = addresses.get(0).getCountryCode();
                }
            } catch (Exception ignored) {
            }

            final String resolvedCountry = countryCode != null ? countryCode : resolveFallbackCountry();
            runOnUiThread(() -> finishWithCountry(resolvedCountry));
        }).start();
    }

    private String resolveFallbackCountry() {
        try {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String networkCountry = telephonyManager != null ? telephonyManager.getNetworkCountryIso() : null;
            if (networkCountry != null && networkCountry.length() == 2) {
                return networkCountry.toUpperCase(Locale.US);
            }
        } catch (Exception ignored) {
        }

        String timezoneId = TimeZone.getDefault().getID();
        if (timezoneId != null && (timezoneId.startsWith("Asia/Jakarta")
                || timezoneId.startsWith("Asia/Makassar")
                || timezoneId.startsWith("Asia/Jayapura"))) {
            return "ID";
        }

        String systemCountry = Locale.getDefault().getCountry();
        if (systemCountry != null && systemCountry.length() == 2) {
            return systemCountry.toUpperCase(Locale.US);
        }
        return "US";
    }

    private void finishWithCountry(String countryCode) {
        if (languageResolved) {
            return;
        }
        languageResolved = true;
        removeLocationUpdates();
        handler.removeCallbacksAndMessages(null);

        String languageCode = LocaleHelper.languageFromCountry(countryCode);
        LocaleHelper.setLanguage(this, languageCode);

        Context localizedContext = LocaleHelper.wrap(this, languageCode);
        Resources resources = localizedContext.getResources();

        String countryName = new Locale("", countryCode).getDisplayCountry(new Locale(languageCode));
        String detectStatus = resources.getString(R.string.splash_detecting);
        if (countryName != null && !countryName.trim().isEmpty()) {
            detectStatus = countryName;
        }

        statusText.setText(detectStatus);
        descriptionText.setText(resources.getString(R.string.splash_description));
        languageText.setText(resources.getString(LocaleHelper.LANGUAGE_INDONESIAN.equals(languageCode)
                ? R.string.language_indonesian
                : R.string.language_english));

        animateResolution(countryCode);

        handler.postDelayed(() -> {
            Class<?> target = MyUangRepository.get(SplashActivity.this).isLoggedIn()
                    ? DashboardActivity.class
                    : LoginActivity.class;
            Intent intent = new Intent(SplashActivity.this, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        }, 1500);
    }

    private void animateResolution(String countryCode) {
        if (imageLocation != null) {
            imageLocation.animate().cancel();
            imageLocation.animate()
                    .scaleX(0.0f)
                    .scaleY(0.0f)
                    .alpha(0.0f)
                    .setDuration(400)
                    .start();
        }
        if (progressBar != null) {
            progressBar.animate()
                    .alpha(0.0f)
                    .setDuration(400)
                    .start();
        }

        if (textFlag != null) {
            textFlag.setText(getFlagEmoji(countryCode));
            textFlag.setVisibility(View.VISIBLE);
            textFlag.animate()
                    .alpha(1.0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(600)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.5f))
                    .start();
        }
    }

    private String getFlagEmoji(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            return "🌐";
        }
        int firstLetter = Character.codePointAt(countryCode.toUpperCase(Locale.US), 0) - 0x41 + 0x1F1E6;
        int secondLetter = Character.codePointAt(countryCode.toUpperCase(Locale.US), 1) - 0x41 + 0x1F1E6;
        try {
            return new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter));
        } catch (Exception e) {
            return "🌐";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (hasLocationPermission()) {
                detectLanguageFromLocation();
            } else {
                finishWithCountry(resolveFallbackCountry());
            }
        }
    }

    @Override
    protected void onDestroy() {
        removeLocationUpdates();
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
