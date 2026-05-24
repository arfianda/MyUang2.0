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
import android.widget.TextView;

import java.io.IOException;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        statusText = findViewById(R.id.textSplashStatus);
        descriptionText = findViewById(R.id.textSplashDescription);
        languageText = findViewById(R.id.textSplashLanguage);
        startLocationLanguageDetection();
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
            for (String provider : providers) {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location != null && (bestLocation == null || location.getTime() > bestLocation.getTime())) {
                    bestLocation = location;
                }
            }
        } catch (SecurityException ignored) {
            return null;
        }
        return bestLocation;
    }

    private void requestSingleLocationUpdate() {
        if (locationManager == null) {
            return;
        }

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        String provider = locationManager.getBestProvider(criteria, true);
        if (provider == null) {
            return;
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (locationManager != null) {
                    try {
                        locationManager.removeUpdates(this);
                    } catch (SecurityException ignored) {
                        // The location result is already available, so continue with detection.
                    }
                }
                resolveCountryFromLocation(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        try {
            locationManager.requestSingleUpdate(provider, locationListener, Looper.getMainLooper());
        } catch (SecurityException ignored) {
            finishWithCountry(resolveFallbackCountry());
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
            } catch (IOException ignored) {
                countryCode = null;
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
            // Fallback continues below.
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

        String languageCode = LocaleHelper.languageFromCountry(countryCode);
        LocaleHelper.setLanguage(this, languageCode);

        Context localizedContext = LocaleHelper.wrap(this, languageCode);
        Resources resources = localizedContext.getResources();
        statusText.setText(resources.getString(R.string.splash_detecting));
        descriptionText.setText(resources.getString(R.string.splash_description));
        languageText.setText(resources.getString(LocaleHelper.LANGUAGE_INDONESIAN.equals(languageCode)
                ? R.string.language_indonesian
                : R.string.language_english));

        handler.postDelayed(() -> {
            Class<?> target = MyUangRepository.get(SplashActivity.this).isLoggedIn()
                    ? DashboardActivity.class
                    : LoginActivity.class;
            Intent intent = new Intent(SplashActivity.this, target);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        }, 700);
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
}
