package com.example.test;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

public class CarouselAdapter extends PagerAdapter {
    private Context context;
    private int[] imageIds;

    public CarouselAdapter(Context context, int[] imageIds) {
        this.context = context;
        this.imageIds = imageIds;
    }

    @Override
    public int getCount() {
        return Integer.MAX_VALUE; // 实现无限循环
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        // 根据位置获取真实的图片索引
        int actualPosition = position % imageIds.length;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.carousel_page, container, false);
        ImageView imageView = view.findViewById(R.id.carousel_image);
        imageView.setImageResource(imageIds[actualPosition]);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}
