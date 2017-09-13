package com.yaoseetech.identity;

import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by xlongtang on 5/15/2017.
 */

public class DefaultGlobalR implements IGlobalR {
    private Resources mResources;
    private String mPackageName;
    private static Map<String, String> mMap = new HashMap<String, String>();;

    public DefaultGlobalR(@NonNull Activity activity) {
        this.mResources = activity.getResources();
        this.mPackageName = activity.getPackageName();
    }

    public int getRStringId(String name) {
        return mResources.getIdentifier(name, "string", mPackageName);
    }

    public int getRLayoutId(String name) {
        return mResources.getIdentifier(name, "layout", mPackageName);
    }

    public int getRDrawableId(String name) {
        return mResources.getIdentifier(name, "drawable", mPackageName);
    }

    public int getRId(String group, String name) {
        return mResources.getIdentifier(name, group, mPackageName);
    }

    public void setExtraString(String name, String value) {
        this.mMap.put(name, value);
    }

    public String getExtraString(String name) {
        return this.mMap.get(name);
    }
}
