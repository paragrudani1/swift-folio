"use client";

import { useTheme } from "../../contexts/ThemeContext";

interface BreadcrumbProps {
  selectedBucket: string;
  currentPrefix: string;
  searchTerm: string;
  onSearchChange: (value: string) => void;
  onBackClick: () => void;
  onCreateFolderClick: () => void;
}

export function Breadcrumb({
  selectedBucket,
  currentPrefix,
  searchTerm,
  onSearchChange,
  onBackClick,
  onCreateFolderClick,
}: BreadcrumbProps) {
  const { token } = useTheme();

  return (
    <div className="backdrop-blur-sm rounded-2xl border p-4 sm:p-6" style={{ 
      backgroundColor: token('color', 'glassBg'), 
      boxShadow: token('shadow', 'lg'), 
      borderColor: token('color', 'primaryBorder') 
    }}>
      <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        {/* Breadcrumb path */}
        <div className="flex items-center space-x-2 text-sm overflow-x-auto w-full md:w-auto">
          <span className="px-3 py-1 bg-blue-100 text-blue-800 rounded-lg font-medium whitespace-nowrap">
            {selectedBucket}
          </span>
          {currentPrefix && (
            <>
              <span style={{ color: token('color', 'mutedText') }}>/</span>
              <span className="px-3 py-1 rounded-lg whitespace-nowrap" style={{ 
                backgroundColor: token('color', 'secondaryBg'), 
                color: token('color', 'primaryText') 
              }}>
                {currentPrefix}
              </span>
            </>
          )}
        </div>

        {/* Actions */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center space-y-2 sm:space-y-0 sm:space-x-2 md:space-x-3 w-full md:w-auto">
          {/* Back button */}
          {currentPrefix && (
            <button
              onClick={onBackClick}
              className="flex items-center justify-center p-2.5 rounded-lg transition-colors duration-200 text-sm touch-manipulation min-w-[44px] min-h-[44px] sm:min-w-[40px] sm:min-h-[40px]"
              style={{ 
                backgroundColor: token('color', 'overlayBg'), 
                color: token('color', 'secondaryText') 
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = token('color', 'hoverBg');
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = token('color', 'overlayBg');
              }}
              title="Go back to parent folder"
              aria-label="Go back to parent folder"
            >
              <svg
                className="w-5 h-5"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M15 19l-7-7 7-7"
                />
              </svg>
            </button>
          )}

          {/* Search input */}
          <div className="relative flex-1 sm:flex-initial order-2 sm:order-2">
            <input
              type="text"
              placeholder="Search files..."
              value={searchTerm}
              onChange={(e) => onSearchChange(e.target.value)}
              className="w-full sm:w-64 md:w-80 pl-8 sm:pl-10 pr-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200"
              style={{ 
                backgroundColor: token('color', 'glassBg'), 
                borderColor: token('color', 'primaryBorder'), 
                color: token('color', 'primaryText') 
              }}
            />
            <svg
              className="w-4 h-4 absolute left-2 sm:left-3 top-3"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              style={{ color: token('color', 'tertiaryText') }}
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
          </div>

          {/* New Folder button */}
          <button
            onClick={onCreateFolderClick}
            className="flex items-center justify-center p-2.5 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-lg hover:from-green-700 hover:to-emerald-700 transition-all duration-200 text-sm whitespace-nowrap touch-manipulation min-w-[44px] min-h-[44px] sm:min-w-[40px] sm:min-h-[40px] order-1 sm:order-3"
            style={{ boxShadow: token('shadow', 'lg') }}
            onMouseEnter={(e) => {
              e.currentTarget.style.boxShadow = token('shadow', 'xl');
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.boxShadow = token('shadow', 'lg');
            }}
            title="Create new folder"
            aria-label="Create new folder"
          >
            <svg
              className="w-5 h-5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth="2"
                d="M12 6v6m0 0v6m0-6h6m-6 0H6"
              />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
}
