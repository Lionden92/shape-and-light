package com.pathfinder.shapeandlight;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Lionden on 2/1/2017.
 */

public class AboutFragment extends Fragment {

    public AboutFragment() {
        // Required empty public constructor.
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.about_dialog, container, false);
        final TextView privacyPolicy = rootView.findViewById(R.id.privacy_policy_link);
        privacyPolicy.setClickable(true);
        privacyPolicy.setMovementMethod(LinkMovementMethod.getInstance());
        String privacyPolicyString =
                "<a href='https://sites.google.com/view/sl-privacypolicy'>Privacy Policy</a>";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            privacyPolicy.setText(Html.fromHtml(privacyPolicyString, Html.FROM_HTML_MODE_COMPACT));
        } else {
            privacyPolicy.setText(Html.fromHtml(privacyPolicyString));
        }
        return rootView;
    }

}
