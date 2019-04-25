package com.zhanyage.bindview.core.template;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.view.View;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by andya on 2019-04-25
 * Describe: bind view center
 */
public class BindService {

    private static final String TAG = "ButterKnife";

    private static final Map<String, BindTemplate> classCache = new LinkedHashMap<>();

    private BindService() {
        throw new AssertionError("No instances.");
    }

    /**
     * bind for Activity
     * @param target activity
     */
    @NonNull
    @UiThread
    public static final void bind(Activity target) {
        View sourceView = target.getWindow().getDecorView();
        bind(target, sourceView);
    }


    /**
     * bind for Adapter
     * @param target ViewHolder
     * @param source item root view
     */
    @NonNull
    @UiThread
    public static final void bind(Object target, View source) {
        String className = target.getClass().getName();
        BindTemplate bindTemplateService = classCache.get(className);
        try {
            if (null == bindTemplateService) {
                bindTemplateService = (BindTemplate) Class.forName(className + "$$bindView$$BindView").newInstance();
                bindTemplateService.bindTargetView(target, source);
                classCache.put(className, bindTemplateService);
            } else {
                bindTemplateService.bindTargetView(target, source);
            }
        } catch (NullPointerException e) {
            throw new RuntimeException("Unable find class" + className + "$$bindView$$BindView", e);
        } catch (InstantiationException e) {

        } catch (ClassNotFoundException e) {

        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to invoke newInstance" + className + "$$bindView$$BindView", e);
        }
    }

}
