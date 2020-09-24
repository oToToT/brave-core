/* Copyright (c) 2020 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.ntp.widget;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.ntp.widget.NTPWidgetAdapter;
import org.chromium.chrome.browser.ntp.widget.NTPWidgetListAdapter;
import org.chromium.chrome.browser.ntp.widget.NTPWidgetManager;

import java.util.ArrayList;
import java.util.List;

public class NTPWidgetBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private NTPWidgetAdapter.NTPWidgetListener ntpWidgetListener;
    private NTPWidgetListAdapter ntpWidgetListAdapter;
    private List<NTPWidgetItem> widgetList = new ArrayList<NTPWidgetItem>();

    public void setNTPWidgetListener(NTPWidgetAdapter.NTPWidgetListener ntpWidgetListener) {
        this.ntpWidgetListener = ntpWidgetListener;
    }

    public static NTPWidgetBottomSheetDialogFragment newInstance() {
        return new NTPWidgetBottomSheetDialogFragment();
    }

    public void setWidgetList(List<NTPWidgetItem> widgetList) {
        this.widgetList = widgetList;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ntp_widget_bottom_sheet_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView usedWidgetsRecyclerView = view.findViewById(R.id.used_widget_list);
        usedWidgetsRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        ntpWidgetListAdapter = new NTPWidgetListAdapter();
        SwipeAndDragHelper swipeAndDragHelper = new SwipeAndDragHelper(ntpWidgetListAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(swipeAndDragHelper);
        ntpWidgetListAdapter.setTouchHelper(touchHelper);
        ntpWidgetListAdapter.setNTPWidgetListener(ntpWidgetListener);
        usedWidgetsRecyclerView.setAdapter(ntpWidgetListAdapter);
        touchHelper.attachToRecyclerView(usedWidgetsRecyclerView);
        ntpWidgetListAdapter.setWidgetList(widgetList);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        for (int i = 0; i < ntpWidgetListAdapter.getWidgetList().size(); i++) {
            NTPWidgetManager.getInstance().setWidget(
                    ntpWidgetListAdapter.getWidgetList().get(i).getWidgetType(), i);
        }
        ntpWidgetListener.onBottomSheetDismiss();
    }
}
