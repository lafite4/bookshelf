package com.smartjinyu.mybookshelf.ui.main;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.lapism.searchview.SearchView;
import com.smartjinyu.mybookshelf.R;
import com.smartjinyu.mybookshelf.base.SimpleActivity;
import com.smartjinyu.mybookshelf.model.BookShelfLab;
import com.smartjinyu.mybookshelf.model.LabelLab;
import com.smartjinyu.mybookshelf.model.bean.BookShelf;
import com.smartjinyu.mybookshelf.model.bean.Label;
import com.smartjinyu.mybookshelf.support.UpdateCheck;
import com.smartjinyu.mybookshelf.ui.about.AboutActivity;
import com.smartjinyu.mybookshelf.ui.addbook.BatchAddActivity;
import com.smartjinyu.mybookshelf.ui.addbook.SingleAddActivity;
import com.smartjinyu.mybookshelf.ui.setting.SettingsActivity;
import com.smartjinyu.mybookshelf.util.AlertUtil;
import com.smartjinyu.mybookshelf.util.AppUtil;
import com.smartjinyu.mybookshelf.util.AnswersUtil;
import com.smartjinyu.mybookshelf.util.SharedPrefUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import moe.feng.alipay.zerosdk.AlipayZeroSdk;

import static com.smartjinyu.mybookshelf.util.SharedPrefUtil.LAUNCH_TIMES;


