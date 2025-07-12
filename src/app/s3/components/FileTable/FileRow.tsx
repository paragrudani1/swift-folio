"use client";

import { _Object } from "@aws-sdk/client-s3";
import { useTheme } from "../../../contexts/ThemeContext";
import { extractFileName, getFileIcon, isValidFile } from "../../utils";

interface FileRowProps {
  object: _Object;
  currentPrefix: string;
  isSelectMode: boolean;
  isSelected: boolean;
  onItemSelection: (key: string) => void;
  onDownload: (key: string) => void;
  onDelete: (key: string) => void;
}

export function FileRow({
  object,
  currentPrefix,
  isSelectMode,
  isSelected,
  onItemSelection,
  onDownload,
  onDelete,
}: FileRowProps) {
  const { token } = useTheme();

  if (!isValidFile(object.Key || "")) {
    return null;
  }

  return (
    <tr
      className="transition-colors duration-200"
      style={{ borderBottomColor: token('color', 'primaryBorder') }}
      onMouseEnter={(e) => {
        e.currentTarget.style.backgroundColor = token('color', 'hoverBg');
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.backgroundColor = 'transparent';
      }}
    >
      {isSelectMode && (
        <td className="w-12 px-4 py-4 md:px-6">
          <input
            type="checkbox"
            checked={isSelected}
            onChange={() => onItemSelection(object.Key!)}
            className="w-5 h-5 md:w-4 md:h-4 text-blue-600 rounded focus:ring-blue-500 cursor-pointer"
            style={{ 
              backgroundColor: token('color', 'primaryBg'), 
              borderColor: token('color', 'primaryBorder') 
            }}
          />
        </td>
      )}
      <td className="px-4 md:px-6 py-4">
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 rounded-lg flex items-center justify-center" style={{ backgroundColor: token('color', 'secondaryBg') }}>
            <span
              className="text-lg"
              dangerouslySetInnerHTML={{
                __html: getFileIcon(object.Key!),
              }}
            ></span>
          </div>
          <span
            className="font-medium truncate max-w-xs cursor-help"
            title={extractFileName(object.Key || "", currentPrefix)}
            style={{ color: token('color', 'primaryText') }}
          >
            {extractFileName(object.Key || "", currentPrefix)}
          </span>
        </div>
      </td>
      <td className="px-4 md:px-6 py-4 text-sm hidden md:table-cell" style={{ color: token('color', 'secondaryText') }}>
        {object.LastModified?.toLocaleDateString()}
      </td>
      <td className="px-4 md:px-6 py-4 text-sm hidden lg:table-cell" style={{ color: token('color', 'secondaryText') }}>
        {object.Size ? `${(object.Size / 1024).toFixed(1)} KB` : "—"}
      </td>
      <td className="px-4 md:px-6 py-4">
        <div className="flex items-center space-x-1 sm:space-x-2">
          <button
            onClick={() => onDownload(object.Key!)}
            className="flex items-center justify-center p-2 sm:px-3 sm:py-1.5 rounded-lg transition-colors duration-200 touch-manipulation min-w-[36px] min-h-[36px] sm:min-w-0 sm:min-h-0 group"
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
            title="Download file"
            aria-label="Download file"
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
            <span className="text-sm hidden lg:inline ml-1 group-hover:inline">Download</span>
          </button>
          <button
            onClick={() => onDelete(object.Key!)}
            className="flex items-center justify-center p-2 sm:px-3 sm:py-1.5 rounded-lg transition-colors duration-200 touch-manipulation min-w-[36px] min-h-[36px] sm:min-w-0 sm:min-h-0 group"
            style={{ 
              backgroundColor: 'rgb(254 226 226)', 
              color: 'rgb(185 28 28)' 
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.backgroundColor = 'rgb(252 165 165)';
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = 'rgb(254 226 226)';
            }}
            title="Delete file"
            aria-label="Delete file"
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
            <span className="text-sm hidden lg:inline ml-1 group-hover:inline">Delete</span>
          </button>
        </div>
      </td>
    </tr>
  );
}
