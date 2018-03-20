package com.android.launcher3.folder.controller;

import android.content.ComponentName;
import android.widget.EditText;
import com.android.launcher3.LauncherFeature;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.base.view.PagedView;
import com.android.launcher3.common.view.IconView;
import com.android.launcher3.folder.FolderInfo;
import com.android.launcher3.folder.view.FolderIconView;
import com.android.launcher3.folder.view.FolderView;
import com.android.launcher3.proxy.FolderProxyCallbacks;
import com.samsung.android.sdk.bixby.data.ParamFilling;
import com.samsung.android.sdk.bixby.data.ScreenParameter;
import java.util.Iterator;

public class FolderProxyCallbacksImpl implements FolderProxyCallbacks {
    private FolderController mFolderController;

    FolderProxyCallbacksImpl(FolderController folderController) {
        this.mFolderController = folderController;
    }

    public IconView getItemViewByComponentName(ComponentName cn) {
        return getPagedView() != null ? getPagedView().findIconView(cn) : null;
    }

    public IconView getItemViewByTitle(String itemTitle) {
        if (getPagedView() == null) {
            return null;
        }
        Iterator it = getPagedView().findIconViews(itemTitle).iterator();
        while (it.hasNext()) {
            IconView v = (IconView) it.next();
            if (!(v instanceof FolderIconView)) {
                return v;
            }
        }
        return null;
    }

    public FolderIconView getFolderItemViewByTitle(String itemTitle) {
        return null;
    }

    public void selectItem(IconView iv) {
        if (LauncherFeature.supportMultiSelect()) {
            this.mFolderController.onCheckedChanged(iv, true);
        }
    }

    public void unSelectItem(IconView iv) {
        if (LauncherFeature.supportMultiSelect()) {
            this.mFolderController.onCheckedChanged(iv, false);
        }
    }

    public PagedView getPagedView() {
        if (this.mFolderController.getTargetFolderIconView() == null) {
            return null;
        }
        return this.mFolderController.getTargetFolderIconView().getFolderView().getContent();
    }

    public void movePage(int pageNum) {
        if (this.mFolderController.getTargetFolderIconView() != null) {
            this.mFolderController.getTargetFolderIconView().getFolderView().getContent().snapToPage(pageNum);
        }
    }

    public void changeBackgroundColor(int colorIndex) {
        this.mFolderController.getTargetFolderIconView().setIconBackgroundColor(colorIndex);
    }

    public void addFolderItem(ItemInfo item) {
        if (item instanceof IconInfo) {
            this.mFolderController.getTargetFolderIconView().addItem((IconInfo) item);
        }
    }

    public void removeFolderItem(ItemInfo item) {
        FolderView folder = this.mFolderController.getTargetFolderIconView().getFolderView();
        if (item != null && folder != null) {
            if (item instanceof IconInfo) {
                folder.getInfo().remove((IconInfo) item);
            }
            folder.getBaseController().deleteItemFromDb(item);
            folder.getBaseController().updateItemInDb(item);
        }
    }

    public FolderInfo getOpenedFolder() {
        if (this.mFolderController.getTargetFolderIconView() != null) {
            return this.mFolderController.getTargetFolderIconView().getFolderInfo();
        }
        return null;
    }

    public void openBackgroundColorView() {
        FolderView f = this.mFolderController.getTargetFolderIconView().getFolderView();
        if (f != null) {
            f.toggleColorPicker();
        }
    }

    public void changeTitle(String title) {
        this.mFolderController.getTargetFolderIconView().onTitleChanged(title);
        FolderView f = this.mFolderController.getTargetFolderIconView().getFolderView();
        ((EditText) f.getEditTextRegion()).setText(title);
        f.dismissEditingName();
    }

    public FolderIconView getOpenedFolderIconView() {
        return this.mFolderController.getTargetFolderIconView();
    }

    public void movePageToItem(ItemInfo item) {
    }

    public boolean onParamFillingReceived(ParamFilling pf) {
        if (!pf.getScreenParamMap().containsKey("Text")) {
            return false;
        }
        changeTitle(((ScreenParameter) pf.getScreenParamMap().get("Text")).getSlotValue());
        return true;
    }
}