public class MainActivity extends SimpleActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String ACTION_SEARCH = "com.smartjinyu.mybookshelf.ACTION_SEARCH";

    @BindView(R.id.main_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.toolbar_spinner)
    Spinner mBookshelfSpinner;
    @BindView(R.id.searchView)
    SearchView mSearchView;
    @BindView(R.id.fab_menu_add)
    FloatingActionMenu mFabAddMenu;
    @BindView(R.id.fab_menu_item_1)
    FloatingActionButton mFabSingleAdd;
    @BindView(R.id.fab_menu_item_2)
    FloatingActionButton mFabBatchAdd;
    @BindView(R.id.nav_view)
    NavigationView mNavView;
    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;

    private ArrayAdapter<BookShelf> mBSSpinnerAdapter;
    private List<BookShelf> mBookshelfList;

    private SubMenu mLabelsSubMenu;
    // labels display in navigation view,
    // keys response to the click event, values are labels
    private Map<Integer, Label> mLabelMap;

    private MainFragment mMainFragment;
    private Context mContext;

    private boolean actionSearch;

    @Override
    protected String getTag() {
        return "MainActivity";
    }

    @Override
    protected int getLayoutId() {
        // Fabric init here
//        AnswersUtil.init(this);
        return R.layout.activity_main;
    }

    @Override
    protected void initEventAndData() {
        AnswersUtil.logContentView(TAG, "Activity", "1001", "onCreate", "onCreate");
        mContext = MainActivity.this;

        mMainFragment = MainFragment.newInstance();
        getSupportFragmentManager().beginTransaction().add(R.id.content_main, mMainFragment).commit();

        setFloatingActionButton();
        // not show the title
        setupToolbar(mToolbar, null);
        initDrawerLayout();
        setupBookShelfSpinner();
        setSearchView();

        if (getIntent().getAction() != null && getIntent().getAction().equals(ACTION_SEARCH)) {
            actionSearch = true;
        }

        if (SharedPrefUtil.getInstance().getInt(LAUNCH_TIMES, 1) == 1) {
            AlertUtil.alertWebview(this, getString(
                    R.string.about_preference_term_of_service), "termOfService.html");
        }

        if (SharedPrefUtil.getInstance().getBoolean(SharedPrefUtil.CHECK_UPDATE, true)) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    new UpdateCheck(mContext);
                }
            }, 3000);
        }
    }

    private void initDrawerLayout() {
        // ???????????????
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        mNavView.setNavigationItemSelectedListener(this);
        mNavView.setCheckedItem(R.id.drawer_item_bookshelf);
        mNavView.setItemIconTintList(null);

        mLabelsSubMenu = mNavView.getMenu().findItem(R.id.drawer_item_added_labels).getSubMenu();
        mLabelMap = new ArrayMap<>();
        refreshLabelMenuItem();
    }

    private void setupBookShelfSpinner() {
        mBookshelfList = new ArrayList<>();
        mBSSpinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_white, mBookshelfList);
        mBSSpinnerAdapter.setDropDownViewResource(R.layout.spinner_drop_down_white);
        mBookshelfSpinner.setAdapter(mBSSpinnerAdapter);
        mBookshelfSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                BookShelf bookShelf = mBookshelfList.get(i);
                if (bookShelf.getTitle().equals(getString(R.string.spinner_all_bookshelf))) {
                    setToolbarColor(0);
                    bookShelf = null;
                } else {// certain bookshelf
                    setToolbarColor(1);
                }
                mMainFragment.selectBookshelf(bookShelf);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        refreshBookShelfSpinner();
    }

    public void refreshBookShelfSpinner() {
        List<BookShelf> bookShelfList = BookShelfLab.get(mContext).getBookShelves();
        mBookshelfList.clear();
        BookShelf allBookshelf = new BookShelf();
        allBookshelf.setTitle(getString(R.string.spinner_all_bookshelf));
        mBookshelfList.add(allBookshelf);
        for (int i = 0; i < bookShelfList.size(); i++)
            mBookshelfList.add(bookShelfList.get(i));
        mBSSpinnerAdapter.notifyDataSetChanged();
    }

    private void setSearchView() {
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.i(TAG, "Search " + query);
                AnswersUtil.logContentView(TAG, "Activity", "1002", "Search", "Search Text Submitted");
                mSearchView.hideKeyboard();
                mMainFragment.doSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        mSearchView.setOnOpenCloseListener(new SearchView.OnOpenCloseListener() {
            @Override
            public boolean onClose() {
                Log.d(TAG, "SearchView close");
                Log.d(TAG, "Show FAM 2");
                showFabMenu();
                mMainFragment.refreshFetch();
                return true;
            }

            @Override
            public boolean onOpen() {
                Log.d(TAG, "SearchView open");
                Log.d(TAG, "Hide FAM 2");
                hideFabMenu();
                return false;
            }
        });
    }

    private void setFloatingActionButton() {
        mFabSingleAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "fab menu item 1 clicked");
                Intent i = new Intent(mContext, SingleAddActivity.class);
                startActivity(i);
                openFabMenu(false);
            }
        });
        mFabBatchAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "fab menu item 2 clicked");
                Intent i = new Intent(mContext, BatchAddActivity.class);
                startActivity(i);
                openFabMenu(false);
            }
        });
        mFabAddMenu.setMenuButtonShowAnimation(AnimationUtils.loadAnimation(this, R.anim.show_from_bottom));
        mFabAddMenu.setMenuButtonHideAnimation(AnimationUtils.loadAnimation(this, R.anim.hide_to_bottom));
    }

    /**
     * change Toolbar and status bar color
     *
     * @param mode 0 represents colorPrimary\colorPrimaryDark, 1 represents selected|selectedDark
     */
    private void setToolbarColor(int mode) {
        int colorPrimaryRes, colorPrimaryDarkRes;
        if (mode == 1) {
            colorPrimaryRes = ContextCompat.getColor(this, R.color.selected_primary);
            colorPrimaryDarkRes = ContextCompat.getColor(this, R.color.selected_primary_dark);
        } else {
            colorPrimaryRes = ContextCompat.getColor(this, R.color.colorPrimary);
            colorPrimaryDarkRes = ContextCompat.getColor(this, R.color.colorPrimaryDark);
        }
        mToolbar.setBackgroundColor(colorPrimaryRes);
        getWindow().setStatusBarColor(colorPrimaryDarkRes);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume, mSearchView open = " + mSearchView.isSearchOpen());
        if (mBookshelfSpinner != null) {
            // user may create new bookshelf in edit or creating new book
            refreshBookShelfSpinner();
            mMainFragment.refreshFetch();
        }
        Log.d(TAG, "ACTION_SEARCH = " + actionSearch);
        if (actionSearch) {
            openSearchView(true);
            hideFabMenu();
            actionSearch = false;
        }
    }

    @Override
    public void onBackPressed() {
        // action menu (multi select) has been processed
        // first close search view , fab menu , drawer view
        boolean isSearchViewOpen = mSearchView.isSearchOpen();
        boolean isFabMenuOpen = mFabAddMenu.isOpened();
        boolean isDrawerOpen = mDrawerLayout.isDrawerOpen(GravityCompat.START);
        if (isSearchViewOpen || isFabMenuOpen || isDrawerOpen) {
            if (isSearchViewOpen) openSearchView(false);
            if (isFabMenuOpen) openFabMenu(false);
            if (isDrawerOpen) mDrawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        // if search view ,drawer view and fab menu are closed, then process others
        int startTimes = SharedPrefUtil.getInstance().getInt(LAUNCH_TIMES, 1);
        Log.i(TAG, "startTimes = " + startTimes);
        SharedPrefUtil.getInstance().putInt(LAUNCH_TIMES, startTimes + 1);

        boolean muteRatings = SharedPrefUtil.getInstance().getBoolean(SharedPrefUtil.MUTE_RATINGS, false);
        boolean isRated = SharedPrefUtil.getInstance().getBoolean(SharedPrefUtil.IS_RATED, false);
        boolean isDonateItemShow = SharedPrefUtil.getInstance().getBoolean(SharedPrefUtil.DONATE_DRAWER_ITEM_SHOW, true);
        Log.i(TAG, "rating info muteRatings = " + muteRatings + ", isRated = " + isRated);

        if (!muteRatings && !isRated && startTimes % getResources().getInteger(R.integer.rating_after_start_times) == 0) {
            //&&mBooks.size() > getResources().getInteger(R.integer.rating_if_books_more_than)
            // show ratings dialog
            showRatingDialog();
        } else if (isDonateItemShow && startTimes % getResources().getInteger(R.integer.donate_after_start_times) == 0) {
            showDonateDialog();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SearchView.SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && results.size() > 0) {
                String searchWrd = results.get(0);
                if (!TextUtils.isEmpty(searchWrd))
                    if (mSearchView != null) mSearchView.setQuery(searchWrd, true);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showRatingDialog() {
        AnswersUtil.logContentView(TAG, "Rating", "2100", "Rating Dialog show", 1 + "");

        new MaterialDialog.Builder(this)
                .title(R.string.rating_dialog_title).content(R.string.rating_dialog_content)
                .positiveText(R.string.rating_dialog_positive).onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                SharedPrefUtil.getInstance().putBoolean(SharedPrefUtil.IS_RATED, true);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("market://details?id=com.smartjinyu.mybookshelf"));
                startActivity(i);
                AnswersUtil.logContentView(TAG, "Rating", "2101", "Go to Store", 1 + "");

                MainActivity.super.onBackPressed();
            }
        }).negativeText(android.R.string.cancel).onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                AnswersUtil.logContentView(TAG, "Rating", "2102", "Cancel Rating", 1 + "");
                MainActivity.super.onBackPressed();
            }
        }).neutralText(R.string.rating_dialog_neutral).onNeutral(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                SharedPrefUtil.getInstance().putBoolean(SharedPrefUtil.IS_RATED, true);
                AnswersUtil.logContentView(TAG, "Rating", "2102", "Mute Rating", 1 + "");
                MainActivity.super.onBackPressed();
            }
        }).canceledOnTouchOutside(false).show();
    }

    private void showDonateDialog() {
        AnswersUtil.logContentView(TAG, "Donate", "2030", "Donate Clicked", "Donate Clicked");
        Log.i(TAG, "Donate Dialog show");
        boolean hasInstalledAlipayClient = AlipayZeroSdk.hasInstalledAlipayClient(mContext);
        if (hasInstalledAlipayClient) {
            new MaterialDialog.Builder(mContext)
                    .title(R.string.about_preference_donate_title).content(R.string.about_donate_dialog_content)
                    .positiveText(R.string.about_donate_dialog_positive0).onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    AlipayZeroSdk.startAlipayClient(MainActivity.this, getString(R.string.about_donate_alipay_qrcode));
                    AnswersUtil.logContentView(TAG, "Donate", "2031", "Alipay Clicked", "Alipay Clicked");
                    SharedPrefUtil.getInstance().putBoolean(SharedPrefUtil.DONATE_DRAWER_ITEM_SHOW, false);
                    dialog.dismiss();
//                            setDrawer(mDrawer.getCurrentSelection());
                }
            }).negativeText(R.string.about_donate_dialog_negative0).onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    AppUtil.copyText2Clipboard(mContext, "smartjinyu@gmail.com");
                    Toast.makeText(mContext, getString(R.string.about_preference_donate_toast), Toast.LENGTH_SHORT).show();
                    AnswersUtil.logContentView(TAG, "Donate", "2032", "Copy to clipboard Clicked", "Copy to clipboard Clicked");
                    SharedPrefUtil.getInstance().putBoolean(SharedPrefUtil.DONATE_DRAWER_ITEM_SHOW, false);
                    dialog.dismiss();
