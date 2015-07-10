package reach.project.onBoarding;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.viewpagerindicator.CirclePageIndicator;

import java.io.IOException;

import reach.backend.entities.userApi.model.OldUserContainer;
import reach.project.R;
import reach.project.core.StaticData;
import reach.project.utils.DoWork;
import reach.project.utils.MiscUtils;
import reach.project.utils.SharedPrefUtils;
import reach.project.utils.SuperInterface;

public class NumberVerification extends Fragment {

    private SuperInterface mListener;

    public static NumberVerification newInstance () {
        return new NumberVerification();
    }

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container,
                              Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View rootView = inflater.inflate(R.layout.fragment_number_verification, container, false);

        rootView.postDelayed(new Runnable() {
            @Override
            public void run() {

                rootView.setBackgroundResource(0);
                rootView.findViewById(R.id.reach_logo).setVisibility(View.GONE);
                rootView.findViewById(R.id.otherStuff).setVisibility(View.VISIBLE);
                rootView.findViewById(R.id.numberVerificationPoster).setVisibility(View.VISIBLE);

                final View telephone = rootView.findViewById(R.id.telephoneNumber);
                final ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.logo);

                telephone.requestFocus();
                viewPager.setAdapter(new TourPagerAdapter(getActivity()));
                ((CirclePageIndicator) rootView.findViewById(R.id.circles)).setViewPager(viewPager);

                rootView.findViewById(R.id.verify).setOnClickListener(new ClickListener(
                        rootView.findViewById(R.id.bottomPart1),
                        rootView.findViewById(R.id.bottomPart2),
                        telephone));
            }
        }, 2000);
        return rootView;
    }

    private final class TourPagerAdapter extends PagerAdapter {

        private final String[] tourTexts = new String[]{"Browse and Access files\nof your Network",
                                                               "Easy file transfer logs\nthrough Reach Queue",
                                                               "Privacy and security\noptions",
                                                               "Synced media files\n"};
        private final int[] tourImages = new int[]{R.drawable.library_view,
                                                          R.drawable.reach_queue,
                                                          R.drawable.my_reach,
                                                          R.drawable.hide};

        private final Context mContext;
        private final LayoutInflater mLayoutInflater;

        public TourPagerAdapter (Context context) {
            mContext = context;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount () {
            return tourTexts.length;
        }

        @Override
        public boolean isViewFromObject (View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem (ViewGroup container, int position) {

            final View itemView = mLayoutInflater.inflate(R.layout.tour_item, container, false);
            ((TextView) itemView.findViewById(R.id.tour_text)).setText(tourTexts[position]);
            ((ImageView) itemView.findViewById(R.id.tour_image)).setImageResource(tourImages[position]);
            //Picasso.with(container.getContext()).load(tourImages[position]).noFade().into((ImageView) itemView.findViewById(R.id.tour_image));
            container.addView(itemView);
            return itemView;
        }

        @Override
        public void destroyItem (ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }
    }

    private final class ClickListener implements View.OnClickListener {

        private final View bottomPart1, bottomPart2;
        private final EditText telephoneNumber;

        private ClickListener (View ... views) {
            this.bottomPart1 = views[0];
            this.bottomPart2 = views[1];
            this.telephoneNumber = (EditText) views[2];
        }

        @Override
        public void onClick (View view) {

            final String phoneNumber = telephoneNumber.getText().toString();
            final String parsed;
            if (TextUtils.isEmpty(phoneNumber) || (parsed = phoneNumber.replaceAll("[^0-9]", "")).length() < 10) {
                Toast.makeText(view.getContext(), "Enter Valid Number", Toast.LENGTH_SHORT).show();
                return;
            }

            final int length = parsed.length();
            //take last 10 digits
            new GetOldAccount().executeOnExecutor(StaticData.threadPool, parsed.substring(length - 10, length));
            bottomPart1.setVisibility(View.INVISIBLE);
            bottomPart2.setVisibility(View.VISIBLE);
        }

        final class GetOldAccount extends AsyncTask<String, Void, Pair<OldUserContainer, String>> {

            @Override
            protected final Pair<OldUserContainer, String> doInBackground (final String... params) {

                return new Pair<>(MiscUtils.autoRetry(new DoWork<OldUserContainer>() {
                    @Override
                    protected OldUserContainer doWork () throws IOException {
                        return StaticData.userEndpoint.isAccountPresent(params[0]).execute();
                    }
                }, Optional.<Predicate<OldUserContainer>> absent()).orNull(), params[0]);
            }

            @Override
            protected void onPostExecute (final Pair<OldUserContainer, String> pair) {

                super.onPostExecute(pair);

                final FragmentActivity activity = getActivity();
                if (isRemoving() || isDetached() || isCancelled() || activity == null || activity.isFinishing())
                    return;

                final SharedPreferences sharedPreferences = activity.getSharedPreferences("Reach", Context.MODE_MULTI_PROCESS);
                sharedPreferences.edit().clear().apply();
                SharedPrefUtils.storePhoneNumber(sharedPreferences, pair.second);
                mListener.startAccountCreation(Optional.of(pair.first));
            }
        }
    }

    @Override
    public void onAttach (Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (SuperInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                                                 + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach () {
        super.onDetach();
        mListener = null;
    }
}