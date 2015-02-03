package in.tosc.studddin.fragments;


import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import in.tosc.studddin.R;
import in.tosc.studddin.fragments.events.EventsCreateFragment;
import in.tosc.studddin.fragments.events.EventsListFragment;
import in.tosc.studddin.fragments.notes.NotesSearchFragment;

/**
 * A simple {@link Fragment} subclass.
 */
public class EventsFragment extends Fragment {

    ViewPager eventsPager;
    FragmentPagerAdapter fragmentPagerAdapter;


    public EventsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_events, container, false);
        setCustomTheme();
        fragmentPagerAdapter = new FragmentPagerAdapter(getChildFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return (new EventsListFragment());
                    case 1:
                        return (new EventsCreateFragment());

                }
                return new NotesSearchFragment();
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return "Events";
                    case 1:
                        return "Create event";
                }

                return null;
            }

            @Override
            public int getCount() {
                return 2;
            }
        };

        eventsPager = (ViewPager) view.findViewById(R.id.events_pager);
        eventsPager.setAdapter(fragmentPagerAdapter);
        eventsPager.setOffscreenPageLimit(2);
        return view;
    }

    public void goToOtherFragment(int position) {
        eventsPager.setCurrentItem(position);
    }

    public void setCustomTheme(){
        int primary = getActivity().getResources().getColor(R.color.eventsColorPrimary);
        int secondary = getActivity().getResources().getColor(R.color.eventsColorPrimaryDark);
        ColorDrawable colorDrawable = new ColorDrawable(primary);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setBackgroundDrawable(colorDrawable);
        if(Build.VERSION.SDK_INT==Build.VERSION_CODES.LOLLIPOP){
            getActivity().getWindow().setNavigationBarColor(secondary);
            getActivity().getWindow().setStatusBarColor(secondary);
        }
    }

}