//                  setDrawer(mDrawer.getCurrentSelection());
                }
            }).neutralText(android.R.string.cancel).onNeutral(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    AnswersUtil.logContentView(TAG, "Donate", "2033", "Cancel Clicked", "Cancel Clicked");
                    dialog.dismiss();
                }
            }).canceledOnTouchOutside(false).show();
        } else {
            new MaterialDialog.Builder(mContext)
                    .title(R.string.about_preference_donate_title).content(R.string.about_donate_dialog_content)
                    .positiveText(R.string.about_donate_dialog_negative0).onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    AppUtil.copyText2Clipboard(mContext, "smartjinyu@gmail.com");
                    Toast.makeText(mContext, getResources().getString(R.string.about_preference_donate_toast), Toast.LENGTH_SHORT).show();
                    AnswersUtil.logContentView(TAG, "Donate", "2032", "Copy to clipboard Clicked", "Copy to clipboard Clicked");
                    SharedPrefUtil.getInstance().putBoolean(SharedPrefUtil.DONATE_DRAWER_ITEM_SHOW, false);
                    dialog.dismiss();
//                            setDrawer(mDrawer.getCurrentSelection());
                }
            }).negativeText(android.R.string.cancel).onNegative(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    AnswersUtil.logContentView(TAG, "Donate", "2033", "Cancel Clicked", "Cancel Clicked");
                    dialog.dismiss();
                }
            }).canceledOnTouchOutside(false).show();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        mDrawerLayout.closeDrawer(GravityCompat.START);
        // close search view
        openSearchView(false);
        openFabMenu(false);
        switch (item.getItemId()) {
            case R.id.drawer_item_bookshelf:
                mMainFragment.selectLabel(null);
                break;
            case R.id.drawer_item_search:
                openSearchView(true);
                break;
            case R.id.drawer_item_add_label:
                newLabel();
                break;
            case R.id.drawer_item_donate:
                showDonateDialog();
                break;
            case R.id.drawer_item_setting:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            case R.id.drawer_item_about:
                startActivity(new Intent(MainActivity.this, AboutActivity.class));
                break;
            default:
                openSearchView(false);
                Label label = mLabelMap.get(item.getItemId());
                if (label != null) {
                    mMainFragment.selectLabel(label);
                    Toast.makeText(mContext, label.getTitle() + " ?????????", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return true;
    }

    private void newLabel() {
        new MaterialDialog.Builder(mContext)
                .title(R.string.label_add_new_dialog_title)
                .inputRange(1, getResources().getInteger(R.integer.label_name_max_length))
                .input(R.string.label_add_new_dialog_edit_text, 0, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog1, CharSequence input) {
                        // nothing to do here
                    }
                }).onPositive(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog inputDialog, @NonNull DialogAction which) {
                EditText etLabel = inputDialog.getInputEditText();
                if (etLabel == null) return;
                Label labelToAdd = new Label();
                labelToAdd.setTitle(etLabel.getText().toString());
                LabelLab.get(mContext).addLabel(labelToAdd);
                Log.i(TAG, "New label created " + labelToAdd.getTitle());
                refreshLabelMenuItem();
            }
        }).negativeText(android.R.string.cancel).onNegative(new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog inputDialog, @NonNull DialogAction which) {
                inputDialog.dismiss();
            }
        }).show();
    }

    public void refreshLabelMenuItem() {
        List<Label> labelList = LabelLab.get(mContext).getLabels();
        if (labelList == null || labelList.size() <= 0) return;
        mLabelMap.clear();
        mLabelsSubMenu.clear();
        int index = 0;
        for (Label label : labelList) {
            MenuItem item = mLabelsSubMenu.add(0, index, 0, label.getTitle());
            mLabelMap.put(index, label);
            item.setIcon(R.drawable.ic_label).setCheckable(true);
            index++;
        }
    }

    public void openSearchView(boolean state) {
        if (mSearchView == null) return;
        if (!mSearchView.isSearchOpen() && state) {
            openFabMenu(false);
            mSearchView.open(true);
        } else if (mSearchView.isSearchOpen() && !state)
            mSearchView.close(true);
    }

    public void openFabMenu(boolean state) {
        if (mFabAddMenu == null) return;
        if (!mFabAddMenu.isOpened() && state) {
            openSearchView(false);
            mFabAddMenu.open(true);
        }
        if (mFabAddMenu.isOpened() && !state)
            mFabAddMenu.close(true);
    }

    public boolean isFabMenuOpened() {
        return mFabAddMenu.isOpened();
    }

    public void hideFabMenu() {
        mFabAddMenu.setVisibility(View.GONE);
        mFabAddMenu.hideMenuButton(true);
    }

    public void showFabMenu() {
        mFabAddMenu.setVisibility(View.VISIBLE);
        mFabAddMenu.showMenuButton(true);
    }
}
