"use client";

import { S3Client } from "@aws-sdk/client-s3";
import { useTheme } from "../../contexts/ThemeContext";
import { Dropzone } from "../Dropzone";

interface UploadAreaProps {
  s3Client: S3Client;
  selectedBucket: string;
  currentPrefix: string;
  uploadProgress: number | null;
  currentlyUploadingFolder: string | null;
  onUploadSuccess: () => void;
  onUploadProgress: (progress: number, folder?: string) => void;
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
}

export function UploadArea({
  s3Client,
  selectedBucket,
  currentPrefix,
  uploadProgress,
  currentlyUploadingFolder,
  onUploadSuccess,
  onUploadProgress,
  uploadFiles,
  cancelUpload,
  isUploading = false,
  onUploadCancelled,
}: UploadAreaProps) {
  const { token } = useTheme();

  return (
    <div className="backdrop-blur-sm rounded-2xl border p-4 sm:p-6 lg:p-8" style={{ 
      backgroundColor: token('color', 'overlayBg'), 
      boxShadow: token('shadow', 'lg'), 
      borderColor: token('color', 'primaryBorder') 
    }}>
      <Dropzone
        s3Client={s3Client}
        bucketName={selectedBucket}
        prefix={currentPrefix}
        onUploadSuccess={onUploadSuccess}
        onProgress={onUploadProgress}
        uploadFiles={uploadFiles}
        cancelUpload={cancelUpload}
        isUploading={isUploading}
        onUploadCancelled={onUploadCancelled}
      />
      
      {/* Upload Progress */}
      {uploadProgress !== null && (
        <div className="mt-4">
          <div className="flex justify-between items-center text-sm mb-2" style={{ color: token('color', 'secondaryText') }}>
            <span>
              {currentlyUploadingFolder
                ? `Uploading folder: ${currentlyUploadingFolder}`
                : "Uploading..."}
            </span>
            <span>{uploadProgress}%</span>
          </div>
          <div className="w-full rounded-full h-2" style={{ backgroundColor: token('color', 'secondaryBg') }}>
            <div
              className="bg-gradient-to-r from-blue-600 to-indigo-600 h-2 rounded-full transition-all duration-300"
              style={{ width: `${uploadProgress}%` }}
            ></div>
          </div>
        </div>
      )}
    </div>
  );
}
