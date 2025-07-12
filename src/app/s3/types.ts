import { S3Client, _Object, CommonPrefix } from "@aws-sdk/client-s3";

export interface Credentials {
  accessKeyId: string;
  secretAccessKey: string;
}

export interface SortConfig {
  key: keyof _Object;
  direction: 'ascending' | 'descending';
}

export interface S3ExplorerState {
  error: string | null;
  credentials: Credentials;
  bucketNameInput: string;
  regionInput: string;
  saveCredentials: boolean;
  s3Client: S3Client | null;
  selectedBucket: string | null;
  objects: _Object[];
  prefixes: CommonPrefix[];
  currentPrefix: string;
  uploadProgress: number | null;
  sortConfig: SortConfig | null;
  searchTerm: string;
  storageUsage: number | null;
  loadingStorageUsage: boolean;
  isCreateFolderModalOpen: boolean;
  newFolderName: string;
  isDeleteModalOpen: boolean;
  itemToDelete: string | null;
  currentlyUploadingFolder: string | null;
  selectedItems: Set<string>;
  isSelectMode: boolean;
  isBulkDeleteModalOpen: boolean;
}

export interface FileIconMap {
  [key: string]: string;
}

export interface StorageUsageInfo {
  size: number;
  loading: boolean;
}
