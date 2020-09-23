/* Copyright (c) 2020 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#ifndef BAT_ADS_INTERNAL_MIGRATION_MIGRATION_H_
#define BAT_ADS_INTERNAL_MIGRATION_MIGRATION_H_

#include "bat/ads/ads_client.h"

namespace ads {

class AdsImpl;

namespace migration {

class Migration {
 public:
  explicit Migration(
      AdsImpl* ads);

  ~Migration();

  void FromVersion(
      const int from_version,
      ResultCallback callback);

 private:
  void ToVersion(
      const int to_version);

  AdsImpl* ads_;  // NOT OWNED
};

}  // namespace migration
}  // namespace ads

#endif  // BAT_ADS_INTERNAL_MIGRATION_MIGRATION_H_
