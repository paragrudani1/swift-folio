import { useState, useEffect, useCallback } from "react";
import { S3Client, ListObjectsV2Command, _Object, CommonPrefix } from "@aws-sdk/client-s3";
import { 
  loadCredentialsFromStorage, 
  saveCredentialsToStorage, 
  clearStoredCredentials,
  StoredCredentials 
} from "../utils";
import { DEFAULT_REGION } from "../constants";

/**
 * Custom hook for managing AWS S3 authentication and client state
 */
export const useS3Auth = () => {
  const [credentials, setCredentials] = useState<StoredCredentials>({
    accessKeyId: "",
    secretAccessKey: "",
  });
  const [bucketNameInput, setBucketNameInput] = useState("");
  const [regionInput, setRegionInput] = useState(DEFAULT_REGION);
  const [saveCredentials, setSaveCredentials] = useState(false);
  const [s3Client, setS3Client] = useState<S3Client | null>(null);
  const [selectedBucket, setSelectedBucket] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Auto-login with saved credentials
  useEffect(() => {
    const autoLogin = async () => {
      try {
        const stored = loadCredentialsFromStorage();
        
        if (stored.credentials) {
          setCredentials(stored.credentials);
          setSaveCredentials(true);
          
          if (stored.bucketName) setBucketNameInput(stored.bucketName);
          if (stored.region) setRegionInput(stored.region);

          // Create S3 client with stored credentials
          const client = new S3Client({
            region: stored.region || DEFAULT_REGION,
            credentials: stored.credentials,
          });
          setS3Client(client);

          if (stored.bucketName) {
            setSelectedBucket(stored.bucketName);
          }
        }
      } catch (error) {
        console.error("Auto-login failed:", error);
        clearStoredCredentials();
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
  };

  const handleLogout = () => {
    setS3Client(null);
    setSelectedBucket(null);
    setCredentials({ accessKeyId: "", secretAccessKey: "" });
    setError(null);
    clearStoredCredentials();
  };

  return {
    credentials,
    bucketNameInput,
    setBucketNameInput,
    regionInput,
    setRegionInput,
    saveCredentials,
    setSaveCredentials,
    s3Client,
    selectedBucket,
    error,
    setError,
    handleCredentialChange,
    handleLogin,
    handleLogout,
  };
};

/**
 * Custom hook for managing S3 bucket operations
 */
export const useS3Operations = (s3Client: S3Client | null, selectedBucket: string | null) => {
  const [objects, setObjects] = useState<_Object[]>([]);
  const [prefixes, setPrefixes] = useState<CommonPrefix[]>([]);
  const [currentPrefix, setCurrentPrefix] = useState<string>("");
  const [storageUsage, setStorageUsage] = useState<number | null>(null);
  const [loadingStorageUsage, setLoadingStorageUsage] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");

  const listObjects = useCallback(
    async (bucketName: string, prefix: string) => {
      if (!s3Client) return;

      try {
        const command = new ListObjectsV2Command({
          Bucket: bucketName,
          Delimiter: "/",
          Prefix: prefix,
        });
        const response = await s3Client.send(command);

        // Filter valid prefixes and objects
        const validPrefixes = (response?.CommonPrefixes || []).filter((p) => {
          const folderName = p.Prefix?.replace(prefix, "").replace(/\/$/, "");
          return (
            folderName &&
            folderName.trim().length > 0 &&
            p.Prefix !== prefix &&
            !folderName.includes("/")
          );
        });

        const validObjects = (response?.Contents || []).filter((obj) => {
          return obj.Key && !obj.Key.endsWith("/");
        });

        setObjects(validObjects);
        setPrefixes(validPrefixes);
      } catch (err) {
        console.error(`Failed to list objects in bucket ${bucketName}:`, err);
        setObjects([]);
        setPrefixes([]);
        throw err;
      }
    },
    [s3Client]
  );

  const getBucketStorageUsage = useCallback(
    async (bucketName: string) => {
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
            response.Contents?.reduce((sum: number, obj) => sum + (obj.Size || 0), 0) || 0;
          isTruncated = response.IsTruncated || false;
          continuationToken = response.NextContinuationToken;
        }
        setStorageUsage(totalSize);
      } catch (err) {
        console.error(`Failed to get storage usage for bucket ${bucketName}:`, err);
        setStorageUsage(null);
      } finally {
        setLoadingStorageUsage(false);
      }
    },
    [s3Client]
  );

  const handlePrefixClick = (prefix: string) => {
    setCurrentPrefix(prefix);
    if (selectedBucket) {
      listObjects(selectedBucket, prefix);
    }
  };

  const handleBackClick = () => {
    const trimmedPrefix = currentPrefix.endsWith("/")
      ? currentPrefix.slice(0, -1)
      : currentPrefix;
    const pathParts = trimmedPrefix.split("/");
    pathParts.pop();
    const parentPrefix = pathParts.length > 0 ? pathParts.join("/") + "/" : "";
    setCurrentPrefix(parentPrefix);
    if (selectedBucket) {
      listObjects(selectedBucket, parentPrefix);
    }
  };

  const refreshCurrentView = () => {
    if (selectedBucket) {
      listObjects(selectedBucket, currentPrefix);
    }
  };

  useEffect(() => {
    if (s3Client && selectedBucket) {
      listObjects(selectedBucket, currentPrefix);
      getBucketStorageUsage(selectedBucket);
    }
  }, [s3Client, selectedBucket, currentPrefix, listObjects, getBucketStorageUsage]);

  return {
    objects,
    prefixes,
    currentPrefix,
    storageUsage,
    loadingStorageUsage,
    searchTerm,
    setSearchTerm,
    handlePrefixClick,
    handleBackClick,
    refreshCurrentView,
    listObjects,
  };
};
