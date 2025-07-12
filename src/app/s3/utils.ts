import CryptoJS from "crypto-js";
import { _Object } from "@aws-sdk/client-s3";
import { ENCRYPTION_KEY, STORAGE_KEYS, FILE_EXTENSIONS, FILE_ICONS } from "./constants";
import { SortConfig } from "./types";

/**
 * Utility functions for the S3 Explorer
 */

export interface StoredCredentials {
  accessKeyId: string;
  secretAccessKey: string;
}

/**
 * Encrypts and stores credentials in localStorage
 */
export const saveCredentialsToStorage = (
  credentials: StoredCredentials,
  bucketName: string,
  region: string
): void => {
  const encryptedCreds = CryptoJS.AES.encrypt(
    JSON.stringify(credentials),
    ENCRYPTION_KEY
  ).toString();
  
  localStorage.setItem(STORAGE_KEYS.credentials, encryptedCreds);
  localStorage.setItem(STORAGE_KEYS.bucketName, bucketName);
  localStorage.setItem(STORAGE_KEYS.region, region);
};

/**
 * Retrieves and decrypts credentials from localStorage
 */
export const loadCredentialsFromStorage = (): {
  credentials: StoredCredentials | null;
  bucketName: string | null;
  region: string | null;
} => {
  try {
    const savedCreds = localStorage.getItem(STORAGE_KEYS.credentials);
    const savedBucketName = localStorage.getItem(STORAGE_KEYS.bucketName);
    const savedRegion = localStorage.getItem(STORAGE_KEYS.region);

    if (savedCreds) {
      const bytes = CryptoJS.AES.decrypt(savedCreds, ENCRYPTION_KEY);
      const decryptedCreds = JSON.parse(bytes.toString(CryptoJS.enc.Utf8));
      
      return {
        credentials: decryptedCreds,
        bucketName: savedBucketName,
        region: savedRegion,
      };
    }
  } catch (error) {
    console.error("Failed to load credentials from localStorage:", error);
    clearStoredCredentials();
  }

  return {
    credentials: null,
    bucketName: null,
    region: null,
  };
};

/**
 * Clears all stored credentials from localStorage
 */
export const clearStoredCredentials = (): void => {
  localStorage.removeItem(STORAGE_KEYS.credentials);
  localStorage.removeItem(STORAGE_KEYS.bucketName);
  localStorage.removeItem(STORAGE_KEYS.region);
};

/**
 * Gets the appropriate file icon based on file extension
 */
export const getFileIcon = (fileName: string): string => {
  const extension = fileName.split(".").pop()?.toLowerCase();
  
  if (!extension) return FILE_ICONS.default;
  
  // Check each file type category
  for (const [type, extensions] of Object.entries(FILE_EXTENSIONS)) {
    if ((extensions as readonly string[]).includes(extension)) {
      return FILE_ICONS[type as keyof typeof FILE_ICONS] || FILE_ICONS.default;
    }
  }
  
  return FILE_ICONS.default;
};

/**
 * Sorts objects based on the provided sort configuration
 */
export const sortObjects = (
  objects: _Object[], 
  sortConfig: SortConfig | null
): _Object[] => {
  if (!sortConfig) return objects;

  return [...objects].sort((a, b) => {
    const aValue = a[sortConfig.key];
    const bValue = b[sortConfig.key];

    if (aValue == null && bValue == null) return 0;
    if (aValue == null) return 1;
    if (bValue == null) return -1;

    if (aValue < bValue) {
      return sortConfig.direction === "ascending" ? -1 : 1;
    }
    if (aValue > bValue) {
      return sortConfig.direction === "ascending" ? 1 : -1;
    }
    return 0;
  });
};

/**
 * Formats file size in human-readable format
 */
export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return "0 B";
  
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB", "TB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`;
};

/**
 * Formats storage usage from bytes to MB
 */
export const formatStorageUsage = (bytes: number): string => {
  return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
};

/**
 * Extracts folder name from S3 prefix
 */
export const extractFolderName = (prefix: string, currentPrefix: string): string => {
  return prefix.replace(currentPrefix, "").replace(/\/$/, "");
};

/**
 * Extracts file name from S3 key
 */
export const extractFileName = (key: string, currentPrefix: string): string => {
  return key.replace(currentPrefix, "").split("/").pop() || "Unnamed File";
};

/**
 * Validates if a folder name is valid for the current directory structure
 */
export const isValidFolder = (
  prefix: string, 
  currentPrefix: string, 
  folderName: string
): boolean => {
  return Boolean(
    folderName &&
    folderName.trim().length > 0 &&
    prefix !== currentPrefix &&
    !folderName.includes("/")
  );
};

/**
 * Validates if a file is valid (not a directory marker)
 */
export const isValidFile = (key: string): boolean => {
  const fileName = key.split("/").pop();
  return Boolean(!key.endsWith("/") && fileName && fileName.trim().length > 0);
};

/**
 * Gets parent prefix from current prefix
 */
export const getParentPrefix = (currentPrefix: string): string => {
  const trimmedPrefix = currentPrefix.endsWith("/")
    ? currentPrefix.slice(0, -1)
    : currentPrefix;
  const pathParts = trimmedPrefix.split("/");
  
  // Remove the last part (current folder) to get parent
  pathParts.pop();
  
  // Join back and add trailing slash if there are remaining parts
  return pathParts.length > 0 ? pathParts.join("/") + "/" : "";
};

/**
 * Validates AWS credentials format
 */
export const validateCredentials = (accessKeyId: string, secretAccessKey: string): boolean => {
  return (
    accessKeyId.trim().length > 0 &&
    secretAccessKey.trim().length > 0 &&
    accessKeyId.length >= 16 && // Minimum AWS access key ID length
    secretAccessKey.length >= 40 // Minimum AWS secret access key length
  );
};

/**
 * Validates S3 bucket name format
 */
export const validateBucketName = (bucketName: string): boolean => {
  const bucketRegex = /^[a-z0-9][a-z0-9\-]*[a-z0-9]$/;
  return (
    bucketName.length >= 3 &&
    bucketName.length <= 63 &&
    bucketRegex.test(bucketName) &&
    !bucketName.includes('..') &&
    !bucketName.startsWith('xn--') &&
    !bucketName.endsWith('-s3alias')
  );
};

/**
 * Filters items by search term
 */
export const filterBySearchTerm = <T extends { name?: string; key?: string }>(
  items: T[], 
  searchTerm: string,
  keyExtractor: (item: T) => string
): T[] => {
  if (!searchTerm.trim()) return items;
  
  const lowerSearchTerm = searchTerm.toLowerCase();
  return items.filter(item => 
    keyExtractor(item).toLowerCase().includes(lowerSearchTerm)
  );
};

/**
 * Truncates text to specified length
 */
export const truncateText = (text: string, maxLength: number): string => {
  if (text.length <= maxLength) return text;
  return `${text.substring(0, maxLength - 3)}...`;
};
