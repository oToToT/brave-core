/* Copyright (c) 2020 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.ntp.widget;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.ntp.widget.NTPWidgetAdapter;
import org.chromium.chrome.browser.ntp.widget.NTPWidgetManager;
import org.chromium.chrome.browser.ntp.widget.SwipeAndDragHelper;

import java.util.List;

public class NTPWidgetListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements SwipeAndDragHelper.ActionCompletionContract {
    private List<NTPWidgetItem> widgetList;
    private ItemTouchHelper touchHelper;

    private NTPWidgetAdapter.NTPWidgetListener ntpWidgetListener;

    public void setNTPWidgetListener(NTPWidgetAdapter.NTPWidgetListener ntpWidgetListener) {
        this.ntpWidgetListener = ntpWidgetListener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.ntp_widget_list_item_layout, parent, false);
        return new NTPWidgetViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        NTPWidgetViewHolder ntpWidgetViewHolder = (NTPWidgetViewHolder) holder;
        ntpWidgetViewHolder.widgetTitle.setText(widgetList.get(position).getWidgetTitle());
        ntpWidgetViewHolder.widgetText.setText(widgetList.get(position).getWidgetText());
        ntpWidgetViewHolder.widgetReorderView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    touchHelper.startDrag(holder);
                }
                return false;
            }
        });
    }

    @Override
    public int getItemCount() {
        return widgetList == null ? 0 : widgetList.size();
    }

    public void setWidgetList(List<NTPWidgetItem> widgetList) {
        this.widgetList = widgetList;
        notifyDataSetChanged();
    }

    public List<NTPWidgetItem> getWidgetList() {
        return this.widgetList;
    }

    @Override
    public void onViewMoved(int oldPosition, int newPosition) {
        NTPWidgetItem targetNTPWidgetItem = widgetList.get(oldPosition);
        NTPWidgetItem ntpWidgetItem = new NTPWidgetItem(targetNTPWidgetItem);
        widgetList.remove(oldPosition);
        widgetList.add(newPosition, ntpWidgetItem);
        notifyItemMoved(oldPosition, newPosition);
    }

    @Override
    public void onViewSwiped(int position) {
        NTPWidgetItem ntpWidgetItem = widgetList.get(position);
        NTPWidgetManager.getInstance().setWidget(ntpWidgetItem.getWidgetType(), -1);
        widgetList.remove(position);
        notifyItemRemoved(position);
    }

    void setTouchHelper(ItemTouchHelper touchHelper) {
        this.touchHelper = touchHelper;
    }

    private class NTPWidgetViewHolder extends RecyclerView.ViewHolder {
        TextView widgetTitle;
        TextView widgetText;
        ImageView widgetReorderView;

        NTPWidgetViewHolder(View itemView) {
            super(itemView);

            widgetTitle = itemView.findViewById(R.id.widget_title);
            widgetText = itemView.findViewById(R.id.widget_text);
            widgetReorderView = itemView.findViewById(R.id.widget_reorder);
        }
    }
}
