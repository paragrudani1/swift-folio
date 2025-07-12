import { useState, useCallback } from "react";
import { 
  S3Client, 
  ListObjectsV2Command, 
  _Object, 
  CommonPrefix,
  GetObjectCommand,
  DeleteObjectCommand,
  PutObjectCommand,
  ListObjectsV2CommandOutput,
} from "@aws-sdk/client-s3";
import { Credentials } from "../types";
import { S3_CONFIG } from "../constants";

/**
 * Custom hook for S3 operations
 */
export const useS3Operations = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /**
   * Create S3 client with credentials
   */
  const createS3Client = useCallback((credentials: Credentials, region: string): S3Client => {
    return new S3Client({
      region,
      credentials: {
        accessKeyId: credentials.accessKeyId,
        secretAccessKey: credentials.secretAccessKey,
      },
    });
  }, []);

  /**
   * List objects in S3 bucket
   */
  const listObjects = useCallback(async (
    s3Client: S3Client, 
    bucketName: string, 
    prefix: string
  ): Promise<{ objects: _Object[]; prefixes: CommonPrefix[] }> => {
    if (!s3Client) {
      throw new Error("S3 client not initialized");
    }

    setIsLoading(true);
    setError(null);

    try {
      const command = new ListObjectsV2Command({
        Bucket: bucketName,
        Delimiter: S3_CONFIG.delimiter,
        Prefix: prefix,
        MaxKeys: S3_CONFIG.maxKeys,
      });
      
      const response = await s3Client.send(command);

      // Filter out invalid prefixes before returning
      const validPrefixes = (response?.CommonPrefixes || []).filter((p) => {
        const folderName = p.Prefix?.replace(prefix, "").replace(/\/$/, "");
        return (
          folderName &&
          folderName.trim().length > 0 &&
          p.Prefix !== prefix && // Don't include current directory
          !folderName.includes("/") // Only direct child folders
        );
      });

      // Filter out directory markers from objects
      const validObjects = (response?.Contents || []).filter((obj) => {
        return obj.Key && !obj.Key.endsWith("/"); // Filter out directory markers
      });

      return {
        objects: validObjects,
        prefixes: validPrefixes,
      };
    } catch (err) {
      const errorMessage = `Failed to list objects in bucket ${bucketName}`;
      setError(errorMessage);
      console.error(errorMessage, err);
      throw new Error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Download file from S3
   */
  const downloadFile = useCallback(async (
    s3Client: S3Client,
    bucketName: string,
    key: string
  ): Promise<void> => {
    if (!s3Client) {
      throw new Error("S3 client not initialized");
    }

    setIsLoading(true);
    setError(null);

    try {
      const command = new GetObjectCommand({
        Bucket: bucketName,
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
      window.URL.revokeObjectURL(url);
    } catch (err) {
      const errorMessage = `Failed to download ${key}`;
      setError(errorMessage);
      console.error(errorMessage, err);
      throw new Error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Delete file from S3
   */
  const deleteFile = useCallback(async (
    s3Client: S3Client,
    bucketName: string,
    key: string
  ): Promise<void> => {
    if (!s3Client) {
      throw new Error("S3 client not initialized");
    }

    setIsLoading(true);
    setError(null);

    try {
      const command = new DeleteObjectCommand({
        Bucket: bucketName,
        Key: key,
      });
      
      await s3Client.send(command);
    } catch (err) {
      const errorMessage = `Failed to delete ${key}`;
      setError(errorMessage);
      console.error(errorMessage, err);
      throw new Error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Delete multiple files from S3
   */
  const deleteMultipleFiles = useCallback(async (
    s3Client: S3Client,
    bucketName: string,
    keys: string[]
  ): Promise<void> => {
    if (!s3Client) {
      throw new Error("S3 client not initialized");
    }

    setIsLoading(true);
    setError(null);

    try {
      const deletePromises = keys.map(async (key) => {
        const command = new DeleteObjectCommand({
          Bucket: bucketName,
          Key: key,
        });
        return s3Client.send(command);
      });

      await Promise.all(deletePromises);
    } catch (err) {
      const errorMessage = `Failed to delete selected items`;
      setError(errorMessage);
      console.error(errorMessage, err);
      throw new Error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Create folder in S3
   */
  const createFolder = useCallback(async (
    s3Client: S3Client,
    bucketName: string,
    folderName: string,
    currentPrefix: string
  ): Promise<void> => {
    if (!s3Client) {
      throw new Error("S3 client not initialized");
    }

    setIsLoading(true);
    setError(null);

    try {
      const folderKey = `${currentPrefix}${folderName}/`;
      const command = new PutObjectCommand({
        Bucket: bucketName,
        Key: folderKey,
        Body: "", // Empty body for folder marker
      });
      
      await s3Client.send(command);
    } catch (err) {
      const errorMessage = `Failed to create folder ${folderName}`;
      setError(errorMessage);
      console.error(errorMessage, err);
      throw new Error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Get bucket storage usage
   */
  const getBucketStorageUsage = useCallback(async (
    s3Client: S3Client,
    bucketName: string
  ): Promise<number> => {
    if (!s3Client) {
      throw new Error("S3 client not initialized");
    }

    setIsLoading(true);
    setError(null);

    try {
      let totalSize = 0;
      let isTruncated = true;
      let continuationToken: string | undefined = undefined;

      while (isTruncated) {
        const command: ListObjectsV2Command = new ListObjectsV2Command({
          Bucket: bucketName,
          ContinuationToken: continuationToken,
        });
        
        const response: ListObjectsV2CommandOutput = await s3Client.send(command);
        totalSize += response.Contents?.reduce((sum: number, obj: _Object) => sum + (obj.Size || 0), 0) || 0;
        isTruncated = response.IsTruncated || false;
        continuationToken = response.NextContinuationToken;
      }

      return totalSize;
    } catch (err) {
      const errorMessage = `Failed to get storage usage for bucket ${bucketName}`;
      setError(errorMessage);
      console.error(errorMessage, err);
      throw new Error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  }, []);

  /**
   * Delete folder and all its contents from S3
   */
  const deleteFolder = useCallback(async (
    s3Client: S3Client,
    bucketName: string,
    folderPrefix: string
  ): Promise<void> => {
    if (!s3Client) {
      throw new Error("S3 client not initialized");
    }

    setIsLoading(true);
    setError(null);

    try {
      // First, list all objects in the folder
      const listCommand = new ListObjectsV2Command({
        Bucket: bucketName,
        Prefix: folderPrefix,
      });
      
      const response = await s3Client.send(listCommand);
      const objectsToDelete = response.Contents || [];

      if (objectsToDelete.length > 0) {
        // Delete all objects in the folder
        const deletePromises = objectsToDelete.map(async (obj) => {
          const command = new DeleteObjectCommand({
            Bucket: bucketName,
            Key: obj.Key!,
          });
          return s3Client.send(command);
        });

        await Promise.all(deletePromises);
      }
    } catch (err) {
      const errorMessage = `Failed to delete folder ${folderPrefix}`;
      setError(errorMessage);
      console.error(errorMessage, err);
      throw new Error(errorMessage);
    } finally {
      setIsLoading(false);
    }
  }, []);

  return {
    isLoading,
    error,
    createS3Client,
    listObjects,
    downloadFile,
    deleteFile,
    deleteMultipleFiles,
    deleteFolder,
    createFolder,
    getBucketStorageUsage,
    setError,
  };
};
