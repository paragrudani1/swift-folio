"use client";

import { CommonPrefix } from "@aws-sdk/client-s3";
import { useTheme } from "../../../contexts/ThemeContext";
import { Icons } from "../Icons";

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
      className="p-4 cursor-pointer transition-colors duration-200 touch-manipulation"
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
            className="w-5 h-5 text-blue-600 rounded focus:ring-blue-500 flex-shrink-0 cursor-pointer"
            style={{ 
              backgroundColor: token('color', 'primaryBg'), 
              borderColor: token('color', 'primaryBorder') 
            }}
          />
        )}
        <div 
          className="w-12 h-12 rounded-lg flex items-center justify-center flex-shrink-0 touch-manipulation"
          onClick={() => !isSelectMode && onPrefixClick(prefix.Prefix!)}
          style={{ 
            backgroundColor: token('color', 'tertiaryBg'),
          }}
        >
          <div className="w-6 h-6" style={{ color: token('color', 'focusBorder') }}>
            {Icons.Folder}
          </div>
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
