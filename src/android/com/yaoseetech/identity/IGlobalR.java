package com.yaoseetech.identity;

/**
 * Created by xlongtang on 5/15/2017.
 */

public interface IGlobalR {
    int getRStringId(String name);
    int getRLayoutId(String name);
    int getRDrawableId(String name);
    int getRId(String group, String name);

    String getExtraString(String name);
    void setExtraString(String name, String value);
}
