package com.alibaba.weex.extend.component;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.dom.WXDomObject;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXDiv;
import com.taobao.weex.ui.component.WXScroller;
import com.taobao.weex.ui.component.WXVContainer;
import com.taobao.weex.ui.component.list.WXListComponent;
import com.taobao.weex.ui.view.WXFrameLayout;
import com.taobao.weex.ui.view.WXScrollView;
import com.taobao.weex.ui.view.listview.WXRecyclerView;
import com.taobao.weex.ui.view.refresh.wrapper.BounceRecyclerView;
import com.taobao.weex.utils.WXLogUtils;
import com.taobao.weex.utils.WXViewUtils;

/**
 * Description:
 * <p>
 * Created by rowandjj(chuyi)<br/>
 * 属性: target. 需要attach的cell的ref
 * 根据ref
 */

public class WXGlueView extends WXDiv {

    private int mInitTranslationY;

    private RecyclerView.OnScrollListener mOnScrollListener;
    private WXScrollView.WXScrollViewListener mWxScrollViewListener;

    private WXFrameLayout mPlaceHolderView;
    private WXFrameLayout mRealContainerView;

    private boolean once = false;

    private WXComponent mScrollableParent;

    public WXGlueView(WXSDKInstance instance, WXDomObject node, WXVContainer parent) {
        super(instance, node, parent);
    }

    @Override
    protected WXFrameLayout initComponentHostView(@NonNull Context context) {
        mPlaceHolderView = new WXFrameLayout(context);
        mPlaceHolderView.holdComponent(this);

        mRealContainerView = new WXFrameLayout(context);
        return mPlaceHolderView;
    }

    @Override
    public ViewGroup getRealView() {
        return mRealContainerView;
    }

    @Override
    protected void addSubView(View child, int index) {
        super.addSubView(child, index);

        if (!once) {


            mPlaceHolderView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Rect rect = WXGlueView.this.getComponentSize();

                    WXLogUtils.d("CHUYI",rect.toString());

                    if(!rect.isEmpty()) {
//                        View view = getInstance().getRootView();
//
//                        ViewGroup root = (ViewGroup) view;
//                        ViewGroup root = (ViewGroup) view.getParent();

                        ViewGroup root = (ViewGroup) mScrollableParent.getHostView();

//                        mScrollableParent.getDomObject().getStyles().get("borderWidth");

                        //TODO border width

                        Rect rootRect = mScrollableParent.getComponentSize();

//                        ViewGroup root = mScrollableParent.getHostView();

                        root.addView(mRealContainerView);
//                        mRealContainerView.setLayoutParams(new FrameLayout.LayoutParams(rect.left,rect.top));

                        mInitTranslationY = (int) (rect.top-rootRect.top- WXViewUtils.getRealPxByWidth(3));
                        mRealContainerView.setTranslationY(rect.top-rootRect.top- WXViewUtils.getRealPxByWidth(3));
                        mRealContainerView.setTranslationX(rect.left-rootRect.left- WXViewUtils.getRealPxByWidth(3));
                    }
                }
            },10);
            once = true;
        }
    }

    @Override
    protected void onHostViewInitialized(WXFrameLayout host) {
        super.onHostViewInitialized(host);

        //2. 注册scroll监听器
        mScrollableParent = findScrollableParent();
        if (mScrollableParent != null) {
            registerScrollListener(mScrollableParent);
        }


    }

    private void registerScrollListener(WXComponent sourceComponent) {
        if (sourceComponent instanceof WXScroller) {
            WXScroller scroller = (WXScroller) sourceComponent;
            View innerView = scroller.getInnerView();
            if (innerView != null && innerView instanceof WXScrollView) {
                mWxScrollViewListener = new InnerScrollListener();
                ((WXScrollView) innerView).addScrollViewListener(mWxScrollViewListener);
            }

        } else if (sourceComponent instanceof WXListComponent) {
            WXListComponent list = (WXListComponent) sourceComponent;
            BounceRecyclerView hostView = list.getHostView();
            if (hostView != null) {
                WXRecyclerView recyclerView = hostView.getInnerView();
                if (recyclerView != null) {
                    mOnScrollListener = new InnerScrollListener();
                    recyclerView.addOnScrollListener(mOnScrollListener);
                }
            }
        }
    }

    @Nullable
    private WXComponent findScrollableParent() {
        WXComponent target = this;
        while (target != null) {
            if (!(target instanceof WXScroller) && !(target instanceof WXListComponent)) {
                target = target.getParent();
            } else {
                break;
            }
        }
        return target;
    }

    private class InnerScrollListener extends RecyclerView.OnScrollListener implements WXScrollView.WXScrollViewListener {

        /**
         * scroller 监听. 其中x和y是相对起始位置的偏移量
         */
        @Override
        public void onScroll(WXScrollView wxScrollView, int x, int y) {
            getRealView().setTranslationY(mInitTranslationY - y);
        }

        /**
         * list 监听. 其中dx和dy是相对上一次的偏移量
         */
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {

            int x = recyclerView.computeHorizontalScrollOffset();
            int y = recyclerView.computeVerticalScrollOffset();

            getRealView().setTranslationY(mInitTranslationY - y);
        }

        @Override
        public void onScrollChanged(WXScrollView wxScrollView, int i, int i1, int i2, int i3) {

        }

        @Override
        public void onScrollToBottom(WXScrollView wxScrollView, int i, int i1) {

        }

        @Override
        public void onScrollStopped(WXScrollView wxScrollView, int i, int i1) {

        }
    }

}
