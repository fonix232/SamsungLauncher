package com.android.launcher3.home.logging;

import android.os.Process;
import android.text.TextUtils;
import com.android.launcher3.common.base.item.IconInfo;
import com.android.launcher3.common.base.item.ItemInfo;
import com.android.launcher3.common.model.nano.LauncherDumpProto.ContainerType;
import com.android.launcher3.common.model.nano.LauncherDumpProto.DumpTarget;
import com.android.launcher3.common.model.nano.LauncherDumpProto.ItemType;
import com.android.launcher3.home.LauncherAppWidgetInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DumpTargetWrapper {
    private ArrayList<DumpTargetWrapper> children;
    private DumpTarget node;

    private DumpTargetWrapper() {
        this.children = new ArrayList();
    }

    public DumpTargetWrapper(int containerType, int id) {
        this();
        this.node = newContainerTarget(containerType, id);
    }

    public DumpTargetWrapper(ItemInfo info) {
        this();
        this.node = newItemTarget(info);
    }

    public DumpTargetWrapper(ItemInfo info, int pageIndex) {
        this(info);
        this.node.pageId = pageIndex;
    }

    public void add(DumpTargetWrapper child) {
        this.children.add(child);
    }

    public List<DumpTarget> getFlattenedList() {
        ArrayList<DumpTarget> list = new ArrayList();
        list.add(this.node);
        if (!this.children.isEmpty()) {
            Iterator it = this.children.iterator();
            while (it.hasNext()) {
                list.addAll(((DumpTargetWrapper) it.next()).getFlattenedList());
            }
            list.add(this.node);
        }
        return list;
    }

    private DumpTarget newItemTarget(ItemInfo info) {
        DumpTarget dt = new DumpTarget();
        dt.type = 1;
        switch (info.itemType) {
            case 0:
                dt.itemType = 1;
                break;
            case 1:
                dt.itemType = 0;
                break;
            case 4:
                dt.itemType = 2;
                break;
            case 6:
                dt.itemType = 3;
                break;
        }
        return dt;
    }

    private DumpTarget newContainerTarget(int type, int id) {
        DumpTarget dt = new DumpTarget();
        dt.type = 2;
        dt.containerType = type;
        dt.pageId = id;
        return dt;
    }

    public static String getDumpTargetStr(DumpTarget t) {
        if (t == null) {
            return "";
        }
        switch (t.type) {
            case 1:
                return getItemStr(t);
            case 2:
                String str = LoggerUtils.getFieldName(t.containerType, ContainerType.class);
                if (t.containerType == 1) {
                    return str + " id=" + t.pageId;
                }
                if (t.containerType == 3) {
                    return str + " grid(" + t.gridX + "," + t.gridY + ")";
                }
                return str;
            default:
                return "UNKNOWN TARGET TYPE";
        }
    }

    private static String getItemStr(DumpTarget t) {
        String typeStr = LoggerUtils.getFieldName(t.itemType, ItemType.class);
        if (!TextUtils.isEmpty(t.packageName)) {
            typeStr = typeStr + ", package=" + t.packageName;
        }
        if (!TextUtils.isEmpty(t.component)) {
            typeStr = typeStr + ", component=" + t.component;
        }
        return typeStr + ", grid(" + t.gridX + "," + t.gridY + "), span(" + t.spanX + "," + t.spanY + "), pageIdx=" + t.pageId + " user=" + t.userType;
    }

    public DumpTarget writeToDumpTarget(ItemInfo info) {
        DumpTarget dumpTarget;
        if (info instanceof IconInfo) {
            String str;
            IconInfo iconInfo = (IconInfo) info;
            dumpTarget = this.node;
            if (iconInfo.getTargetComponent() == null) {
                str = "";
            } else {
                str = iconInfo.getTargetComponent().flattenToString();
            }
            dumpTarget.component = str;
            dumpTarget = this.node;
            if (iconInfo.getTargetComponent() == null) {
                str = "";
            } else {
                str = iconInfo.getTargetComponent().getPackageName();
            }
            dumpTarget.packageName = str;
        } else if (info instanceof LauncherAppWidgetInfo) {
            this.node.component = ((LauncherAppWidgetInfo) info).providerName.flattenToString();
            this.node.packageName = ((LauncherAppWidgetInfo) info).providerName.getPackageName();
        }
        this.node.gridX = info.cellX;
        this.node.gridY = info.cellY;
        this.node.spanX = info.spanX;
        this.node.spanY = info.spanY;
        dumpTarget = this.node;
        int i = (info.user.getUser() == null || info.user.getUser().equals(Process.myUserHandle())) ? 0 : 1;
        dumpTarget.userType = i;
        return this.node;
    }
}
