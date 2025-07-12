"use client";

import { _Object } from "@aws-sdk/client-s3";
import { useTheme } from "../../../contexts/ThemeContext";
import { extractFileName, getFileIcon, isValidFile } from "../../utils";

interface FileCardProps {
  object: _Object;
  currentPrefix: string;
  isSelectMode: boolean;
  isSelected: boolean;
  onItemSelection: (key: string) => void;
  onDownload: (key: string) => void;
  onDelete: (key: string) => void;
}

export function FileCard({
  object,
  currentPrefix,
  isSelectMode,
  isSelected,
  onItemSelection,
  onDownload,
  onDelete,
}: FileCardProps) {
  const { token } = useTheme();

  if (!isValidFile(object.Key || "")) {
    return null;
  }

  const fileName = extractFileName(object.Key || "", currentPrefix);

  return (
    <div 
      key={object.Key} 
      className="p-4" 
      style={{ 
        borderBottomColor: token('color', 'primaryBorder'), 
        borderBottomWidth: '1px' 
      }}
    >
      <div className="flex items-center space-x-3 mb-3">
        {isSelectMode && (
          <input
            type="checkbox"
            checked={isSelected}
            onChange={() => onItemSelection(object.Key!)}
            className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500 flex-shrink-0"
            style={{ 
              backgroundColor: token('color', 'primaryBg'), 
              borderColor: token('color', 'primaryBorder') 
            }}
          />
        )}
        <div 
          className="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0" 
          style={{ backgroundColor: token('color', 'secondaryBg') }}
        >
          <span
            className="text-xl"
            dangerouslySetInnerHTML={{
              __html: getFileIcon(object.Key!),
            }}
          ></span>
        </div>
        <div className="flex-1 min-w-0">
          <p
            className="font-medium truncate"
            title={fileName}
            style={{ color: token('color', 'primaryText') }}
          >
            {fileName}
          </p>
          <p className="text-sm" style={{ color: token('color', 'mutedText') }}>
            {object.LastModified?.toLocaleDateString()} &middot;{" "}
            {object.Size
              ? `${(object.Size / 1024).toFixed(1)} KB`
              : "—"}
          </p>
        </div>
      </div>
      <div className="flex items-center justify-end space-x-2">
        <button
          onClick={() => onDownload(object.Key!)}
          className="flex items-center space-x-1 px-3 py-1.5 rounded-lg transition-colors duration-200 text-sm"
          style={{ 
            backgroundColor: 'rgb(220 252 231)', 
            color: 'rgb(21 128 61)' 
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.backgroundColor = 'rgb(187 247 208)';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.backgroundColor = 'rgb(220 252 231)';
          }}
        >
          <svg
            className="w-4 h-4"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
            />
          </svg>
          <span>Download</span>
        </button>
        <button
          onClick={() => onDelete(object.Key!)}
          className="flex items-center space-x-1 px-3 py-1.5 bg-red-100 hover:bg-red-200 text-red-700 rounded-lg transition-colors duration-200 text-sm"
        >
          <svg
            className="w-4 h-4"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
            />
          </svg>
          <span>Delete</span>
        </button>
      </div>
    </div>
  );
}
