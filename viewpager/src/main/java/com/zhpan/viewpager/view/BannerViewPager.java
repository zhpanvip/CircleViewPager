package com.zhpan.viewpager.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.DimenRes;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.example.viewpager.R;
import com.zhpan.viewpager.adapter.BannerPagerAdapter;
import com.zhpan.viewpager.holder.HolderCreator;
import com.zhpan.viewpager.holder.ViewHolder;
import com.zhpan.viewpager.provider.BannerScroller;
import com.zhpan.viewpager.provider.ViewStyleSetter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhpan on 2017/3/28.
 */
public class BannerViewPager<T, VH extends ViewHolder> extends FrameLayout {
    public String tag = "BannerViewPager";
    private ViewPager mViewPager;
    //  轮播数据集合
    private List<T> mList;
    //  指示器图片集合
    private List<DotView> mDotList;
    //  图片切换时间间隔
    private int interval;
    //  圆点位置
    private int dotPosition = 0;
    //  图片上一个位置
    private int prePosition = 0;
    //  图片当前位置
    private int currentPosition;
    //  是否正在循环
    private boolean isLooping;
    //  是否开启循环
    private boolean isCanLoop;
    //  是否开启自动播放
    private boolean isAutoPlay = false;
    //  是否显示指示器圆点
    private boolean showIndicator = true;
    // 圆点指示器显示位置
    public static final int START = 1;
    public static final int END = 2;
    public static final int CENTER = 0;
    private int gravity;
    //  未选中时圆点颜色
    private int indicatorNormalColor;
    //  选中时选点颜色
    private int indicatorCheckedColor;
    //  指示器圆点半径
    private float indicatorRadius;
    //  页面点击事件监听
    private OnPageClickListener mOnPageClickListener;
    //  圆点指示器的Layout
    private LinearLayout mLlIndicator;
    private HolderCreator holderCreator;

