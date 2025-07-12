"use client";

import { _Object } from "@aws-sdk/client-s3";
import { useTheme } from "../../../contexts/ThemeContext";

interface FileTableHeaderProps {
  isSelectMode: boolean;
  selectedItems: Set<string>;
  totalItems: number;
  onRequestSort: (key: keyof _Object) => void;
  onSelectAll: () => void;
  onClearSelection: () => void;
}

export function FileTableHeader({
  isSelectMode,
  selectedItems,
  totalItems,
  onRequestSort,
  onSelectAll,
  onClearSelection,
}: FileTableHeaderProps) {
  const { token } = useTheme();

  return (
    <thead>
      <tr 
        className="border-b shadow-sm" 
        style={{ 
          backgroundColor: token('color', 'secondaryBg'),
          borderColor: token('color', 'primaryBorder') 
        }}
      >
        {isSelectMode && (
          <th className="w-12 px-4 py-3 md:px-6 md:py-4">
            <div className="flex items-center justify-center">
              <input
                type="checkbox"
                checked={selectedItems.size > 0 && selectedItems.size === totalItems}
                onChange={(e) => e.target.checked ? onSelectAll() : onClearSelection()}
                className="w-5 h-5 md:w-4 md:h-4 text-blue-600 rounded focus:ring-blue-500 focus:ring-2 cursor-pointer transition-all"
                style={{ 
                  backgroundColor: token('color', 'primaryBg'), 
                  borderColor: token('color', 'primaryBorder') 
                }}
              />
            </div>
          </th>
        )}
        <th
          className="min-w-[200px] px-4 md:px-6 py-3 md:py-4 text-left text-xs md:text-sm font-semibold uppercase tracking-wide cursor-pointer transition-colors touch-manipulation"
          onClick={() => onRequestSort("Key")}
          style={{ color: token('color', 'secondaryText') }}
          onMouseEnter={(e) => {
            e.currentTarget.style.color = token('color', 'primaryText');
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.color = token('color', 'secondaryText');
          }}
        >
          <div className="flex items-center space-x-1 md:space-x-2">
            <span>Name</span>
            <svg
              className="w-3 h-3 md:w-4 md:h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4"
              />
            </svg>
          </div>
        </th>
        <th
          className="w-32 sm:w-36 md:w-44 px-4 md:px-6 py-3 md:py-4 text-left text-xs md:text-sm font-semibold uppercase tracking-wide cursor-pointer transition-colors touch-manipulation hidden md:table-cell"
          onClick={() => onRequestSort("LastModified")}
          style={{ color: token('color', 'secondaryText') }}
          onMouseEnter={(e) => {
            e.currentTarget.style.color = token('color', 'primaryText');
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.color = token('color', 'secondaryText');
          }}
        >
          <div className="flex items-center space-x-1 md:space-x-2">
            <span>Modified</span>
            <svg
              className="w-3 h-3 md:w-4 md:h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4"
              />
            </svg>
          </div>
        </th>
        <th
          className="w-24 sm:w-28 md:w-32 px-4 md:px-6 py-3 md:py-4 text-left text-xs md:text-sm font-semibold uppercase tracking-wide cursor-pointer transition-colors touch-manipulation hidden lg:table-cell"
          onClick={() => onRequestSort("Size")}
          style={{ color: token('color', 'secondaryText') }}
          onMouseEnter={(e) => {
            e.currentTarget.style.color = token('color', 'primaryText');
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.color = token('color', 'secondaryText');
          }}
        >
          <div className="flex items-center space-x-1 md:space-x-2">
            <span>Size</span>
            <svg
              className="w-3 h-3 md:w-4 md:h-4"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4"
              />
            </svg>
          </div>
        </th>
        <th className="w-32 sm:w-36 md:w-40 px-4 md:px-6 py-3 md:py-4 text-right text-xs md:text-sm font-semibold uppercase tracking-wide" style={{ color: token('color', 'secondaryText') }}>
          <span className="sr-only sm:not-sr-only">Actions</span>
        </th>
      </tr>
    </thead>
  );
}
