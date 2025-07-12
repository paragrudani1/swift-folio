"use client";

import { S3Client, _Object, CommonPrefix } from "@aws-sdk/client-s3";
import { Breadcrumb } from "./Breadcrumb";
import { UploadArea } from "./UploadArea";
import { FileExplorer } from "./FileExplorer";

interface MainInterfaceProps {
  s3Client: S3Client;
  selectedBucket: string;
  currentPrefix: string;
  objects: _Object[];
  prefixes: CommonPrefix[];
  filteredObjects: _Object[];
  filteredPrefixes: Array<CommonPrefix & { folderName: string }>;
  isSelectMode: boolean;
  selectedItems: Set<string>;
  searchTerm: string;
  uploadProgress: number | null;
  currentlyUploadingFolder: string | null;
  onSearchChange: (value: string) => void;
  onBackClick: () => void;
  onCreateFolderClick: () => void;
  onUploadSuccess: () => void;
  onUploadProgress: (progress: number, folder?: string) => void;
  onItemSelection: (key: string) => void;
  onPrefixClick: (prefix: string) => void;
  onRequestSort: (key: keyof _Object) => void;
  onToggleSelectMode: () => void;
  onSelectAll: () => void;
  uploadFiles: (
    s3Client: S3Client,
    bucketName: string,
    prefix: string,
    files: File[],
    onProgress?: (progress: number) => void
  ) => Promise<{ cancelled: boolean }>;
  cancelUpload: () => void;
  isUploading?: boolean;
  onUploadCancelled?: () => void;
  onClearSelection: () => void;
  onBulkDelete: () => void;
  onDownload: (key: string) => void;
  onDelete: (key: string) => void;
}

export function MainInterface({
  s3Client,
  selectedBucket,
  currentPrefix,
  objects,
  prefixes,
  filteredObjects,
  filteredPrefixes,
  isSelectMode,
  selectedItems,
  searchTerm,
  uploadProgress,
  currentlyUploadingFolder,
  onSearchChange,
  onBackClick,
  onCreateFolderClick,
  onUploadSuccess,
  onUploadProgress,
  onItemSelection,
  onPrefixClick,
  onRequestSort,
  onToggleSelectMode,
  onSelectAll,
  onClearSelection,
  onBulkDelete,
  onDownload,
  onDelete,
  uploadFiles,
  cancelUpload,
  isUploading = false,
  onUploadCancelled,
}: MainInterfaceProps) {
  return (
    <div className="space-y-6">
      {/* Breadcrumb and Actions */}
      <Breadcrumb
        selectedBucket={selectedBucket}
        currentPrefix={currentPrefix}
        searchTerm={searchTerm}
        onSearchChange={onSearchChange}
        onBackClick={onBackClick}
        onCreateFolderClick={onCreateFolderClick}
      />

      {/* Upload Area */}
      <UploadArea
        s3Client={s3Client}
        selectedBucket={selectedBucket}
        currentPrefix={currentPrefix}
        uploadProgress={uploadProgress}
        currentlyUploadingFolder={currentlyUploadingFolder}
        onUploadSuccess={onUploadSuccess}
        onUploadProgress={onUploadProgress}
        uploadFiles={uploadFiles}
        cancelUpload={cancelUpload}
        isUploading={isUploading}
        onUploadCancelled={onUploadCancelled}
      />

      {/* File Explorer */}
      <FileExplorer
        objects={objects}
        prefixes={prefixes}
        filteredObjects={filteredObjects}
        filteredPrefixes={filteredPrefixes}
        isSelectMode={isSelectMode}
        selectedItems={selectedItems}
        currentPrefix={currentPrefix}
        onItemSelection={onItemSelection}
        onPrefixClick={onPrefixClick}
        onRequestSort={onRequestSort}
        onToggleSelectMode={onToggleSelectMode}
        onSelectAll={onSelectAll}
        onClearSelection={onClearSelection}
        onBulkDelete={onBulkDelete}
        onDownload={onDownload}
        onDelete={onDelete}
      />
    </div>
  );
}
