/* Copyright (c) 2020 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.widget.crypto.binance;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONException;

import org.chromium.base.Log;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.QRCodeShareDialogFragment;
import org.chromium.chrome.browser.widget.crypto.binance.BinanceAccountBalance;
import org.chromium.chrome.browser.widget.crypto.binance.BinanceCoinNetworks;
import org.chromium.chrome.browser.widget.crypto.binance.BinanceNativeWorker;
import org.chromium.chrome.browser.widget.crypto.binance.BinanceObserver;
import org.chromium.chrome.browser.widget.crypto.binance.BinanceWidgetManager;
import org.chromium.chrome.browser.widget.crypto.binance.CoinNetworkModel;
import org.chromium.ui.widget.Toast;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class BinanceDepositFragment extends Fragment {
    private BinanceNativeWorker mBinanceNativeWorker;

    private CoinNetworkModel selectedCoinNetworkModel;
    private LinearLayout depositCoinListLayout;
    private NestedScrollView currentNestedScrollView;

    public BinanceDepositFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinanceNativeWorker = BinanceNativeWorker.getInstance();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mBinanceNativeWorker.AddObserver(mBinanaceObserver);
        return inflater.inflate(R.layout.fragment_binance_deposit, container, false);
    }

    @Override
    public void onDestroyView() {
        mBinanceNativeWorker.RemoveObserver(mBinanaceObserver);
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        depositCoinListLayout = view.findViewById(R.id.deposit_layout);
        mBinanceNativeWorker.getCoinNetworks();
    }

    private BinanceDepositAdapter.BinanceDepositListener binanceDepositListener =
            new BinanceDepositAdapter.BinanceDepositListener() {
                @Override
                public void onItemClick(CoinNetworkModel coinNetworkModel) {
                    selectedCoinNetworkModel = coinNetworkModel;
                    mBinanceNativeWorker.getDepositInfo(
                            coinNetworkModel.getCoin(), coinNetworkModel.getTickerNetwork());
                }
            };

    private BinanceObserver mBinanaceObserver = new BinanceObserver() {
        @Override
        public void OnGetAccessToken(boolean isSuccess){};

        @Override
        public void OnGetAccountBalances(String jsonBalances, boolean isSuccess){};

        @Override
        public void OnGetConvertQuote(
                String quoteId, String quotePrice, String totalFee, String totalAmount){};

        @Override
        public void OnGetCoinNetworks(String jsonNetworks) {
            try {
                BinanceCoinNetworks binanceCoinNetworks = new BinanceCoinNetworks(jsonNetworks);
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                for (CoinNetworkModel coinNetworkModel :
                        binanceCoinNetworks.getCoinNetworksList()) {
                    final View view = inflater.inflate(R.layout.binance_deposit_item, null);

                    ImageView currencyImageView = view.findViewById(R.id.currency_image);
                    TextView currencyText = view.findViewById(R.id.currency_text);

                    currencyText.setText(coinNetworkModel.getCoin()
                            + (TextUtils.isEmpty(coinNetworkModel.getCoinDesc())
                                            ? ""
                                            : " (" + coinNetworkModel.getCoinDesc() + ")"));
                    currencyImageView.setImageResource(coinNetworkModel.getCoinRes());
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            selectedCoinNetworkModel = coinNetworkModel;
                            mBinanceNativeWorker.getDepositInfo(coinNetworkModel.getCoin(),
                                    coinNetworkModel.getTickerNetwork());
                        }
                    });
                    if (depositCoinListLayout != null) {
                        depositCoinListLayout.addView(view);
                    }
                }
            } catch (JSONException e) {
                Log.e("NTP", e.getMessage());
            }
        };

        @Override
        public void OnGetDepositInfo(String depositAddress, String depositTag, boolean isSuccess) {
            if (isSuccess && selectedCoinNetworkModel != null && getView() != null) {
                FrameLayout depositLayout =
                        (FrameLayout) getView().findViewById(R.id.binance_deposit_layout);
                ImageView depositBack = depositLayout.findViewById(R.id.currency_back);
                TextView currencyTitleText = depositLayout.findViewById(R.id.currency_text);
                ImageView deposiIcon = depositLayout.findViewById(R.id.currency_image);
                ImageView deposiQrIcon = depositLayout.findViewById(R.id.currency_qr_image);
                TextView currencyAddressText =
                        depositLayout.findViewById(R.id.currency_address_text);
                TextView currencyMemoText = depositLayout.findViewById(R.id.currency_memo_text);
                TextView currencyAddressValueText =
                        depositLayout.findViewById(R.id.currency_address_value_text);
                TextView currencyMemoValueText =
                        depositLayout.findViewById(R.id.currency_memo_value_text);
                Button btnCopyAddress = depositLayout.findViewById(R.id.btn_copy_address);
                Button btnCopyMemo = depositLayout.findViewById(R.id.btn_copy_memo);

                deposiQrIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        QRCodeShareDialogFragment mQRCodeShareDialogFragment =
                                new QRCodeShareDialogFragment();
                        mQRCodeShareDialogFragment.setQRCodeText(depositAddress);
                        mQRCodeShareDialogFragment.show(getActivity().getSupportFragmentManager(),
                                "QRCodeShareDialogFragment");
                    }
                });

                if (selectedCoinNetworkModel != null) {
                    currencyTitleText.setText(selectedCoinNetworkModel.getCoin()
                            + (TextUtils.isEmpty(selectedCoinNetworkModel.getCoinDesc())
                                            ? ""
                                            : " (" + selectedCoinNetworkModel.getCoinDesc() + ")"));
                    currencyAddressText.setText(
                            String.format(getResources().getString(R.string.currency_address_text),
                                    selectedCoinNetworkModel.getCoin()));
                    currencyMemoText.setText(
                            String.format(getResources().getString(R.string.currency_memo_text),
                                    selectedCoinNetworkModel.getCoin()));
                    deposiIcon.setImageResource(selectedCoinNetworkModel.getCoinRes());
                }

                depositBack.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (depositCoinListLayout != null) {
                            depositCoinListLayout.setVisibility(View.VISIBLE);
                            depositLayout.setVisibility(View.GONE);
                            currencyMemoText.setVisibility(View.GONE);
                            currencyMemoValueText.setVisibility(View.GONE);
                            btnCopyMemo.setVisibility(View.GONE);
                        }
                        selectedCoinNetworkModel = null;
                    }
                });
                depositLayout.setVisibility(View.VISIBLE);
                if (depositCoinListLayout != null) {
                    depositCoinListLayout.setVisibility(View.GONE);
                }

                currencyAddressValueText.setText(depositAddress);
                if (!TextUtils.isEmpty(depositTag)) {
                    currencyMemoText.setVisibility(View.VISIBLE);
                    currencyMemoValueText.setVisibility(View.VISIBLE);
                    btnCopyMemo.setVisibility(View.VISIBLE);
                    currencyMemoValueText.setText(depositTag);
                }

                btnCopyAddress.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (selectedCoinNetworkModel != null) {
                            copyTextToClipboard(
                                    String.format(getResources().getString(
                                                          R.string.currency_address_text),
                                            selectedCoinNetworkModel.getCoin()),
                                    depositAddress);
                        }
                    }
                });

                btnCopyMemo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (selectedCoinNetworkModel != null) {
                            copyTextToClipboard(String.format(getResources().getString(
                                                                      R.string.currency_memo_text),
                                                        selectedCoinNetworkModel.getCoin()),
                                    depositTag);
                        }
                    }
                });
            }
        };

        @Override
        public void OnConfirmConvert(boolean isSuccess, String message){};

        @Override
        public void OnGetConvertAssets(String jsonAssets){};

        @Override
        public void OnRevokeToken(boolean isSuccess){};
    };

    private void copyTextToClipboard(String title, String textToCopy) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard =
                    (android.text.ClipboardManager) getActivity().getSystemService(
                            Context.CLIPBOARD_SERVICE);
            clipboard.setText(textToCopy);
        } else {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) getActivity().getSystemService(
                            Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip =
                    android.content.ClipData.newPlainText(title, textToCopy);
            clipboard.setPrimaryClip(clip);
        }
        Toast.makeText(getActivity(), R.string.text_has_been_copied, Toast.LENGTH_LONG).show();
    }
}