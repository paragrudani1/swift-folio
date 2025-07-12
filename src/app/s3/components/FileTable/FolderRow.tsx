"use client";

import { CommonPrefix } from "@aws-sdk/client-s3";
import { useTheme } from "../../../contexts/ThemeContext";

interface FolderRowProps {
  prefix: CommonPrefix;
  folderName: string;
  isSelectMode: boolean;
  isSelected: boolean;
  onItemSelection: (key: string) => void;
  onPrefixClick: (prefix: string) => void;
}

export function FolderRow({
  prefix,
  folderName,
  isSelectMode,
  isSelected,
  onItemSelection,
  onPrefixClick,
}: FolderRowProps) {
  const { token } = useTheme();

  return (
    <tr
      className="cursor-pointer transition-all duration-200 hover:shadow-sm border-b"
      style={{ 
        borderBottomColor: token('color', 'primaryBorder'),
        backgroundColor: isSelected ? token('color', 'overlayBg') : 'transparent'
      }}
      onClick={(e) => {
        // Don't trigger if clicking on checkbox
        if (isSelectMode && (e.target as HTMLInputElement).type === 'checkbox') {
          return;
        }
        if (!isSelectMode) {
          onPrefixClick(prefix.Prefix!);
        }
      }}
      onMouseEnter={(e) => {
        if (!isSelected) {
          e.currentTarget.style.backgroundColor = token('color', 'hoverBg');
        }
      }}
      onMouseLeave={(e) => {
        if (!isSelected) {
          e.currentTarget.style.backgroundColor = 'transparent';
        } else {
          e.currentTarget.style.backgroundColor = token('color', 'overlayBg');
        }
      }}
    >
      {isSelectMode && (
        <td className="w-12 px-4 py-4 md:px-6">
          <div className="flex items-center justify-center">
            <input
              type="checkbox"
              checked={isSelected}
              onChange={(e) => {
                e.stopPropagation();
                onItemSelection(prefix.Prefix!);
              }}
              className="w-5 h-5 md:w-4 md:h-4 text-blue-600 rounded focus:ring-blue-500 focus:ring-2 cursor-pointer transition-all"
              style={{ 
                backgroundColor: token('color', 'primaryBg'), 
                borderColor: token('color', 'primaryBorder') 
              }}
            />
          </div>
        </td>
      )}
      <td className="min-w-[200px] px-4 md:px-6 py-4 touch-manipulation">
        <div className="flex items-center space-x-3">
          <div 
            className="w-8 h-8 rounded-lg flex items-center justify-center shadow-sm"
            style={{ 
              backgroundColor: token('color', 'tertiaryBg'),
              border: `1px solid ${token('color', 'primaryBorder')}`
            }}
          >
            <svg
              className="w-5 h-5"
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
          <span className="font-medium" style={{ color: token('color', 'primaryText') }}>
            {folderName || "Unnamed Folder"}
          </span>
        </div>
      </td>
      <td className="w-32 sm:w-36 md:w-44 px-4 md:px-6 py-4 hidden md:table-cell" style={{ color: token('color', 'mutedText') }}>—</td>
      <td className="w-24 sm:w-28 md:w-32 px-4 md:px-6 py-4 hidden lg:table-cell" style={{ color: token('color', 'mutedText') }}>—</td>
      <td className="w-32 sm:w-36 md:w-40 px-4 md:px-6 py-4 text-right" style={{ color: token('color', 'mutedText') }}></td>
    </tr>
  );
}
