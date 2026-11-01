package com.udesc.scriptumapp.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.udesc.scriptumapp.R;

public class MainActivity extends AppCompatActivity {
    private ScriptumViewModel viewModel;
    private LinearLayout bottomNavigation;
    private Button categoriesButton;
    private Button profileButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        categoriesButton = findViewById(R.id.navigation_categories);
        profileButton = findViewById(R.id.navigation_profile);
        viewModel = new ViewModelProvider(this).get(ScriptumViewModel.class);
        categoriesButton.setOnClickListener(v -> showPage(ScreenFragment.CATEGORIES));
        profileButton.setOnClickListener(v -> showPage(ScreenFragment.PROFILE));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });
        if (savedInstanceState == null) {
            if (viewModel.hasSession()) {
                showPage(ScreenFragment.CATEGORIES);
            } else {
                showPage(ScreenFragment.LOGIN);
            }
        } else {
            Fragment current = getSupportFragmentManager().findFragmentById(R.id.content);
            String page = current == null || current.getArguments() == null
                    ? ScreenFragment.LOGIN
                    : current.getArguments().getString("page", ScreenFragment.LOGIN);
            bottomNavigation.setVisibility(viewModel.hasSession() && !ScreenFragment.LOGIN.equals(page) ? View.VISIBLE : View.GONE);
            updateBottomNavigation(page);
        }
    }

    public ScriptumViewModel getViewModel() {
        return viewModel;
    }

    public void showPage(String page) {
        showPage(page, null);
    }

    public void showPage(String page, Bundle arguments) {
        boolean authenticated = viewModel != null && viewModel.hasSession();
        bottomNavigation.setVisibility(authenticated && !ScreenFragment.LOGIN.equals(page) ? View.VISIBLE : View.GONE);
        updateBottomNavigation(page);
        Fragment fragment = ScreenFragment.newInstance(page, arguments);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, fragment)
                .commit();
    }

    public void updateBottomNavigation(String page) {
        updateNavigationItem(
                categoriesButton,
                R.drawable.category,
                ScreenFragment.CATEGORIES.equals(page) ? R.color.scriptum_green : R.color.scriptum_muted);
        updateNavigationItem(
                profileButton,
                R.drawable.user,
                ScreenFragment.PROFILE.equals(page) ? R.color.scriptum_green : R.color.scriptum_muted);
    }

    private void updateNavigationItem(Button button, int iconResource, int tintResource) {
        int tint = getColor(tintResource);
        button.setTextColor(tint);
        android.graphics.drawable.Drawable icon = DrawableCompat.wrap(getDrawable(iconResource)).mutate();
        DrawableCompat.setTint(icon, tint);
        int iconSize = Math.round(20 * getResources().getDisplayMetrics().density);
        icon.setBounds(0, 0, iconSize, iconSize);
        button.setCompoundDrawablesRelative(icon, null, null, null);
    }
}
