"use client";

import { useCallback } from "react";
import { useDropzone } from "react-dropzone";
import { Upload } from "@aws-sdk/lib-storage";
import { S3Client } from "@aws-sdk/client-s3";
import { useTheme } from "../contexts/ThemeContext";

// Extend HTMLInputElement to include webkitdirectory
declare module "react" {
  interface InputHTMLAttributes<T> extends AriaAttributes, DOMAttributes<T> {
    webkitdirectory?: string;
  }
}

interface DropzoneProps {
  s3Client: S3Client;
  bucketName: string;
  prefix: string;
  onUploadSuccess: () => void;
  onProgress: (progress: number) => void;
}

export function Dropzone({
  s3Client,
  bucketName,
  prefix,
  onUploadSuccess,
  onProgress,
}: DropzoneProps) {
  const { token } = useTheme();
  const onDrop = useCallback(
    async (acceptedFiles: File[]) => {
      for (const file of acceptedFiles) {
        try {
          const fileWithPath = file as File & {
            webkitRelativePath?: string;
            relativePath: string;
          };
          const relativePath = fileWithPath.relativePath.startsWith("./")
            ? fileWithPath.name
            : fileWithPath.relativePath.substring(1);

          const upload = new Upload({
            client: s3Client,
            params: {
              Bucket: bucketName,
              Key: `${prefix}${relativePath}`,
              Body: file,
            },
          });

          upload.on("httpUploadProgress", (progress) => {
            if (progress.loaded && progress.total) {
              const percentage = Math.round(
                (progress.loaded / progress.total) * 100
              );
              onProgress(percentage);
            }
          });

          await upload.done();
        } catch (error) {
          console.error(`Failed to upload ${file.name}:`, error);
        }
      }

      onUploadSuccess();
    },
    [s3Client, bucketName, prefix, onUploadSuccess, onProgress]
  );

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    // Allow both files and directories
    onDropAccepted: (files) => {
      // Handle regular file drops
      onDrop(files);
    },
  });

  return (
    <div
      {...getRootProps()}
      className="relative border-2 border-dashed rounded-2xl p-4 sm:p-8 text-center cursor-pointer transition-all duration-300"
      style={{
        borderColor: isDragActive ? token('color', 'focusBorder') : token('color', 'secondaryBorder'),
        backgroundColor: isDragActive ? token('color', 'hoverBg') : 'transparent',
        transform: isDragActive ? 'scale(1.02)' : 'scale(1)',
      }}
      onMouseEnter={(e) => {
        if (!isDragActive) {
          e.currentTarget.style.backgroundColor = token('color', 'hoverBg');
        }
      }}
      onMouseLeave={(e) => {
        if (!isDragActive) {
          e.currentTarget.style.backgroundColor = 'transparent';
        }
      }}
    >
      <input {...getInputProps()} />
      <div className="flex flex-col items-center space-y-3 sm:space-y-4">
        <div
          className="w-12 h-12 sm:w-16 sm:h-16 rounded-2xl flex items-center justify-center transition-all duration-300"
          style={{
            backgroundColor: isDragActive ? token('color', 'focusBorder') : token('color', 'tertiaryBg'),
            transform: isDragActive ? 'scale(1.1)' : 'scale(1)',
          }}
        >
          <svg
            className="w-6 h-6 sm:w-8 sm:h-8 text-white"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
              d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
            />
          </svg>
        </div>

        <div>
          <h3
            className="text-base sm:text-lg font-semibold mb-1 sm:mb-2 transition-colors duration-300"
            style={{
              color: isDragActive ? '#1e40af' : token('color', 'primaryText')
            }}
          >
            {isDragActive ? "Drop your files here!" : "Upload Files"}
          </h3>
          <p
            className="text-xs sm:text-sm transition-colors duration-300"
            style={{
              color: isDragActive ? '#2563eb' : token('color', 'secondaryText')
            }}
          >
            {isDragActive
              ? "Release to upload to your S3 bucket"
              : "Drag & drop files or folders here, or click to browse"}
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-center space-y-2 sm:space-y-0 sm:space-x-4 text-xs" style={{ color: token('color', 'mutedText') }}>
          <div className="flex items-center space-x-1">
            <svg
              className="w-3 h-3 sm:w-4 sm:h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
            <span>Secure upload</span>
          </div>
          <div className="flex items-center space-x-1">
            <svg
              className="w-3 h-3 sm:w-4 sm:h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M13 10V3L4 14h7v7l9-11h-7z"
              />
            </svg>
            <span>Fast transfer</span>
          </div>
        </div>
      </div>
    </div>
  );
}
