package com.location.sms.smslocator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;

public class StartPage extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.start_page);

        final ImageView image = (ImageView) this.findViewById(R.id.image);
        final Animation am1 = new AlphaAnimation(0, 1);
        final Animation am2 = new AlphaAnimation(1, 0);
        am1.setDuration(2000);
        am2.setDuration(2000);

        image.setImageResource(R.drawable.start_page);
        image.setVisibility(View.INVISIBLE);
        image.setAnimation(am1);
        image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(StartPage.this, MainActivity.class);
                startActivity(intent);
                StartPage.this.finish();
            }
        });

        am1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation arg0) {
            }

            @Override
            public void onAnimationRepeat(Animation arg0) {
            }

            @Override
            public void onAnimationEnd(Animation arg0) {
                image.setAnimation(am2);
                am2.startNow();
            }
        });

        am2.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation arg0) {
            }

            @Override
            public void onAnimationRepeat(Animation arg0) {
            }

            @Override
            public void onAnimationEnd(Animation arg0) {
                Intent intent = new Intent();
                intent.setClass(StartPage.this, MainActivity.class);
                startActivity(intent);
                StartPage.this.finish();
            }
        });

        am1.startNow();
    }
}
