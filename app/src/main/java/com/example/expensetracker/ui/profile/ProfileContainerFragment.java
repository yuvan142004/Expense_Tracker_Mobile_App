package com.example.expensetracker.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.expensetracker.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * The nav-graph destination for "Profile".
 * Hosts a TabLayout + ViewPager2 with two tabs:
 *   0 → ProfileFragment  (avatar, name)
 *   1 → SettingsFragment (text / theme / palette)
 */
public class ProfileContainerFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_container, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewPager2  pager  = view.findViewById(R.id.viewPagerProfile);
        TabLayout   tabs   = view.findViewById(R.id.tabLayoutProfile);

        pager.setAdapter(new ProfilePagerAdapter(requireActivity()));
        pager.setOffscreenPageLimit(1);

        new TabLayoutMediator(tabs, pager, (tab, position) -> {
            tab.setText(position == 0 ? "Profile" : "Settings");
        }).attach();
    }

    // ── Pager adapter ──────────────────────────────────────────────────────────

    private static class ProfilePagerAdapter extends FragmentStateAdapter {
        ProfilePagerAdapter(FragmentActivity fa) { super(fa); }

        @Override
        public int getItemCount() { return 2; }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return position == 0 ? new ProfileFragment() : new SettingsFragment();
        }
    }
}
