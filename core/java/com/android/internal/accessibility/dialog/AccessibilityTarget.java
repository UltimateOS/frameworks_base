/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.accessibility.dialog;

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;
import static com.android.internal.accessibility.util.ShortcutUtils.optInValueToSettings;
import static com.android.internal.accessibility.util.ShortcutUtils.optOutValueFromSettings;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.internal.accessibility.common.ShortcutConstants.AccessibilityFragmentType;
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType;
import com.android.internal.accessibility.dialog.TargetAdapter.ViewHolder;
import com.android.internal.annotations.VisibleForTesting;

/**
 * Abstract base class for creating various target related to accessibility service, accessibility
 * activity, and allowlisting features.
 *
 * <p> Disables accessibility features that are not permitted in adding a restricted padlock icon
 * and showing admin support message dialog.
 */
public abstract class AccessibilityTarget implements TargetOperations, OnTargetSelectedListener,
        OnTargetCheckedChangeListener {
    private Context mContext;
    @UserShortcutType
    private int mShortcutType;
    @AccessibilityFragmentType
    private int mFragmentType;
    private boolean mShortcutEnabled;
    private String mId;
    private int mUid;
    private ComponentName mComponentName;
    private CharSequence mLabel;
    private Drawable mIcon;
    private String mKey;
    private CharSequence mStateDescription;

    @VisibleForTesting
    public AccessibilityTarget(Context context, @UserShortcutType int shortcutType,
            @AccessibilityFragmentType int fragmentType, boolean isShortcutSwitched, String id,
            int uid, CharSequence label, Drawable icon, String key) {
        mContext = context;
        mShortcutType = shortcutType;
        mFragmentType = fragmentType;
        mShortcutEnabled = isShortcutSwitched;
        mId = id;
        mUid = uid;
        mComponentName = ComponentName.unflattenFromString(id);
        mLabel = label;
        mIcon = icon;
        mKey = key;
    }

    @Override
    public void updateActionItem(@NonNull ViewHolder holder,
            @ShortcutConstants.ShortcutMenuMode int shortcutMenuMode) {
        // Resetting the enable state of the item to avoid the previous wrong state of RecyclerView.
        holder.mCheckBoxView.setEnabled(true);
        holder.mIconView.setEnabled(true);
        holder.mLabelView.setEnabled(true);
        holder.mStatusView.setEnabled(true);

        final boolean isEditMenuMode =
                shortcutMenuMode == ShortcutConstants.ShortcutMenuMode.EDIT;
        holder.mCheckBoxView.setChecked(isEditMenuMode && isShortcutEnabled());
        holder.mCheckBoxView.setVisibility(isEditMenuMode ? View.VISIBLE : View.GONE);
        holder.mIconView.setImageDrawable(getIcon());
        holder.mLabelView.setText(getLabel());
        holder.mStatusView.setVisibility(View.GONE);
    }

    @Override
    public void onSelected() {
        final AccessibilityManager am =
                getContext().getSystemService(AccessibilityManager.class);
        switch (getShortcutType()) {
            case SOFTWARE:
                am.notifyAccessibilityButtonClicked(getContext().getDisplayId(), getId());
                return;
            case HARDWARE:
                am.performAccessibilityShortcut(getId());
                return;
            default:
                throw new IllegalStateException("Unexpected shortcut type");
        }
    }

    @Override
    public void onCheckedChanged(boolean isChecked) {
        setShortcutEnabled(isChecked);
        if (isChecked) {
            optInValueToSettings(getContext(), getShortcutType(), getId());
        } else {
            optOutValueFromSettings(getContext(), getShortcutType(), getId());
        }
    }

    public void setStateDescription(CharSequence stateDescription) {
        mStateDescription = stateDescription;
    }

    /**
     * Gets the state description of this feature target.
     *
     * @return the state description
     */
    @Nullable
    public CharSequence getStateDescription() {
        return mStateDescription;
    }

    public void setShortcutEnabled(boolean enabled) {
        mShortcutEnabled = enabled;
    }

    public Context getContext() {
        return mContext;
    }

    public @UserShortcutType int getShortcutType() {
        return mShortcutType;
    }

    public @AccessibilityFragmentType int getFragmentType() {
        return mFragmentType;
    }

    public boolean isShortcutEnabled() {
        return mShortcutEnabled;
    }

    public String getId() {
        return mId;
    }

    public int getUid() {
        return mUid;
    }

    public ComponentName getComponentName() {
        return mComponentName;
    }

    public CharSequence getLabel() {
        return mLabel;
    }

    public Drawable getIcon() {
        return mIcon;
    }

    public String getKey() {
        return mKey;
    }
}
