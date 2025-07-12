"use client";

import { useEffect } from "react";
import { useTheme } from "../contexts/ThemeContext";

// Components
import {
  Header,
  LoginForm,
  MainInterface,
  CreateFolderModal,
  DeleteModal,
  BulkDeleteModal,
} from "./components";

// Hooks
import { useS3Explorer } from "./hooks/useS3Explorer";
import { useS3Operations } from "./hooks/useS3Operations";

// Utils
import { 
  sortObjects,
  extractFolderName,
  extractFileName,
  isValidFolder,
  isValidFile,
} from "./utils";

export default function S3Explorer() {
  const { token } = useTheme();
  
  // Custom hooks for state management
  const {
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
    
    // Setters
    setBucketNameInput,
    setRegionInput,
    setSaveCredentials,
    setObjects,
    setPrefixes,
    setStorageUsage,
    setLoadingStorageUsage,
    setError,
    setNewFolderName,
    setSearchTerm,
    
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
  } = useS3Explorer();

  // Custom hooks for S3 operations
  const {
    error: s3Error,
    listObjects,
    downloadFile,
    deleteFile,
    deleteMultipleFiles,
    deleteFolder,
    createFolder,
    getBucketStorageUsage,
  } = useS3Operations();

  // Sync errors between hooks
  useEffect(() => {
    if (s3Error) {
      setError(s3Error);
    }
  }, [s3Error, setError]);

  // Load bucket data when S3 client and bucket are ready
  useEffect(() => {
    if (s3Client && selectedBucket) {
      const loadBucketData = async () => {
        try {
          const { objects: newObjects, prefixes: newPrefixes } = await listObjects(
            s3Client, 
            selectedBucket, 
            currentPrefix
          );
          setObjects(newObjects);
          setPrefixes(newPrefixes);

          // Load storage usage
          setLoadingStorageUsage(true);
          const usage = await getBucketStorageUsage(s3Client, selectedBucket);
          setStorageUsage(usage);
        } catch (err) {
          console.error("Failed to load bucket data:", err);
        } finally {
          setLoadingStorageUsage(false);
        }
      };

      loadBucketData();
    }
  }, [
    s3Client,
    selectedBucket,
    currentPrefix,
    listObjects,
    getBucketStorageUsage,
    setObjects,
    setPrefixes,
    setStorageUsage,
    setLoadingStorageUsage,
  ]);

  // Handle file download
  const handleDownload = async (key: string) => {
    if (!s3Client || !selectedBucket) return;
    
    try {
      await downloadFile(s3Client, selectedBucket, key);
    } catch (err) {
      console.error("Download failed:", err);
    }
  };

  // Handle file deletion
  const handleConfirmDelete = async () => {
    if (!s3Client || !selectedBucket || !itemToDelete) return;

    try {
      // Check if it's a folder (ends with /)
      if (itemToDelete.endsWith('/')) {
        await deleteFolder(s3Client, selectedBucket, itemToDelete);
      } else {
        await deleteFile(s3Client, selectedBucket, itemToDelete);
      }
      
      // Refresh the list
      const { objects: newObjects, prefixes: newPrefixes } = await listObjects(
        s3Client, 
        selectedBucket, 
        currentPrefix
      );
      setObjects(newObjects);
      setPrefixes(newPrefixes);
      
      // Clear selection state if the deleted item was selected
      if (selectedItems.has(itemToDelete)) {
        const newSelection = new Set(selectedItems);
        newSelection.delete(itemToDelete);
        clearSelection();
      }
      
      // If no items remain selected, exit select mode
      if (selectedItems.size <= 1 && selectedItems.has(itemToDelete)) {
        toggleSelectMode();
      }
      
      closeDeleteModal();
    } catch (err) {
      console.error("Delete failed:", err);
    }
  };

  // Handle bulk deletion
  const handleConfirmBulkDelete = async () => {
    if (!s3Client || !selectedBucket || selectedItems.size === 0) return;

    try {
      const itemsToDelete = Array.from(selectedItems);
      
      // Separate files and folders
      const files = itemsToDelete.filter(item => !item.endsWith('/'));
      const folders = itemsToDelete.filter(item => item.endsWith('/'));
      
      // Delete files
      if (files.length > 0) {
        await deleteMultipleFiles(s3Client, selectedBucket, files);
      }
      
      // Delete folders one by one
      for (const folder of folders) {
        await deleteFolder(s3Client, selectedBucket, folder);
      }
      
      // Refresh the list
      const { objects: newObjects, prefixes: newPrefixes } = await listObjects(
        s3Client, 
        selectedBucket, 
        currentPrefix
      );
      setObjects(newObjects);
      setPrefixes(newPrefixes);
      
      // Clear all selection state
      clearSelection();
      
      // Exit select mode since all selected items are deleted
      if (isSelectMode) {
        toggleSelectMode();
      }
      
      closeBulkDeleteModal();
    } catch (err) {
      console.error("Bulk delete failed:", err);
    }
  };

  // Handle folder creation
  const handleConfirmCreateFolder = async () => {
    if (!s3Client || !selectedBucket || !newFolderName.trim()) return;

    try {
      await createFolder(s3Client, selectedBucket, newFolderName, currentPrefix);
      
      // Refresh the list
      const { objects: newObjects, prefixes: newPrefixes } = await listObjects(
        s3Client, 
        selectedBucket, 
        currentPrefix
      );
      setObjects(newObjects);
      setPrefixes(newPrefixes);
      
      closeCreateFolderModal();
    } catch (err) {
      console.error("Create folder failed:", err);
    }
  };

  // Sort objects
  const sortedObjects = sortObjects(objects, sortConfig);

  // Filter objects and prefixes by search term
  const filteredObjects = sortedObjects.filter(obj => {
    if (!searchTerm.trim()) return true;
    const fileName = extractFileName(obj.Key || "", currentPrefix);
    return fileName.toLowerCase().includes(searchTerm.toLowerCase());
  });

  const filteredPrefixes = prefixes.filter(prefix => {
    if (!searchTerm.trim()) return true;
    const folderName = extractFolderName(prefix.Prefix || "", currentPrefix);
    return folderName.toLowerCase().includes(searchTerm.toLowerCase());
  });

  // Add filtered data with folder names
  const filteredPrefixesWithNames = filteredPrefixes
    .map((prefix) => {
      const folderName = extractFolderName(prefix.Prefix || "", currentPrefix);
      return { ...prefix, folderName };
    })
    .filter((item) => isValidFolder(item.Prefix || "", currentPrefix, item.folderName));

  // Main render
  return (
    <div className="min-h-screen" style={{ backgroundColor: token('color', 'primaryBg') }}>
      {/* Header */}
      <Header
        selectedBucket={selectedBucket}
        storageUsage={storageUsage}
        loadingStorageUsage={loadingStorageUsage}
        onLogout={handleLogout}
      />

      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-6 sm:py-8">
        {!s3Client ? (
          /* Login Form */
          <LoginForm
            credentials={credentials}
            bucketNameInput={bucketNameInput}
            setBucketNameInput={setBucketNameInput}
            regionInput={regionInput}
            setRegionInput={setRegionInput}
            saveCredentials={saveCredentials}
            setSaveCredentials={setSaveCredentials}
            error={error}
            onCredentialChange={handleCredentialChange}
            onLogin={handleLogin}
          />
        ) : selectedBucket ? (
          /* Main Explorer Interface */
          <MainInterface
            s3Client={s3Client}
            selectedBucket={selectedBucket}
            currentPrefix={currentPrefix}
            objects={objects}
            prefixes={prefixes}
            filteredObjects={filteredObjects.filter((o) => isValidFile(o.Key || ""))}
            filteredPrefixes={filteredPrefixesWithNames}
            isSelectMode={isSelectMode}
            selectedItems={selectedItems}
            searchTerm={searchTerm}
            uploadProgress={uploadProgress}
            currentlyUploadingFolder={currentlyUploadingFolder}
            onSearchChange={setSearchTerm}
            onBackClick={handleBackClick}
            onCreateFolderClick={handleCreateFolderClick}
            onUploadSuccess={handleUploadSuccess}
            onUploadProgress={handleUploadProgress}
            onItemSelection={toggleItemSelection}
            onPrefixClick={handlePrefixClick}
            onRequestSort={requestSort}
            onToggleSelectMode={toggleSelectMode}
            onSelectAll={selectAllItems}
            onClearSelection={clearSelection}
            onBulkDelete={handleBulkDelete}
            onDownload={handleDownload}
            onDelete={handleDelete}
          />
        ) : (
          <div className="text-center py-12">
            <p style={{ color: token('color', 'mutedText') }}>
              Please enter your credentials and select a bucket to begin.
            </p>
          </div>
        )}
      </div>

      {/* Modals */}
      <CreateFolderModal
        isOpen={isCreateFolderModalOpen}
        newFolderName={newFolderName}
        onFolderNameChange={setNewFolderName}
        onConfirm={handleConfirmCreateFolder}
        onCancel={closeCreateFolderModal}
      />

      <DeleteModal
        isOpen={isDeleteModalOpen}
        itemToDelete={itemToDelete}
        onConfirm={handleConfirmDelete}
        onCancel={closeDeleteModal}
      />

      <BulkDeleteModal
        isOpen={isBulkDeleteModalOpen}
        selectedItemsCount={selectedItems.size}
        onConfirm={handleConfirmBulkDelete}
        onCancel={closeBulkDeleteModal}
      />
    </div>
  );
}
