"use client";

import { useState, useEffect, useCallback } from "react";
import {
  S3Client,
  ListObjectsV2Command,
  _Object,
  CommonPrefix,
  GetObjectCommand,
  DeleteObjectCommand,
  PutObjectCommand,
} from "@aws-sdk/client-s3";
import CryptoJS from "crypto-js";
import { Dropzone } from "./Dropzone";

const ENCRYPTION_KEY = "your-super-secret-key"; // In a real app, manage this securely!

export default function S3Explorer() {
  const [error, setError] = useState<string | null>(null);
  const [credentials, setCredentials] = useState({
    accessKeyId: "",
    secretAccessKey: "",
  });
  const [bucketNameInput, setBucketNameInput] = useState("");
  const [regionInput, setRegionInput] = useState("us-east-1"); // Default region
  const [saveCredentials, setSaveCredentials] = useState(false);
  const [s3Client, setS3Client] = useState<S3Client | null>(null);
  const [selectedBucket, setSelectedBucket] = useState<string | null>(null);
  const [objects, setObjects] = useState<_Object[]>([]);
  const [prefixes, setPrefixes] = useState<CommonPrefix[]>([]);
  const [currentPrefix, setCurrentPrefix] = useState<string>("");
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [sortConfig, setSortConfig] = useState<{
    key: keyof _Object;
    direction: string;
  } | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [storageUsage, setStorageUsage] = useState<number | null>(null);
  const [loadingStorageUsage, setLoadingStorageUsage] = useState(false);

  const handleLogout = () => {
    setS3Client(null);
    setSelectedBucket(null);
    setCredentials({ accessKeyId: "", secretAccessKey: "" });
    setObjects([]);
    setPrefixes([]);
    setCurrentPrefix("");
    setError(null);
    setStorageUsage(null);
    
    // Clear credentials from localStorage
    localStorage.removeItem("aws_credentials");
    localStorage.removeItem("aws_bucket_name");
    localStorage.removeItem("aws_region");
  };

  const sortedObjects = [...objects].sort((a, b) => {
    if (!sortConfig) return 0;
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

  const requestSort = (key: keyof _Object) => {
    let direction = "ascending";
    if (
      sortConfig &&
      sortConfig.key === key &&
      sortConfig.direction === "ascending"
    ) {
      direction = "descending";
    }
    setSortConfig({ key, direction });
  };

  const getFileIcon = (key: string) => {
    const extension = key.split(".").pop()?.toLowerCase();
    switch (extension) {
      case "jpg":
      case "jpeg":
      case "png":
      case "gif":
      case "svg":
        return "&#128444; "; // Image icon
      case "pdf":
        return "&#128443; "; // PDF icon
      case "txt":
      case "md":
        return "&#128441; "; // Text file icon
      default:
        return "&#128440; "; // Generic file icon
    }
  };

  useEffect(() => {
    const autoLogin = async () => {
      try {
        const savedCreds = localStorage.getItem("aws_credentials");
        const savedBucketName = localStorage.getItem("aws_bucket_name");
        const savedRegion = localStorage.getItem("aws_region");

        if (savedCreds) {
          const bytes = CryptoJS.AES.decrypt(savedCreds, ENCRYPTION_KEY);
          const decryptedCreds = JSON.parse(bytes.toString(CryptoJS.enc.Utf8));
          if (decryptedCreds.accessKeyId && decryptedCreds.secretAccessKey) {
            setCredentials(decryptedCreds);
            setSaveCredentials(true);
            if (savedBucketName) {
              setBucketNameInput(savedBucketName);
            }
            if (savedRegion) {
              setRegionInput(savedRegion);
            }

            // Attempt to auto-login with saved credentials
            const client = new S3Client({
              region: savedRegion || "us-east-1", // Use saved region or default
              credentials: {accessKeyId: decryptedCreds.accessKeyId, secretAccessKey: decryptedCreds.secretAccessKey},
            });
            setS3Client(client);

            // Skip credential validation during auto-login to avoid CORS issues
            // Credentials will be validated when actual S3 operations are performed
            if (savedBucketName) {
              setSelectedBucket(savedBucketName);
            }
          }
        }
      } catch (error) {
        console.error("autoLogin: Failed to load credentials from localStorage:", error);
        // Only clear localStorage if there was an attempt to load credentials that failed
        const savedCreds = localStorage.getItem("aws_credentials");
        if (savedCreds) {
          localStorage.removeItem("aws_credentials");
          localStorage.removeItem("aws_bucket_name");
          localStorage.removeItem("aws_region");
        }
      }
    };

    autoLogin();
  }, []);

  const handleCredentialChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setCredentials((prev) => ({ ...prev, [name]: value }));
  };

  const handleLogin = () => {
    if (saveCredentials) {
      const encryptedCreds = CryptoJS.AES.encrypt(
        JSON.stringify(credentials),
        ENCRYPTION_KEY
      ).toString();
      localStorage.setItem("aws_credentials", encryptedCreds);
      localStorage.setItem("aws_bucket_name", bucketNameInput);
      localStorage.setItem("aws_region", regionInput);
    } else {
      localStorage.removeItem("aws_credentials");
      localStorage.removeItem("aws_bucket_name");
      localStorage.removeItem("aws_region");
    }

    const client = new S3Client({
      region: regionInput, // Use region from input
      credentials,
    });
    setS3Client(client);

    if (!bucketNameInput) {
      setError("Please enter a bucket name.");
      setS3Client(null); // Reset S3 client if no bucket name
      return;
    }

    setSelectedBucket(bucketNameInput);
  };

  const getBucketStorageUsage = useCallback(async (bucketName: string) => {
    if (!s3Client) return;
    setLoadingStorageUsage(true);
    let totalSize = 0;
    let isTruncated = true;
    let continuationToken: string | undefined = undefined;

    try {
      while (isTruncated) {
        const command: ListObjectsV2Command = new ListObjectsV2Command({
          Bucket: bucketName,
          ContinuationToken: continuationToken,
        });
        const response = await s3Client.send(command);
        totalSize +=
          response.Contents?.reduce((sum, obj) => sum + (obj.Size || 0), 0) ||
          0;
        isTruncated = response.IsTruncated || false;
        continuationToken = response.NextContinuationToken;
      }
      setStorageUsage(totalSize);
    } catch (err) {
      console.error(
        `Failed to get storage usage for bucket ${bucketName}:`,
        err
      );
      setStorageUsage(null);
    } finally {
      setLoadingStorageUsage(false);
    }
  }, [s3Client]);

  const listObjects = useCallback(async (bucketName: string, prefix: string) => {
    if (!s3Client) {
      return;
    }

    try {
      const command = new ListObjectsV2Command({
        Bucket: bucketName,
        Delimiter: "/",
        Prefix: prefix,
      });
      const response = await s3Client?.send(command);
      
      // Filter out invalid prefixes before setting state
      const validPrefixes = (response?.CommonPrefixes || []).filter(p => {
        const folderName = p.Prefix?.replace(prefix, "").replace(/\/$/, "");
        
        // Filter out current directory, empty names, and nested paths
        const isValid = folderName && 
                       folderName.trim().length > 0 && 
                       p.Prefix !== prefix && // Don't include current directory
                       !folderName.includes('/'); // Only direct child folders
        
        return isValid;
      });
      
      // Filter out directory markers from objects
      const validObjects = (response?.Contents || []).filter(obj => {
        return obj.Key && !obj.Key.endsWith('/'); // Filter out directory markers
      });
      
      setObjects(validObjects);
      setPrefixes(validPrefixes);
      setError(null);
    } catch (err) {
      console.error(`listObjects: Failed to list objects in bucket ${bucketName}:`, err);
      setError(`Failed to list objects in bucket ${bucketName}.`);
      setObjects([]);
      setPrefixes([]);
    }
  }, [s3Client]);

  const handlePrefixClick = (prefix: string) => {
    setCurrentPrefix(prefix);
    listObjects(selectedBucket!, prefix);
  };

  const handleBackClick = () => {
    // Remove the trailing slash if it exists, then get parent directory
    const trimmedPrefix = currentPrefix.endsWith("/") ? currentPrefix.slice(0, -1) : currentPrefix;
    const pathParts = trimmedPrefix.split("/");
    
    // Remove the last part (current folder) to get parent
    pathParts.pop();
    
    // Join back and add trailing slash if there are remaining parts
    const parentPrefix = pathParts.length > 0 ? pathParts.join("/") + "/" : "";
    
    setCurrentPrefix(parentPrefix);
    listObjects(selectedBucket!, parentPrefix);
  };

  const handleUploadSuccess = () => {
    listObjects(selectedBucket!, currentPrefix);
    setUploadProgress(null);
  };

  const handleUploadProgress = (progress: number) => {
    setUploadProgress(progress);
  };

  const handleDownload = async (key: string) => {
    if (!s3Client) return;

    try {
      const command = new GetObjectCommand({
        Bucket: selectedBucket!,
        Key: key,
      });
      const response = await s3Client.send(command);
      const blob = await response.Body!.transformToByteArray();
      const url = window.URL.createObjectURL(new Blob([new Uint8Array(blob)]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", key.split("/").pop() || key);
      document.body.appendChild(link);
      link.click();
      link.parentNode?.removeChild(link);
    } catch (error) {
      console.error(`Failed to download ${key}:`, error);
      setError(`Failed to download ${key}.`);
    }
  };

  const handleDelete = async (key: string) => {
    if (!s3Client) return;

    if (window.confirm(`Are you sure you want to delete ${key}?`)) {
      try {
        const command = new DeleteObjectCommand({
          Bucket: selectedBucket!,
          Key: key,
        });
        await s3Client.send(command);
        listObjects(selectedBucket!, currentPrefix);
      } catch (error) {
        console.error(`Failed to delete ${key}:`, error);
        setError(`Failed to delete ${key}.`);
      }
    }
  };

  const handleCreateFolder = async () => {
    if (!s3Client) return;

    const folderName = prompt("Enter folder name:");
    if (folderName) {
      try {
        const command = new PutObjectCommand({
          Bucket: selectedBucket!,
          Key: `${currentPrefix}${folderName}/`,
        });
        await s3Client.send(command);
        listObjects(selectedBucket!, currentPrefix);
      } catch (error) {
        console.error(`Failed to create folder ${folderName}:`, error);
        setError(`Failed to create folder ${folderName}.`);
      }
    }
  };

  useEffect(() => {
    if (s3Client && selectedBucket) {
      listObjects(selectedBucket, currentPrefix);
      getBucketStorageUsage(selectedBucket);
    }
  }, [s3Client, selectedBucket, currentPrefix, getBucketStorageUsage, listObjects]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-100">
      {/* Header */}
      <div className="bg-white/80 backdrop-blur-sm border-b border-slate-200 sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-4">
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 sm:w-10 sm:h-10 bg-gradient-to-r from-blue-600 to-indigo-600 rounded-xl flex items-center justify-center">
                <svg className="w-5 h-5 sm:w-6 sm:h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 1v6m8-6v6" />
                </svg>
              </div>
              <div>
                <h1 className="text-xl sm:text-2xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent">
                  Swift S3 Explorer
                </h1>
                <p className="text-xs sm:text-sm text-slate-500 hidden sm:block">Manage your AWS S3 buckets with ease</p>
              </div>
            </div>
            {s3Client && selectedBucket && (
              <div className="flex items-center space-x-2 sm:space-x-4">
                <div className="text-xs sm:text-sm text-slate-600 bg-white/60 px-3 sm:px-4 py-2 rounded-lg border border-slate-200">
                  <span className="font-medium">Storage:</span>{" "}
                  {loadingStorageUsage ? (
                    <span className="animate-pulse">Loading...</span>
                  ) : storageUsage !== null ? (
                    <span className="font-semibold text-blue-600">
                      {(storageUsage / (1024 * 1024)).toFixed(2)} MB
                    </span>
                  ) : (
                    "N/A"
                  )}
                </div>
                <button
                  onClick={handleLogout}
                  className="flex items-center space-x-2 px-3 sm:px-4 py-2 bg-red-100 hover:bg-red-200 text-red-700 rounded-lg transition-colors duration-200 text-sm"
                  title="Logout"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                  </svg>
                  <span className="hidden sm:inline">Logout</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-6 sm:py-8">
        {!s3Client ? (
          /* Login Form */
          <div className="flex items-center justify-center min-h-[70vh] px-4">
            <div className="w-full max-w-md">
              <div className="bg-white/90 backdrop-blur-sm rounded-2xl shadow-2xl border border-white/20 p-6 sm:p-8">
                <div className="text-center mb-6 sm:mb-8">
                  <div className="w-12 h-12 sm:w-16 sm:h-16 bg-gradient-to-r from-blue-600 to-indigo-600 rounded-2xl flex items-center justify-center mx-auto mb-4">
                    <svg className="w-6 h-6 sm:w-8 sm:h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" />
                    </svg>
                  </div>
                  <h2 className="text-xl sm:text-2xl font-bold text-slate-800 mb-2">Connect to AWS S3</h2>
                  <p className="text-sm sm:text-base text-slate-600">Enter your credentials to get started</p>
                </div>

                <div className="space-y-4 sm:space-y-6">
                  <div className="space-y-2">
                    <label className="text-sm font-medium text-slate-700">Access Key ID</label>
                    <input
                      type="text"
                      name="accessKeyId"
                      placeholder="AKIA..."
                      value={credentials.accessKeyId}
                      onChange={handleCredentialChange}
                      className="w-full px-4 py-3 bg-white/70 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 text-slate-900 placeholder-slate-400"
                    />
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm font-medium text-slate-700">Secret Access Key</label>
                    <input
                      type="password"
                      name="secretAccessKey"
                      placeholder="••••••••••••••••••••••••••••••••••••••••"
                      value={credentials.secretAccessKey}
                      onChange={handleCredentialChange}
                      className="w-full px-4 py-3 bg-white/70 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 text-slate-900 placeholder-slate-400"
                    />
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm font-medium text-slate-700">Bucket Name (Optional)</label>
                    <input
                      type="text"
                      name="bucketName"
                      placeholder="my-s3-bucket"
                      value={bucketNameInput}
                      onChange={(e) => setBucketNameInput(e.target.value)}
                      className="w-full px-4 py-3 bg-white/70 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 text-slate-900 placeholder-slate-400"
                    />
                  </div>

                  <div className="space-y-2">
                    <label className="text-sm font-medium text-slate-700">AWS Region</label>
                    <input
                      type="text"
                      name="region"
                      placeholder="us-east-1"
                      value={regionInput}
                      onChange={(e) => setRegionInput(e.target.value)}
                      className="w-full px-4 py-3 bg-white/70 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 text-slate-900 placeholder-slate-400"
                    />
                  </div>

                  <div className="flex items-center space-x-3">
                    <input
                      type="checkbox"
                      id="saveCredentials"
                      checked={saveCredentials}
                      onChange={(e) => setSaveCredentials(e.target.checked)}
                      className="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500"
                    />
                    <label htmlFor="saveCredentials" className="text-sm font-medium text-slate-700">
                      Remember my credentials securely
                    </label>
                  </div>

                  <button
                    onClick={handleLogin}
                    className="w-full bg-gradient-to-r from-blue-600 to-indigo-600 text-white py-3 px-6 rounded-xl font-medium hover:from-blue-700 hover:to-indigo-700 transform hover:scale-105 transition-all duration-200 shadow-lg hover:shadow-xl"
                  >
                    Connect to S3
                  </button>

                  {error && (
                    <div className="bg-red-50 border border-red-200 rounded-xl p-4">
                      <p className="text-red-600 text-sm">{error}</p>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        ) : selectedBucket ? (
          /* Main Explorer Interface */
          <div className="space-y-6">
            {/* Breadcrumb and Actions */}
            <div className="bg-white/90 backdrop-blur-sm rounded-2xl shadow-lg border border-white/20 p-4 sm:p-6">
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                <div className="flex items-center space-x-2 text-sm overflow-x-auto w-full sm:w-auto">
                  <span className="px-3 py-1 bg-blue-100 text-blue-800 rounded-lg font-medium whitespace-nowrap">
                    {selectedBucket}
                  </span>
                  {currentPrefix && (
                    <>
                      <span className="text-slate-400">/</span>
                      <span className="px-3 py-1 bg-slate-100 text-slate-700 rounded-lg whitespace-nowrap">
                        {currentPrefix}
                      </span>
                    </>
                  )}
                </div>

                <div className="flex items-center space-x-2 sm:space-x-3 w-full sm:w-auto">
                  {currentPrefix && (
                    <button
                      onClick={handleBackClick}
                      className="flex items-center space-x-2 px-3 sm:px-4 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg transition-colors duration-200 text-sm"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" />
                      </svg>
                      <span className="hidden sm:inline">Back</span>
                    </button>
                  )}

                  <div className="relative flex-1 sm:flex-none">
                    <input
                      type="text"
                      placeholder="Search..."
                      className="pl-8 sm:pl-10 pr-3 sm:pr-4 py-2 bg-white/70 border border-slate-200 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 text-sm w-full sm:w-auto text-slate-900 placeholder-slate-400"
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                    />
                    <svg className="w-4 h-4 text-slate-400 absolute left-2 sm:left-3 top-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                    </svg>
                  </div>

                  <button
                    onClick={handleCreateFolder}
                    className="flex items-center space-x-1 sm:space-x-2 px-3 sm:px-4 py-2 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-lg hover:from-green-700 hover:to-emerald-700 transition-all duration-200 shadow-lg hover:shadow-xl text-sm whitespace-nowrap"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                    </svg>
                    <span className="hidden sm:inline">New Folder</span>
                    <span className="sm:hidden">New</span>
                  </button>
                </div>
              </div>
            </div>

            {/* Upload Area */}
            <div className="bg-white/90 backdrop-blur-sm rounded-2xl shadow-lg border border-white/20 p-4 sm:p-6">
              <Dropzone
                s3Client={s3Client}
                bucketName={selectedBucket}
                prefix={currentPrefix}
                onUploadSuccess={handleUploadSuccess}
                onProgress={handleUploadProgress}
              />
              {uploadProgress !== null && (
                <div className="mt-4">
                  <div className="flex justify-between text-sm text-slate-600 mb-2">
                    <span>Uploading...</span>
                    <span>{uploadProgress}%</span>
                  </div>
                  <div className="w-full bg-slate-200 rounded-full h-2">
                    <div
                      className="bg-gradient-to-r from-blue-600 to-indigo-600 h-2 rounded-full transition-all duration-300"
                      style={{ width: `${uploadProgress}%` }}
                    ></div>
                  </div>
                </div>
              )}
            </div>

            {/* Files and Folders Grid */}
            <div className="bg-white/90 backdrop-blur-sm rounded-2xl shadow-lg border border-white/20 overflow-hidden">
              <div className="p-4 sm:p-6 border-b border-slate-200">
                <h3 className="text-lg font-semibold text-slate-800">Files & Folders</h3>
              </div>
              
              {/* Desktop Table View */}
              <div className="hidden sm:block overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-slate-50">
                    <tr>
                      <th
                        className="px-6 py-4 text-left text-sm font-medium text-slate-600 cursor-pointer hover:text-slate-800 transition-colors"
                        onClick={() => requestSort("Key")}
                      >
                        <div className="flex items-center space-x-2">
                          <span>Name</span>
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4" />
                          </svg>
                        </div>
                      </th>
                      <th
                        className="px-6 py-4 text-left text-sm font-medium text-slate-600 cursor-pointer hover:text-slate-800 transition-colors"
                        onClick={() => requestSort("LastModified")}
                      >
                        <div className="flex items-center space-x-2">
                          <span>Modified</span>
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4" />
                          </svg>
                        </div>
                      </th>
                      <th
                        className="px-6 py-4 text-left text-sm font-medium text-slate-600 cursor-pointer hover:text-slate-800 transition-colors"
                        onClick={() => requestSort("Size")}
                      >
                        <div className="flex items-center space-x-2">
                          <span>Size</span>
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4" />
                          </svg>
                        </div>
                      </th>
                      <th className="px-6 py-4 text-left text-sm font-medium text-slate-600">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {/* Folders */}
                    {prefixes
                      .map((prefix) => {
                        const folderName = prefix.Prefix?.replace(currentPrefix, "").replace(/\/$/, "");
                        return { ...prefix, folderName };
                      })
                      .filter((item) => {
                        // Filter out the current directory itself and empty/invalid names
                        const isValid = item.folderName && 
                                       item.folderName.trim().length > 0 && 
                                       item.Prefix !== currentPrefix && // Don't show current directory as a folder
                                       !item.folderName.includes('/') && // Only direct child folders
                                       item.folderName.toLowerCase().includes(searchTerm.toLowerCase());
                        return isValid;
                      })
                      .map((item) => (
                        <tr
                          key={item.Prefix}
                          onClick={() => handlePrefixClick(item.Prefix!)}
                          className="cursor-pointer hover:bg-blue-50 transition-colors duration-200"
                        >
                          <td className="px-6 py-4">
                            <div className="flex items-center space-x-3">
                              <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
                                <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2z" />
                                </svg>
                              </div>
                              <span className="font-medium text-slate-800">
                                {item.folderName || "Unnamed Folder"}
                              </span>
                            </div>
                          </td>
                          <td className="px-6 py-4 text-slate-500">—</td>
                          <td className="px-6 py-4 text-slate-500">—</td>
                          <td className="px-6 py-4 text-slate-500">—</td>
                        </tr>
                      ))}
                    
                    {/* Files */}
                    {sortedObjects
                      .filter((o) => {
                        // Filter out directory markers and ensure we have a valid filename
                        const fileName = o.Key?.replace(currentPrefix, "").split('/').pop();
                        return o.Key && 
                               !o.Key.endsWith('/') && // Not a directory marker
                               fileName && 
                               fileName.trim().length > 0 && 
                               o.Key.toLowerCase().includes(searchTerm.toLowerCase());
                      })
                      .map((object) => (
                        <tr key={object.Key} className="hover:bg-slate-50 transition-colors duration-200">
                          <td className="px-6 py-4">
                            <div className="flex items-center space-x-3">
                              <div className="w-8 h-8 bg-slate-100 rounded-lg flex items-center justify-center">
                                <span className="text-lg" dangerouslySetInnerHTML={{ __html: getFileIcon(object.Key!) }}></span>
                              </div>
                              <span 
                                className="font-medium text-slate-800 truncate max-w-xs cursor-help" 
                                title={object.Key?.replace(currentPrefix, "")}
                              >
                                {object.Key?.replace(currentPrefix, "").split('/').pop() || "Unnamed File"}
                              </span>
                            </div>
                          </td>
                          <td className="px-6 py-4 text-slate-600 text-sm">
                            {object.LastModified?.toLocaleDateString()}
                          </td>
                          <td className="px-6 py-4 text-slate-600 text-sm">
                            {object.Size ? `${(object.Size / 1024).toFixed(1)} KB` : "—"}
                          </td>
                          <td className="px-6 py-4">
                            <div className="flex items-center space-x-2">
                              <button
                                onClick={() => handleDownload(object.Key!)}
                                className="flex items-center space-x-1 px-3 py-1.5 bg-green-100 hover:bg-green-200 text-green-700 rounded-lg transition-colors duration-200"
                              >
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                                </svg>
                                <span className="text-sm">Download</span>
                              </button>
                              <button
                                onClick={() => handleDelete(object.Key!)}
                                className="flex items-center space-x-1 px-3 py-1.5 bg-red-100 hover:bg-red-200 text-red-700 rounded-lg transition-colors duration-200"
                              >
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                </svg>
                                <span className="text-sm">Delete</span>
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>

              {/* Mobile Card View */}
              <div className="sm:hidden divide-y divide-slate-200">
                {/* Folders */}
                {prefixes
                  .map((prefix) => {
                    const folderName = prefix.Prefix?.replace(currentPrefix, "").replace(/\/$/, "");
                    return { ...prefix, folderName };
                  })
                  .filter((item) => {
                    // Filter out the current directory itself and empty/invalid names
                    const isValid = item.folderName && 
                                   item.folderName.trim().length > 0 && 
                                   item.Prefix !== currentPrefix && // Don't show current directory as a folder
                                   !item.folderName.includes('/') && // Only direct child folders
                                   item.folderName.toLowerCase().includes(searchTerm.toLowerCase());
                    return isValid;
                  })
                  .map((item) => (
                    <div
                      key={item.Prefix}
                      onClick={() => handlePrefixClick(item.Prefix!)}
                      className="p-4 cursor-pointer hover:bg-blue-50 transition-colors duration-200"
                    >
                      <div className="flex items-center space-x-3">
                        <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center flex-shrink-0">
                          <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2z" />
                          </svg>
                        </div>
                        <span className="font-medium text-slate-800 truncate">
                          {item.folderName || "Unnamed Folder"}
                        </span>
                      </div>
                    </div>
                  ))}

                {/* Files */}
                {sortedObjects
                  .filter((o) => {
                    const fileName = o.Key?.replace(currentPrefix, "").split('/').pop();
                    return o.Key && !o.Key.endsWith('/') && fileName && fileName.trim().length > 0 && o.Key.toLowerCase().includes(searchTerm.toLowerCase());
                  })
                  .map((object) => (
                    <div key={object.Key} className="p-4">
                      <div className="flex items-center space-x-3 mb-3">
                        <div className="w-10 h-10 bg-slate-100 rounded-lg flex items-center justify-center flex-shrink-0">
                          <span className="text-xl" dangerouslySetInnerHTML={{ __html: getFileIcon(object.Key!) }}></span>
                        </div>
                        <div className="flex-1 min-w-0">
                          <p 
                            className="font-medium text-slate-800 truncate"
                            title={object.Key?.replace(currentPrefix, "")}
                          >
                            {object.Key?.replace(currentPrefix, "").split('/').pop() || "Unnamed File"}
                          </p>
                          <p className="text-sm text-slate-500">
                            {object.LastModified?.toLocaleDateString()} &middot; {object.Size ? `${(object.Size / 1024).toFixed(1)} KB` : "—"}
                          </p>
                        </div>
                      </div>
                      <div className="flex items-center justify-end space-x-2">
                        <button
                          onClick={() => handleDownload(object.Key!)}
                          className="flex items-center space-x-1 px-3 py-1.5 bg-green-100 hover:bg-green-200 text-green-700 rounded-lg transition-colors duration-200 text-sm"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                          </svg>
                          <span>Download</span>
                        </button>
                        <button
                          onClick={() => handleDelete(object.Key!)}
                          className="flex items-center space-x-1 px-3 py-1.5 bg-red-100 hover:bg-red-200 text-red-700 rounded-lg transition-colors duration-200 text-sm"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                          <span>Delete</span>
                        </button>
                      </div>
                    </div>
                  ))}
              </div>
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-center min-h-[70vh]">
            <p className="text-slate-500 text-sm">
              {/* eslint-disable-next-line react/no-unescaped-entities */}
              You're connected to AWS S3! Now you can manage your files and folders.
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
