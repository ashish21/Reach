package reach.project.core;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import reach.project.R;
import reach.project.utils.MiscUtils;
import reach.project.utils.viewHelpers.CircleTransform;

public class DialogActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push);

        final TextView userName = (TextView) findViewById(R.id.userName);
        final TextView textView1 = (TextView) findViewById(R.id.textView1);
        final TextView exit = (TextView) findViewById(R.id.exit);
        final LinearLayout accept = (LinearLayout) findViewById(R.id.accept);
        final LinearLayout reject = (LinearLayout) findViewById(R.id.reject);
        final ImageView userImageView = (ImageView) findViewById(R.id.userImage);
        final int type = getIntent().getIntExtra("type",0);

        final int px = MiscUtils.dpToPx(50);
        Picasso.with(DialogActivity.this)
                .load("https://scontent-sin1-1.xx.fbcdn.net/hphotos-xap1/v/t1.0-9/1011255_638449632916744_321328860_n.jpg?oh=5c1daa8d7d015f7ce698ee1793d5a929&oe=55EECF36&dl=1")
                .centerCrop()
                .resize(px, px)
                .transform(new CircleTransform())
                .into(userImageView);
        reject.setVisibility(View.GONE);
        accept.setVisibility(View.GONE);
        exit.setVisibility(View.VISIBLE);
        exit.setOnClickListener(v -> finish());
        if (type == 1) {
            userName.setText("Hey!");
            textView1.setText("I am Devika from Team Reach! \n" +
                    "Send me an access request by clicking on the lock icon beside my name to view my Music collection. \n" +
                    "Keep Reaching ;)");
        }
        else if (type == 2) {
            userName.setText("Click and Grab!");
            textView1.setText("You can add multiple songs instantly to your Reach Queue by just clicking on the songs here.");
        }
        else if (type == 3) {
            userName.setText(getIntent().getStringExtra("manual_title"));
            textView1.setText(getIntent().getStringExtra("manual_text"));
        }
    }
}