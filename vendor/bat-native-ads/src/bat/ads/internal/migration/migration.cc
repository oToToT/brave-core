/* Copyright (c) 2020 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "bat/ads/internal/migration.h"

#include "bat/ads/internal/ads_impl.h"
#include "bat/ads/internal/migration/migration_version.h"
#include "bat/ads/internal/logging.h"

namespace ads {
namespace migration {

Migration::Migration(
    AdsImpl* ads)
    : ads_(ads) {
  DCHECK(ads_);
}

Migration::~Migration() = default;

void Migration::FromVersion(
    const int from_version,
    ResultCallback callback) {
  const int to_version = version();
  if (to_version == from_version) {
    callback(Result::SUCCESS);
    return;
  }

  for (int i = from_version + 1; i <= to_version; i++) {
    ToVersion(i);
  }

  BLOG(1, "Migrated from version " << from_version << " to version "
      << to_version);

  callback(Result::SUCCESS);
}

void Migration::ToVersion(
    const int to_version) {
  switch (to_version) {
    case 2: {
      MigrateState();
      break;
    }

    default: {
      break;
    }
  }
}

}  // namespace migration
}  // namespace ads
