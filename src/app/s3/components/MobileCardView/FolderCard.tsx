"use client";

import { CommonPrefix } from "@aws-sdk/client-s3";
import { useTheme } from "../../../contexts/ThemeContext";

interface FolderCardProps {
  prefix: CommonPrefix;
  folderName: string;
  isSelectMode: boolean;
  isSelected: boolean;
  onItemSelection: (key: string) => void;
  onPrefixClick: (prefix: string) => void;
}

export function FolderCard({
  prefix,
  folderName,
  isSelectMode,
  isSelected,
  onItemSelection,
  onPrefixClick,
}: FolderCardProps) {
  const { token } = useTheme();

  return (
    <div
      className="p-4 cursor-pointer transition-colors duration-200"
      style={{ borderBottomColor: token('color', 'primaryBorder'), borderBottomWidth: '1px' }}
      onMouseEnter={(e) => {
        e.currentTarget.style.backgroundColor = token('color', 'hoverBg');
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.backgroundColor = 'transparent';
      }}
    >
      <div className="flex items-center space-x-3">
        {isSelectMode && (
          <input
            type="checkbox"
            checked={isSelected}
            onChange={(e) => {
              e.stopPropagation();
              onItemSelection(prefix.Prefix!);
            }}
            className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500 flex-shrink-0"
            style={{ 
              backgroundColor: token('color', 'primaryBg'), 
              borderColor: token('color', 'primaryBorder') 
            }}
          />
        )}
        <div 
          className="w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0"
          onClick={() => !isSelectMode && onPrefixClick(prefix.Prefix!)}
          style={{ 
            backgroundColor: token('color', 'tertiaryBg'),
          }}
        >
          <svg
            className="w-6 h-6"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
            style={{ color: token('color', 'focusBorder') }}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="2"
              d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2z"
            />
          </svg>
        </div>
        <span 
          className="font-medium truncate"
          onClick={() => !isSelectMode && onPrefixClick(prefix.Prefix!)}
          style={{ color: token('color', 'primaryText') }}
        >
          {folderName || "Unnamed Folder"}
        </span>
      </div>
    </div>
  );
}