    Handler mHandler = new Handler();
    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if (mList.size() > 1) {
                currentPosition = currentPosition % (mList.size() + 1) + 1;
                if (currentPosition == 1) {
                    mViewPager.setCurrentItem(currentPosition, false);
                    mHandler.post(mRunnable);
                } else {
                    mViewPager.setCurrentItem(currentPosition, true);
                    mHandler.postDelayed(mRunnable, interval);
                }
            }
        }
    };
    private BannerScroller mScroller;
    private int mScrollTime = 1000;

    public BannerViewPager(Context context) {
        this(context, null);
        init(null, context);
    }

    public BannerViewPager(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init(attrs, context);
    }

    public BannerViewPager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, context);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            initData();
            setIndicator();
            setViewPager();
        }
    }

    private void init(AttributeSet attrs, Context context) {
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.BannerViewPager);
            interval = typedArray.getInteger(R.styleable.BannerViewPager_interval, 3000);
            indicatorCheckedColor = typedArray.getColor(R.styleable.BannerViewPager_indicator_checked_color, Color.parseColor("#FF4C39"));
            indicatorNormalColor = typedArray.getColor(R.styleable.BannerViewPager_indicator_normal_color, Color.parseColor("#935656"));
            indicatorRadius = typedArray.getDimension(R.styleable.BannerViewPager_indicator_radius, dp2px(context, 4));
            isAutoPlay = typedArray.getBoolean(R.styleable.BannerViewPager_isAutoPlay, true);
            isCanLoop = typedArray.getBoolean(R.styleable.BannerViewPager_isCanLoop, true);
            gravity = typedArray.getInt(R.styleable.BannerViewPager_indicator_gravity, 0);
            typedArray.recycle();
        }
        View view = LayoutInflater.from(getContext()).inflate(R.layout.view_pager_layout, this);
        mLlIndicator = view.findViewById(R.id.ll_main_dot);
        mViewPager = view.findViewById(R.id.vp_main);
        mList = new ArrayList<>();
        mDotList = new ArrayList<>();
        initScroller();
    }

    private void initScroller() {
        try {
            Field mField = ViewPager.class.getDeclaredField("mScroller");
            mField.setAccessible(true);
            mScroller = new BannerScroller(mViewPager.getContext());
            mScroller.setDuration(mScrollTime);
            mField.set(mViewPager, mScroller);
        } catch (Exception e) {
            Log.e(tag, e.getMessage());
        }
    }

    private void setIndicatorGravity() {
        switch (gravity) {
            case CENTER:
                mLlIndicator.setGravity(Gravity.CENTER);
                break;
            case START:
                mLlIndicator.setGravity(Gravity.START);
                break;
            case END:
                mLlIndicator.setGravity(Gravity.END);
                break;
        }
    }

    //  根据mList数据集构造mListAdd
    private void initData() {
        if (mList.size() == 0) {
            setVisibility(GONE);
        } else {
            if (isCanLoop) {
                currentPosition = 1;
            }
        }
    }

    //  设置触摸事件，当滑动或者触摸时停止自动轮播
    private void setTouchListener() {
        mViewPager.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        isLooping = true;
                        stopLoop();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isLooping = false;
                        startLoop();
                    default:
                        break;
                }
                return false;
            }
        });
    }


    private void startLoop() {
        if (!isLooping && isAutoPlay && mViewPager != null) {
            mHandler.postDelayed(mRunnable, interval);// 每interval秒执行一次runnable.
            isLooping = true;
        }
    }

    public void stopLoop() {
        if (isLooping && mViewPager != null) {
            mHandler.removeCallbacks(mRunnable);
            isLooping = false;
        }
    }

    public void setRoundCorner(@DimenRes int radius) {
        setRoundCorner(getResources().getDimension(radius));
    }

    public void setRoundCorner(float radius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewStyleSetter viewStyleSetter = new ViewStyleSetter(this);
            viewStyleSetter.setRoundCorner(radius);
        }
    }


    //  设置轮播小圆点
    private void setIndicator() {
        // mDotList.clear();
        mLlIndicator.removeAllViews();
        //  设置LinearLayout的子控件的宽高，这里单位是像素。
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) indicatorRadius * 2, (int) indicatorRadius * 2);
        params.rightMargin = (int) (indicatorRadius * 2 / 1.5);
        if (mList.size() > 1) {
            //  for循环创建mUrlList.size()个ImageView（小圆点）
            for (int i = 0; i < mList.size(); i++) {
                DotView dotView = new DotView(getContext());
                dotView.setLayoutParams(params);
                dotView.setNormalColor(indicatorNormalColor);
                dotView.setCheckedColor(indicatorCheckedColor);
                dotView.setChecked(false);
                mLlIndicator.addView(dotView);
                mDotList.add(dotView);
            }
        }
        //设置第一个小圆点图片背景为红色
        if (mList.size() > 1) {
            mDotList.get(dotPosition).setChecked(true);
        }
        setIndicatorGravity();
    }

    private void setViewPager() {
        BannerPagerAdapter<T, VH> adapter = new BannerPagerAdapter<>(mList, this, holderCreator);
        adapter.setCanLoop(isCanLoop);
        mViewPager.setAdapter(adapter);
        mViewPager.setCurrentItem(currentPosition);

        setPageChangeListener();
        startLoop();
        setTouchListener();
        if (showIndicator) {
            mLlIndicator.setVisibility(VISIBLE);
        } else {
            mLlIndicator.setVisibility(GONE);
        }
    }

    public ViewPager getViewPager() {
        return mViewPager;
    }

    //  是否显示轮播指示器
    public void isShowIndicator(boolean showIndicator) {
        this.showIndicator = showIndicator;
    }

    public void setIndicatorGravity(int gravity) {
        this.gravity = gravity;
    }

    public void setPages(List<T> list, HolderCreator<VH> holderCreator) {
        if (list == null || holderCreator == null) {
            return;
        }
        mList.addAll(list);
        this.holderCreator = holderCreator;
    }

    //  ViewPager页面改变监听
    private void setPageChangeListener() {
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                pageSelected(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //  当state为SCROLL_STATE_IDLE即没有滑动的状态时切换页面
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    mViewPager.setCurrentItem(currentPosition, false);
                }
            }
        });
    }

    private void pageSelected(int position) {
        if (isCanLoop) {
            if (position == 0) {    //判断当切换到第0个页面时把currentPosition设置为list.size(),即倒数第二个位置，小圆点位置为length-1
                currentPosition = mList.size();
                dotPosition = mList.size() - 1;
            } else if (position == mList.size() + 1) {    //当切换到最后一个页面时currentPosition设置为第一个位置，小圆点位置为0
                currentPosition = 1;
                dotPosition = 0;
            } else {
                currentPosition = position;
                dotPosition = position - 1;
            }
        } else {
            currentPosition = position;
            dotPosition = currentPosition;
        }
        //  把之前的小圆点设置未选中状态，当前小圆点设置为选中状态
        mDotList.get(prePosition).setChecked(false);
        mDotList.get(dotPosition).setChecked(true);
        prePosition = dotPosition;
    }

    public interface OnPageClickListener {
        void onPageClick(int position);
    }

    //  adapter中图片点击的回掉方法
    public void imageClick(int position) {
        if (mOnPageClickListener != null) {
            mOnPageClickListener.onPageClick(isCanLoop ? position - 1 : position);
        }
    }

    /**
     * @param checkedColor 选中时指示器颜色
     * @param normalColor  未选中时指示器颜色
     */
    public void setIndicatorColor(@ColorInt int normalColor, @ColorInt int checkedColor) {
        indicatorCheckedColor = checkedColor;
        indicatorNormalColor = normalColor;
    }

    public void setOnPageClickListener(OnPageClickListener onPageClickListener) {
        this.mOnPageClickListener = onPageClickListener;
    }

    public boolean isAutoPlay() {
        return isAutoPlay;
    }

    public void setAutoPlay(boolean autoPlay) {
        isAutoPlay = autoPlay;
    }

    public boolean isCanLoop() {
        return isCanLoop;
    }

    public void setCanLoop(boolean canLoop) {
        isCanLoop = canLoop;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public void setCurrentItem(final int position) {
        mViewPager.post(new Runnable() {
            @Override
            public void run() {
                mViewPager.setCurrentItem(getRealPosition(position));
            }
        });
    }

    public void setCurrentItem(final int position, final boolean smoothScroll) {
        mViewPager.post(new Runnable() {
            @Override
            public void run() {
                mViewPager.setCurrentItem(getRealPosition(position), smoothScroll);
            }
        });
    }

    private int getRealPosition(int position) {
        return isCanLoop ? (position < mList.size()) ? (++position) : mList.size() : position;
    }

    public List<T> getList() {
        return mList;
    }

    public void setList(List<T> list) {
        mList = list;
    }


    public float getIndicatorRadius() {
        return indicatorRadius;
    }

    public void setIndicatorRadius(float indicatorRadius) {
        this.indicatorRadius = dp2px(getContext(), indicatorRadius);
    }

    public void setScrollTime(int scrollTime) {
        mScrollTime = scrollTime;
    }

    public static int dp2px(Context context, float dpValue) {
        DisplayMetrics metric = context.getResources().getDisplayMetrics();
        float screenDensity = metric.density;
        return (int) (dpValue * screenDensity + 0.5f);
    }
}