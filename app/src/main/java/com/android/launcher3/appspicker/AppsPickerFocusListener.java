package com.android.launcher3.appspicker;

import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;
import com.android.launcher3.util.focus.FocusListener;
import com.sec.android.app.launcher.R;

public class AppsPickerFocusListener extends FocusListener {
    private static final String TAG = "AppsPickerFocusListener";

    private static boolean handleAppListIconKeyEvent(View v, int keyCode, KeyEvent e) {
        LinearLayout rowView;
        if (v.getParent().getParent() instanceof LinearLayout) {
            rowView = (LinearLayout) v.getParent().getParent();
        } else {
            rowView = (LinearLayout) v.getParent();
        }
        AppIconViewHolder[] viewHolders = (AppIconViewHolder[]) rowView.getTag();
        ListView listView = (ListView) rowView.getParent();
        SearchView searchView = (SearchView) ((FrameLayout) v.getRootView().findViewById(R.id.apps_picker_app_search_box_container)).findViewById(R.id.apps_picker_app_search_input);
        if (listView == null || listView.getParent() == null) {
            return false;
        }
        int iconIndex = 0;
        int maxRowItem = v.getContext().getResources().getInteger(R.integer.config_appsPicker_NumAppsPerRow);
        int rowIndex = viewHolders[0].rowIndex;
        for (int i = 0; i < maxRowItem; i++) {
            if (v == viewHolders[i].container) {
                iconIndex = viewHolders[i].colIndex;
            }
        }
        boolean handleKeyEvent = e.getAction() != 1;
        int firstRowView;
        int lastVisibleIcon;
        switch (keyCode) {
            case 19:
                if (handleKeyEvent) {
                    if (rowIndex > 0) {
                        firstRowView = listView.getFirstVisiblePosition();
                        if (rowIndex < firstRowView + 1) {
                            listView.smoothScrollToPosition(rowIndex - 1);
                            return true;
                        }
                        if (rowIndex == firstRowView + 1) {
                            listView.smoothScrollToPosition(rowIndex - 1);
                        }
                        LinearLayout preRowView = (LinearLayout) listView.getChildAt((rowIndex - 1) - firstRowView);
                        if (preRowView == null) {
                            Log.e(TAG, "preRowView is not visible yet");
                            return true;
                        }
                        AppIconViewHolder[] preViewHolders = (AppIconViewHolder[]) preRowView.getTag();
                        if (preViewHolders[iconIndex].icon.getDrawable() == null || preViewHolders[iconIndex].container.getVisibility() != 0) {
                            lastVisibleIcon = 0;
                            while (lastVisibleIcon < maxRowItem) {
                                if (preViewHolders[lastVisibleIcon].icon.getDrawable() == null || preViewHolders[lastVisibleIcon].container.getVisibility() != 0) {
                                    lastVisibleIcon--;
                                    if (lastVisibleIcon == maxRowItem) {
                                        lastVisibleIcon--;
                                    }
                                    preViewHolders[lastVisibleIcon].container.requestFocus();
                                    preViewHolders[lastVisibleIcon].container.playSoundEffect(0);
                                } else {
                                    lastVisibleIcon++;
                                }
                            }
                            if (lastVisibleIcon == maxRowItem) {
                                lastVisibleIcon--;
                            }
                            preViewHolders[lastVisibleIcon].container.requestFocus();
                            preViewHolders[lastVisibleIcon].container.playSoundEffect(0);
                        } else {
                            preViewHolders[iconIndex].container.requestFocus();
                            preViewHolders[iconIndex].container.playSoundEffect(0);
                        }
                    } else {
                        viewHolders[iconIndex].container.clearFocus();
                        searchView.requestFocus();
                        return true;
                    }
                }
                return true;
            case 20:
                if (handleKeyEvent && rowIndex < listView.getCount() - 1) {
                    firstRowView = listView.getFirstVisiblePosition();
                    int lastRowView = listView.getLastVisiblePosition();
                    if (rowIndex > lastRowView - 1) {
                        listView.smoothScrollToPosition(rowIndex + 1);
                        return true;
                    }
                    if (rowIndex == lastRowView - 1) {
                        listView.smoothScrollToPosition(rowIndex + 1);
                    }
                    LinearLayout nextRowView = (LinearLayout) listView.getChildAt((rowIndex + 1) - firstRowView);
                    if (nextRowView == null) {
                        Log.e(TAG, "nextRowView is not visible yet");
                        return true;
                    }
                    AppIconViewHolder[] nextViewHolders = (AppIconViewHolder[]) nextRowView.getTag();
                    if (nextViewHolders[iconIndex].icon.getDrawable() == null || nextViewHolders[iconIndex].container.getVisibility() != 0) {
                        lastVisibleIcon = 0;
                        while (lastVisibleIcon < iconIndex) {
                            if (nextViewHolders[lastVisibleIcon].icon.getDrawable() == null || nextViewHolders[lastVisibleIcon].container.getVisibility() != 0) {
                                lastVisibleIcon--;
                                if (lastVisibleIcon == iconIndex) {
                                    lastVisibleIcon--;
                                }
                                nextViewHolders[lastVisibleIcon].container.requestFocus();
                                nextViewHolders[lastVisibleIcon].container.playSoundEffect(0);
                            } else {
                                lastVisibleIcon++;
                            }
                        }
                        if (lastVisibleIcon == iconIndex) {
                            lastVisibleIcon--;
                        }
                        nextViewHolders[lastVisibleIcon].container.requestFocus();
                        nextViewHolders[lastVisibleIcon].container.playSoundEffect(0);
                    } else {
                        nextViewHolders[iconIndex].container.requestFocus();
                        nextViewHolders[iconIndex].container.playSoundEffect(0);
                    }
                }
                return true;
            default:
                return false;
        }
    }

    public boolean onKey(View v, int keyCode, KeyEvent e) {
        return handleAppListIconKeyEvent(v, keyCode, e);
    }

    public boolean onKeyPressUp(View v, int keyCode, KeyEvent event) {
        return handleAppListIconKeyEvent(v, keyCode, event);
    }

    public boolean onKeyPressDown(View v, int keyCode, KeyEvent event) {
        return handleAppListIconKeyEvent(v, keyCode, event);
    }

    public boolean onKeyPressLeft(View v, int keyCode, KeyEvent event) {
        return handleAppListIconKeyEvent(v, keyCode, event);
    }

    public boolean onKeyPressRight(View v, int keyCode, KeyEvent event) {
        return handleAppListIconKeyEvent(v, keyCode, event);
    }

    public void onFocusIn(View v) {
    }

    public void onFocusOut(View v) {
    }
}
