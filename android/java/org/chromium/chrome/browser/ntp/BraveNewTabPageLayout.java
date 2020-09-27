/* Copyright (c) 2019 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.ntp;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.viewpager.widget.ViewPager;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.chromium.base.ContextUtils;
import org.chromium.base.Log;
import org.chromium.base.ThreadUtils;
import org.chromium.base.TraceEvent;
import org.chromium.base.supplier.Supplier;
import org.chromium.base.task.AsyncTask;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.BraveRewardsHelper;
import org.chromium.chrome.browser.ChromeTabbedActivity;
import org.chromium.chrome.browser.app.BraveActivity;
import org.chromium.chrome.browser.brave_stats.BraveStatsUtil;
import org.chromium.chrome.browser.compositor.layouts.OverviewModeBehavior;
import org.chromium.chrome.browser.custom_layout.VerticalViewPager;
import org.chromium.chrome.browser.explore_sites.ExploreSitesBridge;
import org.chromium.chrome.browser.lifecycle.ActivityLifecycleDispatcher;
import org.chromium.chrome.browser.local_database.DatabaseHelper;
import org.chromium.chrome.browser.local_database.TopSiteTable;
import org.chromium.chrome.browser.native_page.ContextMenuManager;
import org.chromium.chrome.browser.night_mode.GlobalNightModeStateProviderHolder;
import org.chromium.chrome.browser.ntp.NTPWidgetAdapter;
import org.chromium.chrome.browser.ntp.NTPWidgetBottomSheetDialogFragment;
import org.chromium.chrome.browser.ntp.NewTabPageLayout;
import org.chromium.chrome.browser.ntp_background_images.NTPBackgroundImagesBridge;
import org.chromium.chrome.browser.ntp_background_images.SuperReferralShareDialogFragment;
import org.chromium.chrome.browser.ntp_background_images.model.BackgroundImage;
import org.chromium.chrome.browser.ntp_background_images.model.NTPImage;
import org.chromium.chrome.browser.ntp_background_images.model.SponsoredTab;
import org.chromium.chrome.browser.ntp_background_images.model.TopSite;
import org.chromium.chrome.browser.ntp_background_images.model.Wallpaper;
import org.chromium.chrome.browser.ntp_background_images.util.FetchWallpaperWorkerTask;
import org.chromium.chrome.browser.ntp_background_images.util.NTPUtil;
import org.chromium.chrome.browser.ntp_background_images.util.NewTabPageListener;
import org.chromium.chrome.browser.ntp_background_images.util.SponsoredImageUtil;
import org.chromium.chrome.browser.offlinepages.DownloadUiActionFlags;
import org.chromium.chrome.browser.offlinepages.OfflinePageBridge;
import org.chromium.chrome.browser.offlinepages.RequestCoordinatorBridge;
import org.chromium.chrome.browser.onboarding.OnboardingPrefManager;
import org.chromium.chrome.browser.preferences.BravePref;
import org.chromium.chrome.browser.preferences.BravePrefServiceBridge;
import org.chromium.components.user_prefs.UserPrefs;
import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.suggestions.tile.SiteSection;
import org.chromium.chrome.browser.suggestions.tile.TileGroup;
import org.chromium.chrome.browser.tab.EmptyTabObserver;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tab.TabAttributes;
import org.chromium.chrome.browser.tab.TabImpl;
import org.chromium.chrome.browser.tab.TabObserver;
import org.chromium.chrome.browser.tabmodel.TabModel;
import org.chromium.chrome.browser.util.PackageUtils;
import org.chromium.components.browser_ui.widget.displaystyle.UiConfig;

import java.util.List;

public class BraveNewTabPageLayout extends NewTabPageLayout {
    private static final String TAG = "BraveNewTabPageView";

    private ImageView bgImageView;
    private Profile mProfile;

    private SponsoredTab sponsoredTab;

    private BitmapDrawable imageDrawable;

    private FetchWallpaperWorkerTask mWorkerTask;
    private boolean isFromBottomSheet;
    private NTPBackgroundImagesBridge mNTPBackgroundImagesBridge;
    private ViewGroup mainLayout;
    private DatabaseHelper mDatabaseHelper;

    private View mBraveStatsView;
    private ViewGroup mSiteSectionView;
    private LottieAnimationView mBadgeAnimationView;
    private VerticalViewPager ntpWidgetViewPager;
    private NTPWidgetAdapter ntpWidgetAdapter;

    private Tab mTab;
    private Activity mActivity;
    private LinearLayout indicatorLayout;

    public BraveNewTabPageLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mProfile = Profile.getLastUsedRegularProfile();
        mNTPBackgroundImagesBridge = NTPBackgroundImagesBridge.getInstance(mProfile);
        mNTPBackgroundImagesBridge.setNewTabPageListener(newTabPageListener);
        mDatabaseHelper = DatabaseHelper.getInstance();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        indicatorLayout = findViewById(R.id.indicator_layout);

        ntpWidgetViewPager = findViewById(R.id.ntp_widget_view_pager);
        ntpWidgetAdapter = new NTPWidgetAdapter();
        ntpWidgetAdapter.setNTPWidgetMenuListener(ntpWidgetMenuListener);
        ntpWidgetViewPager.setAdapter(ntpWidgetAdapter);

        ntpWidgetViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(
                    int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                updateAndShowIndicators(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });
    }

    private void addWidgets() {
        LayoutInflater inflater =
                (LayoutInflater) ContextUtils.getApplicationContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);

        mBraveStatsView = inflater.inflate(R.layout.brave_stats_layout, null);
        mBraveStatsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkForBraveStats();
            }
        });
        ntpWidgetAdapter.addView(mBraveStatsView);

        boolean showPlaceholder = getTileGroup().hasReceivedData() && getTileGroup().isEmpty();

        if (mSiteSectionView != null && !showPlaceholder) {
            if (!mNTPBackgroundImagesBridge.isSuperReferral()
                    || !NTPBackgroundImagesBridge.enableSponsoredImages()
                    || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                ntpWidgetAdapter.addView(mSiteSectionView);
            }
        }

        View binanceWidgetView = inflater.inflate(R.layout.crypto_widget_layout, null);
        ntpWidgetAdapter.addView(binanceWidgetView);

        ntpWidgetAdapter.notifyDataSetChanged();
    }

    private void updateAndShowIndicators(int position) {
        Context context = ContextUtils.getApplicationContext();
        indicatorLayout.removeAllViews();
        for (int i = 0; i < ntpWidgetAdapter.getCount(); i++) {
            TextView dotTextView = new TextView(context);
            dotTextView.setText(Html.fromHtml("&#9679;"));
            dotTextView.setTextColor(context.getResources().getColor(android.R.color.white));
            dotTextView.setTextSize(8);
            if (position == i) {
                dotTextView.setAlpha(1.0f);
            } else {
                dotTextView.setAlpha(0.4f);
            }
            indicatorLayout.addView(dotTextView);
        }
    }

    private void checkForBraveStats() {
        if (OnboardingPrefManager.getInstance().isBraveStatsEnabled()) {
            BraveStatsUtil.showBraveStats();
        } else {
            setWidgetCurrentPage(mBraveStatsView);
            ntpWidgetAdapter.notifyDataSetChanged();
            ((BraveActivity)mActivity).showOnboardingV2(true);
        }
    }

    protected void insertSiteSectionView() {
        mainLayout = findViewById(R.id.ntp_main_layout);

        mSiteSectionView = SiteSection.inflateSiteSection(mainLayout);
        ViewGroup.LayoutParams layoutParams = mSiteSectionView.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        // If the explore sites section exists as its own section, then space it more closely.
        int variation = ExploreSitesBridge.getVariation();
        if (ExploreSitesBridge.isEnabled(variation)) {
            ((MarginLayoutParams) layoutParams).bottomMargin =
                getResources().getDimensionPixelOffset(
                    R.dimen.tile_grid_layout_vertical_spacing);
        }
        mSiteSectionView.setLayoutParams(layoutParams);
    }

    protected int getMaxRowsForMostVisitedTiles() {
        if (UserPrefs.get(Profile.getLastUsedRegularProfile()).getBoolean(BravePref.NEW_TAB_PAGE_SHOW_BACKGROUND_IMAGE)
                && NTPUtil.shouldEnableNTPFeature()) {
            return 1;
        } else {
            return 2;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (sponsoredTab == null) {
            initilizeSponsoredTab();
        }
        if (getPlaceholder() != null
                && ((ViewGroup)getPlaceholder().getParent()) != null) {
            ((ViewGroup)getPlaceholder().getParent()).removeView(getPlaceholder());
        }
        checkAndShowNTPImage(false);
        mNTPBackgroundImagesBridge.addObserver(mNTPBackgroundImageServiceObserver);
        if (PackageUtils.isFirstInstall(ContextUtils.getApplicationContext())
                && !OnboardingPrefManager.getInstance().isNewOnboardingShown()) {
            ((BraveActivity)mActivity).showOnboardingV2(false);
        }
        if (OnboardingPrefManager.getInstance().isFromNotification() ) {
            ((BraveActivity)mActivity).showOnboardingV2(false);
            OnboardingPrefManager.getInstance().setFromNotification(false);
        }
        if (mBadgeAnimationView != null
                && !OnboardingPrefManager.getInstance().shouldShowBadgeAnimation()) {
            mBadgeAnimationView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mWorkerTask != null && mWorkerTask.getStatus() == AsyncTask.Status.RUNNING) {
            mWorkerTask.cancel(true);
            mWorkerTask = null;
        }

        if (!isFromBottomSheet) {
            setBackgroundResource(0);
            if (imageDrawable != null && imageDrawable.getBitmap() != null && !imageDrawable.getBitmap().isRecycled()) {
                imageDrawable.getBitmap().recycle();
            }
        }
        mNTPBackgroundImagesBridge.removeObserver(mNTPBackgroundImageServiceObserver);
        super.onDetachedFromWindow();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (sponsoredTab != null && NTPUtil.shouldEnableNTPFeature()) {
            if (bgImageView != null) {
                // We need to redraw image to fit parent properly
                bgImageView.setImageResource(android.R.color.transparent);
            }
            NTPImage ntpImage = sponsoredTab.getTabNTPImage(false);
            if (ntpImage == null) {
                sponsoredTab.setNTPImage(SponsoredImageUtil.getBackgroundImage());
            } else if (ntpImage instanceof Wallpaper) {
                Wallpaper mWallpaper = (Wallpaper) ntpImage;
                if (mWallpaper == null) {
                    sponsoredTab.setNTPImage(SponsoredImageUtil.getBackgroundImage());
                }
            }
            checkForNonDistruptiveBanner(ntpImage);
            super.onConfigurationChanged(newConfig);
            showNTPImage(ntpImage);
        } else {
            super.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void initialize(NewTabPageManager manager, Activity activity,
            TileGroup.Delegate tileGroupDelegate, boolean searchProviderHasLogo,
            boolean searchProviderIsGoogle, ScrollDelegate scrollDelegate,
            ContextMenuManager contextMenuManager, UiConfig uiConfig, Supplier<Tab> tabProvider,
            ActivityLifecycleDispatcher lifecycleDispatcher, NewTabPageUma uma) {
        super.initialize(manager, activity, tileGroupDelegate, searchProviderHasLogo,
                searchProviderIsGoogle, scrollDelegate, contextMenuManager, uiConfig, tabProvider,
                lifecycleDispatcher, uma);

        assert (activity instanceof BraveActivity);
        mActivity = activity;
        ((BraveActivity)mActivity).dismissShieldsTooltip();

        addWidgets();
        updateAndShowIndicators(0);
    }

    private void showNTPImage(NTPImage ntpImage) {
        NTPUtil.updateOrientedUI(mActivity, this);
        ImageView mSponsoredLogo = (ImageView) findViewById(R.id.sponsored_logo);
        FloatingActionButton mSuperReferralLogo = (FloatingActionButton) findViewById(R.id.super_referral_logo);
        TextView mCreditText = (TextView) findViewById(R.id.credit_text);
        if (ntpImage instanceof Wallpaper
                && NTPUtil.isReferralEnabled()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setBackgroundImage(ntpImage);
            mSuperReferralLogo.setVisibility(View.VISIBLE);
            mCreditText.setVisibility(View.GONE);
            int floatingButtonIcon =
                GlobalNightModeStateProviderHolder.getInstance().isInNightMode()
                ? R.drawable.qrcode_dark
                : R.drawable.qrcode_light;
            mSuperReferralLogo.setImageResource(floatingButtonIcon);
            mSuperReferralLogo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SuperReferralShareDialogFragment mSuperReferralShareDialogFragment =
                        new SuperReferralShareDialogFragment();
                    mSuperReferralShareDialogFragment.show(
                        ((BraveActivity) mActivity).getSupportFragmentManager(),
                        "SuperReferralShareDialogFragment");
                }
            });
        } else if (UserPrefs.get(Profile.getLastUsedRegularProfile()).getBoolean(
                       BravePref.NEW_TAB_PAGE_SHOW_BACKGROUND_IMAGE)
                   && sponsoredTab != null
                   && NTPUtil.shouldEnableNTPFeature()) {
            setBackgroundImage(ntpImage);
            if (ntpImage instanceof BackgroundImage) {
                BackgroundImage backgroundImage = (BackgroundImage) ntpImage;
                mSponsoredLogo.setVisibility(View.GONE);
                mSuperReferralLogo.setVisibility(View.GONE);
                if (backgroundImage.getImageCredit() != null) {
                    String imageCreditStr = String.format(getResources().getString(R.string.photo_by, backgroundImage.getImageCredit().getName()));

                    SpannableStringBuilder spannableString = new SpannableStringBuilder(imageCreditStr);
                    spannableString.setSpan(
                        new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        ((imageCreditStr.length() - 1)
                         - (backgroundImage.getImageCredit().getName().length() - 1)),
                        imageCreditStr.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    mCreditText.setText(spannableString);
                    mCreditText.setVisibility(View.VISIBLE);
                    mCreditText.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (backgroundImage.getImageCredit() != null) {
                                NTPUtil.openUrlInSameTab(backgroundImage.getImageCredit().getUrl());
                            }
                        }
                    });
                }
            }
        }
    }

    private void setBackgroundImage(NTPImage ntpImage) {
        bgImageView = (ImageView) findViewById(R.id.bg_image_view);
        bgImageView.setScaleType(ImageView.ScaleType.MATRIX);

        ViewTreeObserver observer = bgImageView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mWorkerTask = new FetchWallpaperWorkerTask(ntpImage, bgImageView.getMeasuredWidth(), bgImageView.getMeasuredHeight(), wallpaperRetrievedCallback);
                mWorkerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                bgImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void checkForNonDistruptiveBanner(NTPImage ntpImage) {
        int brOption = NTPUtil.checkForNonDistruptiveBanner(ntpImage, sponsoredTab);
        if (SponsoredImageUtil.BR_INVALID_OPTION != brOption && !NTPUtil.isReferralEnabled()) {
            NTPUtil.showNonDistruptiveBanner((BraveActivity) mActivity, this, brOption,
                                             sponsoredTab, newTabPageListener);
        }
    }

    private void checkAndShowNTPImage(boolean isReset) {
        NTPImage ntpImage = sponsoredTab.getTabNTPImage(isReset);
        if (ntpImage == null) {
            sponsoredTab.setNTPImage(SponsoredImageUtil.getBackgroundImage());
        } else if (ntpImage instanceof Wallpaper) {
            Wallpaper mWallpaper = (Wallpaper) ntpImage;
            if (mWallpaper == null) {
                sponsoredTab.setNTPImage(SponsoredImageUtil.getBackgroundImage());
            }
        }
        checkForNonDistruptiveBanner(ntpImage);
        showNTPImage(ntpImage);
    }

    private void initilizeSponsoredTab() {
        if (TabAttributes.from(getTab()).get(String.valueOf(getTabImpl().getId())) == null) {
            SponsoredTab mSponsoredTab = new SponsoredTab(mNTPBackgroundImagesBridge);
            TabAttributes.from(getTab()).set(String.valueOf(getTabImpl().getId()), mSponsoredTab);
        }
        sponsoredTab = TabAttributes.from(getTab()).get(String.valueOf((getTabImpl()).getId()));
        if (mNTPBackgroundImagesBridge.isSuperReferral()
                && NTPBackgroundImagesBridge.enableSponsoredImages()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            mNTPBackgroundImagesBridge.getTopSites();
    }

    private NewTabPageListener newTabPageListener = new NewTabPageListener() {
        @Override
        public void updateInteractableFlag(boolean isBottomSheet) {
            isFromBottomSheet = isBottomSheet;
        }

        @Override
        public void updateNTPImage() {
            if (sponsoredTab == null) {
                initilizeSponsoredTab();
            }
            checkAndShowNTPImage(false);
        }

        @Override
        public void updateTopSites(List<TopSite> topSites) {
            new AsyncTask<Void>() {
                @Override
                protected Void doInBackground() {
                    for (TopSite topSite : topSites) {
                        mDatabaseHelper.insertTopSite(topSite);
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    assert ThreadUtils.runningOnUiThread();
                    if (isCancelled()) return;

                    List<TopSiteTable> topSites = mDatabaseHelper.getAllTopSites();
                    loadTopSites(topSites);
                }
            } .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    };

    private NTPBackgroundImagesBridge.NTPBackgroundImageServiceObserver mNTPBackgroundImageServiceObserver = new NTPBackgroundImagesBridge.NTPBackgroundImageServiceObserver() {
        @Override
        public void onUpdated() {
            if (NTPUtil.isReferralEnabled()) {
                checkAndShowNTPImage(true);
                if (mNTPBackgroundImagesBridge.isSuperReferral()
                        && NTPBackgroundImagesBridge.enableSponsoredImages()
                        && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    removeDefaultTopSites();
                    mNTPBackgroundImagesBridge.getTopSites();
                }
            }
        }
    };

    private FetchWallpaperWorkerTask.WallpaperRetrievedCallback wallpaperRetrievedCallback = new FetchWallpaperWorkerTask.WallpaperRetrievedCallback() {
        @Override
        public void bgWallpaperRetrieved(Bitmap bgWallpaper) {
            bgImageView.setImageBitmap(bgWallpaper);
        }

        @Override
        public void logoRetrieved(Wallpaper mWallpaper, Bitmap logoWallpaper) {
            if (!NTPUtil.isReferralEnabled()) {
                FloatingActionButton mSuperReferralLogo = (FloatingActionButton) findViewById(R.id.super_referral_logo);
                mSuperReferralLogo.setVisibility(View.GONE);

                ImageView sponsoredLogo = (ImageView) findViewById(R.id.sponsored_logo);
                sponsoredLogo.setVisibility(View.VISIBLE);
                sponsoredLogo.setImageBitmap(logoWallpaper);
                sponsoredLogo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mWallpaper.getLogoDestinationUrl() != null) {
                            NTPUtil.openUrlInSameTab(mWallpaper.getLogoDestinationUrl());
                        }
                    }
                });
            }
        }
    };

    private void loadTopSites(List<TopSiteTable> topSites) {
        LinearLayout superReferralSitesLayout = (LinearLayout) findViewById(R.id.ntp_super_referral_sites_layout);
        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        for (TopSiteTable topSite : topSites) {
            final View view = inflater.inflate(R.layout.suggestions_tile_view, null);

            TextView tileViewTitleTv = view.findViewById(R.id.tile_view_title);
            tileViewTitleTv.setText(topSite.getName());

            if (!GlobalNightModeStateProviderHolder.getInstance().isInNightMode()
                    && !UserPrefs.get(Profile.getLastUsedRegularProfile()).getBoolean(BravePref.NEW_TAB_PAGE_SHOW_BACKGROUND_IMAGE)
                    && !NTPUtil.isReferralEnabled()) {
                tileViewTitleTv.setTextColor(getResources().getColor(android.R.color.black));
            } else {
                tileViewTitleTv.setTextColor(getResources().getColor(android.R.color.white));
            }

            ImageView iconIv = view.findViewById(R.id.tile_view_icon);
            if (NTPUtil.imageCache.get(topSite.getDestinationUrl()) == null) {
                NTPUtil.imageCache.put(topSite.getDestinationUrl(), new java.lang.ref.SoftReference(NTPUtil.getTopSiteBitmap(topSite.getImagePath())));
            }
            iconIv.setImageBitmap(NTPUtil.imageCache.get(topSite.getDestinationUrl()).get());
            iconIv.setClickable(false);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    NTPUtil.openUrlInSameTab(topSite.getDestinationUrl());
                }
            });

            int paddingTop = getResources().getDimensionPixelSize(R.dimen.tile_grid_layout_no_logo_padding_top);
            view.setPadding(0, paddingTop, 0, view.getPaddingBottom());

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
            layoutParams.weight = 0.25f;
            layoutParams.gravity = Gravity.CENTER;
            view.setLayoutParams(layoutParams);
            view.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                @Override
                public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                    menu.add(R.string.contextmenu_open_in_new_tab).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            NTPUtil.openNewTab(false, topSite.getDestinationUrl());
                            return true;
                        }
                    });
                    menu.add(R.string.contextmenu_open_in_incognito_tab).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            NTPUtil.openNewTab(true, topSite.getDestinationUrl());
                            return true;
                        }
                    });
                    menu.add(R.string.contextmenu_save_link).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            if (getTab() != null) {
                                OfflinePageBridge.getForProfile(mProfile).scheduleDownload(getTab().getWebContents(),
                                        OfflinePageBridge.NTP_SUGGESTIONS_NAMESPACE, topSite.getDestinationUrl(), DownloadUiActionFlags.ALL);
                            } else {
                                RequestCoordinatorBridge.getForProfile(mProfile).savePageLater(
                                    topSite.getDestinationUrl(), OfflinePageBridge.NTP_SUGGESTIONS_NAMESPACE, true /* userRequested */);
                            }
                            return true;
                        }
                    });
                    menu.add(R.string.remove).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            NTPUtil.imageCache.remove(topSite.getDestinationUrl());
                            mDatabaseHelper.deleteTopSite(topSite.getDestinationUrl());
                            NTPUtil.addToRemovedTopSite(topSite.getDestinationUrl());
                            superReferralSitesLayout.removeView(view);
                            return true;
                        }
                    });
                }
            });
            superReferralSitesLayout.addView(view);
        }
    }

    public void removeDefaultTopSites() {
        if (mainLayout != null && hasParent(mSiteSectionView)) {
            mainLayout.removeView(mSiteSectionView);
        }
    }

    private boolean hasParent(View view) {
        return view != null && view.getParent() != null;
    }

    public void setTab(Tab tab) {
        mTab = tab;
    }

    private Tab getTab() {
        assert mTab != null;
        return mTab;
    }

    private TabImpl getTabImpl() {
        return (TabImpl) getTab();
    }

    // NTP related methods
    public void addViewToWidget(View newPage) {
        int pageIndex = ntpWidgetAdapter.addView(newPage);
        ntpWidgetViewPager.setCurrentItem(pageIndex, true);
    }

    public void removeViewFromWidget(int position) {
        int pageIndex = ntpWidgetAdapter.removeView(ntpWidgetViewPager, position);
        Log.e("NTP", "PageIndex : " + pageIndex);
        if (pageIndex == ntpWidgetAdapter.getCount()) {
            Log.e("NTP", "inside condition : " + pageIndex);
            pageIndex--;
        }
        ntpWidgetViewPager.setCurrentItem(pageIndex);
    }

    public View getWidgetCurrentPage() {
        return ntpWidgetAdapter.getView(ntpWidgetViewPager.getCurrentItem());
    }

    public void setWidgetCurrentPage(View pageToShow) {
        ntpWidgetViewPager.setCurrentItem(ntpWidgetAdapter.getItemPosition(pageToShow), true);
    }

    private NTPWidgetAdapter.NTPWidgetMenuListener ntpWidgetMenuListener =
            new NTPWidgetAdapter.NTPWidgetMenuListener() {
                @Override
                public void onEdit() {
                    NTPWidgetBottomSheetDialogFragment ntpWidgetBottomSheetDialogFragment =
                            NTPWidgetBottomSheetDialogFragment.newInstance();
                    ntpWidgetBottomSheetDialogFragment.show(
                            ((BraveActivity) mActivity).getSupportFragmentManager(),
                            "NTPWidgetBottomSheetDialogFragment");
                }

                @Override
                public void onRemove(int position) {
                    removeViewFromWidget(position);
                }
            };
}
