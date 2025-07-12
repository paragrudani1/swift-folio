import { useState, useEffect, useCallback } from "react";
import { S3Client, _Object, CommonPrefix } from "@aws-sdk/client-s3";
import { Credentials, SortConfig } from "../types";
import { DEFAULT_REGION } from "../constants";
import { 
  saveCredentialsToStorage, 
  loadCredentialsFromStorage, 
  clearStoredCredentials,
  getParentPrefix,
} from "../utils";

/**
 * Custom hook for S3 Explorer authentication and state management
 */
export const useS3Explorer = () => {
  // Authentication state
  const [credentials, setCredentials] = useState<Credentials>({
    accessKeyId: "",
    secretAccessKey: "",
  });
  const [bucketNameInput, setBucketNameInput] = useState("");
  const [regionInput, setRegionInput] = useState(DEFAULT_REGION);
  const [saveCredentials, setSaveCredentials] = useState(false);
  const [s3Client, setS3Client] = useState<S3Client | null>(null);
  const [selectedBucket, setSelectedBucket] = useState<string | null>(null);

  // File management state
  const [objects, setObjects] = useState<_Object[]>([]);
  const [prefixes, setPrefixes] = useState<CommonPrefix[]>([]);
  const [currentPrefix, setCurrentPrefix] = useState<string>("");
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [currentlyUploadingFolder, setCurrentlyUploadingFolder] = useState<string | null>(null);

  // UI state
  const [sortConfig, setSortConfig] = useState<SortConfig | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [storageUsage, setStorageUsage] = useState<number | null>(null);
  const [loadingStorageUsage, setLoadingStorageUsage] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Modal state
  const [isCreateFolderModalOpen, setIsCreateFolderModalOpen] = useState(false);
  const [newFolderName, setNewFolderName] = useState("");
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<string | null>(null);
  const [isBulkDeleteModalOpen, setIsBulkDeleteModalOpen] = useState(false);

  // Selection state
  const [selectedItems, setSelectedItems] = useState<Set<string>>(new Set());
  const [isSelectMode, setIsSelectMode] = useState(false);

  /**
   * Auto-login on component mount
   */
  useEffect(() => {
    const autoLogin = async () => {
      try {
        const { credentials: savedCreds, bucketName, region } = loadCredentialsFromStorage();

        if (savedCreds && savedCreds.accessKeyId && savedCreds.secretAccessKey) {
          setCredentials(savedCreds);
          setSaveCredentials(true);
          
          if (bucketName) {
            setBucketNameInput(bucketName);
          }
          if (region) {
            setRegionInput(region);
          }

          // Create S3 client with saved credentials
          const client = new S3Client({
            region: region || DEFAULT_REGION,
            credentials: {
              accessKeyId: savedCreds.accessKeyId,
              secretAccessKey: savedCreds.secretAccessKey,
            },
          });

          setS3Client(client);

          if (bucketName) {
            setSelectedBucket(bucketName);
          }
        }
      } catch (error) {
        console.error("autoLogin: Failed to load credentials from localStorage:", error);
        clearStoredCredentials();
      }
    };

    autoLogin();
  }, []);

  /**
   * Handle credential input changes
   */
  const handleCredentialChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setCredentials((prev) => ({ ...prev, [name]: value }));
  }, []);

  /**
   * Handle login
   */
  const handleLogin = useCallback(() => {
    if (saveCredentials) {
      saveCredentialsToStorage(credentials, bucketNameInput, regionInput);
    } else {
      clearStoredCredentials();
    }

    const client = new S3Client({
      region: regionInput,
      credentials,
    });
    setS3Client(client);

    if (!bucketNameInput) {
      setError("Please enter a bucket name.");
      setS3Client(null);
      return;
    }

    setSelectedBucket(bucketNameInput);
    setError(null);
  }, [credentials, bucketNameInput, regionInput, saveCredentials]);

  /**
   * Handle logout
   */
  const handleLogout = useCallback(() => {
    setS3Client(null);
    setSelectedBucket(null);
    setCredentials({ accessKeyId: "", secretAccessKey: "" });
    setObjects([]);
    setPrefixes([]);
    setCurrentPrefix("");
    setError(null);
    setStorageUsage(null);
    clearStoredCredentials();
  }, []);

  /**
   * Handle prefix (folder) navigation
   */
  const handlePrefixClick = useCallback((prefix: string) => {
    setCurrentPrefix(prefix);
  }, []);

  /**
   * Handle back navigation
   */
  const handleBackClick = useCallback(() => {
    const parentPrefix = getParentPrefix(currentPrefix);
    setCurrentPrefix(parentPrefix);
  }, [currentPrefix]);

  /**
   * Handle upload success
   */
  const handleUploadSuccess = useCallback(() => {
    setUploadProgress(null);
    setCurrentlyUploadingFolder(null);
  }, []);

  /**
   * Handle upload progress
   */
  const handleUploadProgress = useCallback((progress: number) => {
    setUploadProgress(progress);
  }, []);

  /**
   * Toggle select mode
   */
  const toggleSelectMode = useCallback(() => {
    setIsSelectMode(!isSelectMode);
    setSelectedItems(new Set());
  }, [isSelectMode]);

  /**
   * Toggle item selection
   */
  const toggleItemSelection = useCallback((key: string) => {
    const newSelectedItems = new Set(selectedItems);
    if (newSelectedItems.has(key)) {
      newSelectedItems.delete(key);
    } else {
      newSelectedItems.add(key);
    }
    setSelectedItems(newSelectedItems);
  }, [selectedItems]);

  /**
   * Select all items
   */
  const selectAllItems = useCallback(() => {
    const allKeys = new Set([
      ...objects.map(obj => obj.Key!),
      ...prefixes.map(prefix => prefix.Prefix!)
    ]);
    setSelectedItems(allKeys);
  }, [objects, prefixes]);

  /**
   * Clear selection
   */
  const clearSelection = useCallback(() => {
    setSelectedItems(new Set());
  }, []);

  /**
   * Request sort
   */
  const requestSort = useCallback((key: keyof _Object) => {
    let direction: 'ascending' | 'descending' = "ascending";
    if (
      sortConfig &&
      sortConfig.key === key &&
      sortConfig.direction === "ascending"
    ) {
      direction = "descending";
    }
    setSortConfig({ key, direction });
  }, [sortConfig]);

  /**
   * Handle create folder modal
   */
  const handleCreateFolderClick = useCallback(() => {
    setIsCreateFolderModalOpen(true);
  }, []);

  const closeCreateFolderModal = useCallback(() => {
    setIsCreateFolderModalOpen(false);
    setNewFolderName("");
  }, []);

  /**
   * Handle delete modal
   */
  const handleDelete = useCallback((key: string) => {
    setItemToDelete(key);
    setIsDeleteModalOpen(true);
  }, []);

  const closeDeleteModal = useCallback(() => {
    setIsDeleteModalOpen(false);
    setItemToDelete(null);
  }, []);

  /**
   * Handle bulk delete modal
   */
  const handleBulkDelete = useCallback(() => {
    if (selectedItems.size === 0) return;
    setIsBulkDeleteModalOpen(true);
  }, [selectedItems]);

  const closeBulkDeleteModal = useCallback(() => {
    setIsBulkDeleteModalOpen(false);
  }, []);

  return {
    // Authentication state
    credentials,
    bucketNameInput,
    regionInput,
    saveCredentials,
    s3Client,
    selectedBucket,

    // File management state
    objects,
    prefixes,
    currentPrefix,
    uploadProgress,
    currentlyUploadingFolder,

    // UI state
    sortConfig,
    searchTerm,
    storageUsage,
    loadingStorageUsage,
    error,

    // Modal state
    isCreateFolderModalOpen,
    newFolderName,
    isDeleteModalOpen,
    itemToDelete,
    isBulkDeleteModalOpen,

    // Selection state
    selectedItems,
    isSelectMode,

    // Setters for direct state updates
    setCredentials,
    setBucketNameInput,
    setRegionInput,
    setSaveCredentials,
    setS3Client,
    setSelectedBucket,
    setObjects,
    setPrefixes,
    setCurrentPrefix,
    setUploadProgress,
    setCurrentlyUploadingFolder,
    setSortConfig,
    setSearchTerm,
    setStorageUsage,
    setLoadingStorageUsage,
    setError,
    setIsCreateFolderModalOpen,
    setNewFolderName,
    setIsDeleteModalOpen,
    setItemToDelete,
    setIsBulkDeleteModalOpen,
    setSelectedItems,
    setIsSelectMode,

    // Action handlers
    handleCredentialChange,
    handleLogin,
    handleLogout,
    handlePrefixClick,
    handleBackClick,
    handleUploadSuccess,
    handleUploadProgress,
    toggleSelectMode,
    toggleItemSelection,
    selectAllItems,
    clearSelection,
    requestSort,
    handleCreateFolderClick,
    closeCreateFolderModal,
    handleDelete,
    closeDeleteModal,
    handleBulkDelete,
    closeBulkDeleteModal,
  };
};
