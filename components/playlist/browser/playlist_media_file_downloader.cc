/* Copyright (c) 2020 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

#include "brave/components/playlist/browser/playlist_media_file_downloader.h"

#include <algorithm>
#include <utility>

#include "base/bind.h"
#include "base/files/file.h"
#include "base/files/file_util.h"
#include "base/json/json_reader.h"
#include "base/json/json_writer.h"
#include "base/strings/string_number_conversions.h"
#include "base/strings/utf_string_conversions.h"
#include "base/task/post_task.h"
#include "base/task_runner_util.h"
#include "brave/components/playlist/browser/playlist_constants.h"
#include "brave/components/playlist/browser/playlist_types.h"
#include "content/public/browser/browser_context.h"
#include "content/public/browser/storage_partition.h"
#include "services/network/public/cpp/shared_url_loader_factory.h"
#include "services/network/public/cpp/simple_url_loader.h"
#include "url/gurl.h"

namespace playlist {
namespace {

constexpr unsigned int kRetriesCountOnNetworkChange = 1;

net::NetworkTrafficAnnotationTag GetNetworkTrafficAnnotationTagForURLLoad() {
  return net::DefineNetworkTrafficAnnotation("playlist_service", R"(
      semantics {
        sender: "Brave Playlist Service"
        description:
          "Fetching media file for newly created playlist"
        trigger:
          "User-initiated for creating new playlist "
        data:
          "media file for playlist"
        destination: WEBSITE
      }
      policy {
        cookies_allowed: NO
      })");
}

base::FilePath::StringType GetFileNameStringFromIndex(int index) {
#if defined(OS_WIN)
  return base::NumberToString16(index);
#else
  return base::NumberToString(index);
#endif
}

size_t WriteToFile(base::File* file, base::StringPiece data) {
  size_t bytes_written = 0;

  if (file->IsValid()) {
    if (!data.empty())
      bytes_written +=
          std::max(0, file->WriteAtCurrentPos(data.data(), data.size()));
  }

  return bytes_written;
}

// Return false when source file is not appended properly.
bool AppendToFileThenDelete(const base::FilePath& source_path,
                            base::File* destination_file,
                            char* read_buffer,
                            size_t read_buffer_size) {
  base::ScopedFILE source_file(base::OpenFile(source_path, "rb"));
  if (!source_file)
    return false;

  // This size will be used to rollback current source file appending.
  const int64_t dest_file_size = destination_file->GetLength();

  // Read |source_path|'s contents in chunks of read_buffer_size and append
  // to |destination_file|.
  size_t num_bytes_read;
  bool got_error = false;
  while ((num_bytes_read =
              fread(read_buffer, 1, read_buffer_size, source_file.get())) > 0) {
    if (num_bytes_read !=
        WriteToFile(destination_file,
                    base::StringPiece(read_buffer, num_bytes_read))) {
      // Exclude source file if an error occurs during the appending by
      // resetting dest file's previous file length.
      destination_file->SetLength(dest_file_size);
      VLOG(2) << __func__ << " file is excluded: " << source_path;
      got_error = true;
      break;
    }
  }

  // Now that it has been copied, delete the source file.
  source_file.reset();
  base::DeleteFile(source_path);
  return !got_error;
}

// Return -1 if failed.
// Return 0 if all source files are used for generation.
// Return 1 if some of source files are skipped for generation.
int DoGenerateSingleMediaFileOnIOThread(
    const base::FilePath& playlist_dir_path,
    const base::FilePath::StringType& source_media_files_dir,
    const base::FilePath::StringType& unified_media_file_name,
    int num_source_files) {
  const base::FilePath source_files_dir =
      playlist_dir_path.Append(base::FilePath::StringType(
          source_media_files_dir.begin(), source_media_files_dir.end()));

  const base::FilePath unified_media_file_path =
      playlist_dir_path.Append(base::FilePath::StringType(
          unified_media_file_name.begin(), unified_media_file_name.end()));

  base::DeleteFile(unified_media_file_path);

  base::File unified_media_file(
      unified_media_file_path,
      base::File::FLAG_CREATE | base::File::FLAG_WRITE);
  if (!unified_media_file.IsValid())
    return -1;

  const size_t kReadBufferSize = 1 << 16;  // 64KiB
  std::unique_ptr<char[]> read_buffer(new char[kReadBufferSize]);
  bool has_skipped_source_files = false;
  for (int i = 0; i < num_source_files; ++i) {
    const base::FilePath media_file_source_path =
        source_files_dir.Append(GetFileNameStringFromIndex(i));
    if (!base::PathExists(media_file_source_path)) {
      VLOG(2) << __func__
              << " file not existed: " << media_file_source_path.value();
      has_skipped_source_files = true;
      continue;
    }

    if (!AppendToFileThenDelete(media_file_source_path, &unified_media_file,
                                read_buffer.get(), kReadBufferSize)) {
      has_skipped_source_files = true;
    }
  }
  DCHECK(base::PathExists(unified_media_file_path));

  // Delete empty source files dir.
  base::DeletePathRecursively(source_files_dir);

  if (unified_media_file.GetLength() == 0)
    return -1;

  return has_skipped_source_files ? 1 : 0;
}

}  // namespace

base::FilePath::StringType GetPlaylistIDDirName(
    const std::string& playlist_id) {
#if defined(OS_WIN)
  return base::UTF8ToUTF16(playlist_id);
#else
  return playlist_id;
#endif
}

PlaylistMediaFileDownloader::PlaylistMediaFileDownloader(
    Client* client,
    content::BrowserContext* context,
    base::FilePath::StringType source_media_files_dir,
    base::FilePath::StringType unified_media_file_name,
    std::string media_file_path_key,
    std::string create_params_path_key)
    : client_(client),
      url_loader_factory_(
          content::BrowserContext::GetDefaultStoragePartition(context)
              ->GetURLLoaderFactoryForBrowserProcess()),
      source_media_files_dir_(source_media_files_dir),
      unified_media_file_name_(unified_media_file_name),
      media_file_path_key_(media_file_path_key),
      create_params_path_key_(create_params_path_key),
      weak_factory_(this) {}

PlaylistMediaFileDownloader::~PlaylistMediaFileDownloader() {}

void PlaylistMediaFileDownloader::NotifyFail() {
  ResetStatus();
  client_->OnMediaFileGenerationFailed(std::move(current_playlist_));
}

void PlaylistMediaFileDownloader::NotifySucceed(bool partial) {
  ResetStatus();
  client_->OnMediaFileReady(std::move(current_playlist_), partial);
}

void PlaylistMediaFileDownloader::GenerateSingleMediaFile(
    base::Value&& playlist_value,
    const base::FilePath& base_dir) {
  DCHECK(!in_progress_);

  in_progress_ = true;
  current_playlist_ = std::move(playlist_value);
  current_playlist_id_ = *current_playlist_.FindStringKey(kPlaylistIDKey);
  remained_download_files_ = GetNumberOfMediaFileSources();
  media_file_source_files_count_ = remained_download_files_;
  if (media_file_source_files_count_ == 0) {
    VLOG(2) << __func__ << ": Empty media file source list";
    NotifySucceed(false);
  }

  playlist_dir_path_ =
      base_dir.Append(GetPlaylistIDDirName(current_playlist_id_));

  // Create PROFILE_DIR/playlist/ID/source_files dir to store each media files.
  // Then, downloads them in that directory.
  CreateSourceFilesDirThenDownloads();
}

void PlaylistMediaFileDownloader::CreateSourceFilesDirThenDownloads() {
  const base::FilePath source_files_dir =
      playlist_dir_path_.Append(source_media_files_dir_);
  base::PostTaskAndReplyWithResult(
      io_task_runner(), FROM_HERE,
      base::BindOnce(&base::CreateDirectory, source_files_dir),
      base::BindOnce(&PlaylistMediaFileDownloader::OnSourceFilesDirCreated,
                     weak_factory_.GetWeakPtr()));
}

void PlaylistMediaFileDownloader::OnSourceFilesDirCreated(bool success) {
  if (!success) {
    NotifyFail();
    return;
  }

  DownloadAllMediaFileSources();
}

int PlaylistMediaFileDownloader::GetNumberOfMediaFileSources() {
  DCHECK(in_progress_);
  base::Value* media_files =
      current_playlist_.FindPath(create_params_path_key_);
  return media_files->GetList().size();
}

void PlaylistMediaFileDownloader::DownloadAllMediaFileSources() {
  base::Value* media_files =
      current_playlist_.FindPath(create_params_path_key_);
  for (int i = 0; i < media_file_source_files_count_; ++i) {
    const std::string* url =
        media_files->GetList()[i].FindStringKey(kPlaylistMediaFileUrlKey);
    if (url) {
      DownloadMediaFile(GURL(*url), i);
    } else {
      NOTREACHED() << "Playlist has empty media file url";
      NotifyFail();
      break;
    }
  }
}

void PlaylistMediaFileDownloader::DownloadMediaFile(const GURL& url,
                                                     int index) {
  VLOG(2) << __func__ << ": " << url.spec() << " at: " << index;

  auto request = std::make_unique<network::ResourceRequest>();
  request->url = GURL(url);
  request->credentials_mode = network::mojom::CredentialsMode::kOmit;
  auto loader = network::SimpleURLLoader::Create(
      std::move(request), GetNetworkTrafficAnnotationTagForURLLoad());
  loader->SetRetryOptions(
      kRetriesCountOnNetworkChange,
      network::SimpleURLLoader::RetryMode::RETRY_ON_NETWORK_CHANGE);

  auto iter = url_loaders_.insert(url_loaders_.begin(), std::move(loader));

  const base::FilePath file_path =
      playlist_dir_path_.Append(source_media_files_dir_)
          .Append(GetFileNameStringFromIndex(index));
  iter->get()->DownloadToFile(
      url_loader_factory_.get(),
      base::BindOnce(&PlaylistMediaFileDownloader::OnMediaFileDownloaded,
                     base::Unretained(this), std::move(iter), index),
      file_path);
}

void PlaylistMediaFileDownloader::OnMediaFileDownloaded(
    SimpleURLLoaderList::iterator iter,
    int index,
    base::FilePath path) {
  // When cancelled, we don't need to more for current job.
  if (cancelled_)
    return;

  url_loaders_.erase(iter);

  if (path.empty()) {
    // This fail is handled during the generation.
    // See |has_skipped_source_files| in DoGenerateSingleMediaFileOnIOThread().
    // |has_skipped_source_files| will be set to true.
    VLOG(1) << __func__ << ": failed to download media file at " << index;
  }

  remained_download_files_--;

  // If all source files are downloaded, unify them into one media file.
  if (IsDownloadFinished())
    StartSingleMediaFileGeneration();
}

void PlaylistMediaFileDownloader::RequestCancelCurrentPlaylistGeneration() {
  cancelled_ = true;
  url_loaders_.clear();
}

void PlaylistMediaFileDownloader::StartSingleMediaFileGeneration() {
  base::PostTaskAndReplyWithResult(
      io_task_runner(), FROM_HERE,
      base::BindOnce(&DoGenerateSingleMediaFileOnIOThread, playlist_dir_path_,
                     source_media_files_dir_, unified_media_file_name_,
                     media_file_source_files_count_),
      base::BindOnce(&PlaylistMediaFileDownloader::OnSingleMediaFileGenerated,
                     weak_factory_.GetWeakPtr()));
}

void PlaylistMediaFileDownloader::OnSingleMediaFileGenerated(int result) {
  if (cancelled_) {
    ResetStatus();
    return;
  }

  if (result != -1) {
    base::FilePath media_file_path =
        playlist_dir_path_.Append(unified_media_file_name_);
    const std::string media_file_path_utf8 =
#if defined(OS_WIN)
        base::UTF16ToUTF8(media_file_path.value());
#else
        media_file_path.value();
#endif
    const bool partial_ready = result == 1;
    current_playlist_.SetStringKey(media_file_path_key_, media_file_path_utf8);
    current_playlist_.SetBoolKey(kPlaylistPartialReadyKey, partial_ready);
    NotifySucceed(partial_ready);
  } else {
    current_playlist_.SetStringKey(media_file_path_key_, "");
    current_playlist_.SetBoolKey(kPlaylistPartialReadyKey, false);
    NotifyFail();
  }
}

base::SequencedTaskRunner* PlaylistMediaFileDownloader::io_task_runner() {
  if (!io_task_runner_) {
    io_task_runner_ = base::CreateSequencedTaskRunner(
        {base::ThreadPool(), base::MayBlock(), base::TaskPriority::BEST_EFFORT,
         base::TaskShutdownBehavior::SKIP_ON_SHUTDOWN});
  }
  return io_task_runner_.get();
}

void PlaylistMediaFileDownloader::ResetStatus() {
  in_progress_ = false;
  cancelled_ = false;
  current_playlist_id_.clear();
  url_loaders_.clear();
}

bool PlaylistMediaFileDownloader::IsDownloadFinished() {
  return remained_download_files_ == 0;
}

}  // namespace playlist
