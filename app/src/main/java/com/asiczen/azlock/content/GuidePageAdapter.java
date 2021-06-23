package com.asiczen.azlock.content;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by user on 11/12/2015.
 */
class GuidePageAdapter extends PagerAdapter {

    private final int[] resources;
    private final LayoutInflater inflater;

    public GuidePageAdapter(Context mContext, int[] resources){
        this.resources = resources;
        inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return resources.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View view = inflater.inflate(resources[position], null);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup arg0, int arg1, @NonNull Object arg2) {
        arg0.removeView((View) arg2);

    }
}
