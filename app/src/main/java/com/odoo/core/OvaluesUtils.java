package com.odoo.core;

import android.text.TextUtils;
import android.util.Log;

import com.odoo.core.orm.OValues;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by fanani on 12/26/17.
 */

public class OvaluesUtils {

    public static final String TAG = OvaluesUtils.class.getSimpleName();

    public static String getValue(OValues values, String key, int index, String default_value){
        String results = "";
        try {
            if (!values.getString(key).equals("false")) {
                List<Object> parent_id = (ArrayList<Object>) values.get(key);
                results = parent_id.get(index).toString();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        if (TextUtils.isEmpty(results) && !TextUtils.isEmpty(default_value)){
            return default_value;
        } else {
            return results;
        }
    }

    public static JSONArray getJSONArray(OValues values, String key){
        JSONArray results = null;
        try {
            results = new JSONArray(values.getString(key));
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        return results;
    }

}
