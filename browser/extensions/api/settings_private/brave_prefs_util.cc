/* Copyright (c) 2019 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "brave/browser/extensions/api/settings_private/brave_prefs_util.h"

#include "brave/common/pref_names.h"
#include "brave/components/brave_wayback_machine/buildflags.h"
#include "brave/components/brave_rewards/common/pref_names.h"
#include "brave/components/ntp_background_images/common/pref_names.h"
#include "chrome/browser/extensions/api/settings_private/prefs_util.h"
#include "chrome/common/extensions/api/settings_private.h"
#include "components/browsing_data/core/pref_names.h"
#include "components/gcm_driver/gcm_buildflags.h"
#include "components/omnibox/browser/omnibox_prefs.h"

#if BUILDFLAG(ENABLE_BRAVE_WAYBACK_MACHINE)
#include "brave/components/brave_wayback_machine/pref_names.h"
#endif


namespace extensions {

using ntp_background_images::prefs::kNewTabPageSuperReferralThemesOption;
using
    ntp_background_images::prefs::kNewTabPageShowSponsoredImagesBackgroundImage;
using ntp_background_images::prefs::kNewTabPageShowBackgroundImage;

namespace settings_api = api::settings_private;

const PrefsUtil::TypedPrefMap& BravePrefsUtil::GetAllowlistedKeys() {
  // Static cache, similar to parent class
  static PrefsUtil::TypedPrefMap* s_brave_allowlist = nullptr;
  if (s_brave_allowlist)
    return *s_brave_allowlist;
  s_brave_allowlist = new PrefsUtil::TypedPrefMap();
  // Start with parent class whitelist
  const auto chromium_prefs = PrefsUtil::GetAllowlistedKeys();
  s_brave_allowlist->insert(chromium_prefs.begin(), chromium_prefs.end());
  // Add Brave values to the whitelist
  // import data
  (*s_brave_allowlist)[kImportDialogExtensions] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kImportDialogPayments] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  // Default Brave shields
  (*s_brave_allowlist)[kShieldsAdvancedViewEnabled] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kShieldsStatsBadgeVisible] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kAdControlType] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kHTTPSEVerywhereControlType] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kNoScriptControlType] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kGoogleLoginControlType] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kFBEmbedControlType] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kTwitterEmbedControlType] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kLinkedInEmbedControlType] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;

  // appearance prefs
  (*s_brave_allowlist)[kLocationBarIsWide] =
    settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kAutocompleteEnabled] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kTopSiteSuggestionsEnabled] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kBraveSuggestedSiteSuggestionsEnabled] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[brave_rewards::prefs::kHideButton] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kAskWidevineInstall] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kNewTabPageSuperReferralThemesOption] =
      settings_api::PrefType::PREF_TYPE_NUMBER;
  // new tab prefs
  (*s_brave_allowlist)[kNewTabPageShowSponsoredImagesBackgroundImage] =
        settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kNewTabPageShowBackgroundImage] =
        settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kNewTabPageShowClock] =
        settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kNewTabPageShowTopSites] =
        settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kNewTabPageShowStats] =
        settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kNewTabPageShowRewards] =
        settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kNewTabPageShowBinance] =
        settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kNewTabPageShowTogether] =
        settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kNewTabPageShowAddCard] =
        settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kNewTabPageShowGemini] =
        settings_api::PrefType::PREF_TYPE_BOOLEAN;
  // Clear browsing data on exit prefs.
  (*s_brave_allowlist)[browsing_data::prefs::kDeleteBrowsingHistoryOnExit] =
    settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[browsing_data::prefs::kDeleteDownloadHistoryOnExit] =
    settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[browsing_data::prefs::kDeleteCacheOnExit] =
    settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[browsing_data::prefs::kDeleteCookiesOnExit] =
    settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[browsing_data::prefs::kDeletePasswordsOnExit] =
    settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[browsing_data::prefs::kDeleteFormDataOnExit] =
    settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[browsing_data::prefs::kDeleteSiteSettingsOnExit] =
    settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[browsing_data::prefs::kDeleteHostedAppsDataOnExit] =
    settings_api::PrefType::PREF_TYPE_BOOLEAN;
  (*s_brave_allowlist)[kAlwaysShowBookmarkBarOnNTP] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  // WebTorrent pref
  (*s_brave_allowlist)[kWebTorrentEnabled] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
#if BUILDFLAG(ENABLE_BRAVE_WAYBACK_MACHINE)
  (*s_brave_allowlist)[kBraveWaybackMachineEnabled] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
#endif
  // Hangouts pref
  (*s_brave_allowlist)[kHangoutsEnabled] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  // IPFS Companion pref
  (*s_brave_allowlist)[kIPFSCompanionEnabled] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  // Brave Wallet pref
  (*s_brave_allowlist)[kBraveWalletWeb3Provider] =
      settings_api::PrefType::PREF_TYPE_NUMBER;
  (*s_brave_allowlist)[kLoadCryptoWalletsOnStartup] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
  // IPFS pref
  (*s_brave_allowlist)[kIPFSResolveMethod] =
      settings_api::PrefType::PREF_TYPE_NUMBER;
  // Media Router Pref
  (*s_brave_allowlist)[kBraveEnabledMediaRouter] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;

#if !BUILDFLAG(USE_GCM_FROM_PLATFORM)
  // Push Messaging Pref
  (*s_brave_allowlist)[kBraveGCMChannelStatus] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;
#endif
  // Omnibox pref
  (*s_brave_allowlist)[omnibox::kPreventUrlElisionsInOmnibox] =
      settings_api::PrefType::PREF_TYPE_BOOLEAN;

  return *s_brave_allowlist;
}

}  // namespace extensions
