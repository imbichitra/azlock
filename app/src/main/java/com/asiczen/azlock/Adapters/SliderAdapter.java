package com.asiczen.azlock.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.asiczen.azlock.R;


public class SliderAdapter extends PagerAdapter {

    Context context;
    //LinearLayout linearLayout;
    LayoutInflater layoutInflater;

    public SliderAdapter(Context context){
        this.context = context;
    }

    private final int[] slide_image={
            R.drawable.lock_img,
            R.drawable.ic_bluetooth,
            R.drawable.ic_wifi1,
            R.drawable.ic_key1
    };

    private final String[] slide_headings={
            "Get Smart... Go Digital...",
            "Access Locally",
            "Access Globally",
            "Key Sharing"
    };

    private final String[] slide_desc={
            "Forget the key from now on.",
            "Operate your lock using Bluetooth.",
            "Operate your door lock from anywhere across the global using WI-FI.",
            "Allow or restrict access to guest at will,from anywhere and manage log of operations."

    };
    @Override
    public int getCount() {
        return slide_headings.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
        return view == o;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = null;
        if (layoutInflater != null) {
            view = layoutInflater.inflate(R.layout.slide_layout,container,false);
        }
        //RelativeLayout relativeLayout = view.findViewById(R.id.main_layout);
        /*if(position==0){
            //relativeLayout.setBackgroundColor(getColor(R.color.statusbar_1));
            relativeLayout.setBackgroundColor(context.getResources().getColor(R.color.pink));
        }*/
        ImageView sliderImageView = view.findViewById(R.id.slider_image);
        TextView sliderHeading = view.findViewById(R.id.slider_heading);
        TextView sliderDesc = view.findViewById(R.id.slider_desc);

        sliderImageView.setImageResource(slide_image[position]);
        sliderHeading.setText(slide_headings[position]);
        sliderDesc.setText(slide_desc[position]);

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((RelativeLayout)object);
    }
}
