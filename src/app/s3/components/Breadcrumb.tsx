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
    <div className="backdrop-blur-sm rounded-2xl border p-3 sm:p-4 lg:p-6" style={{ 
      backgroundColor: token('color', 'glassBg'), 
      boxShadow: token('shadow', 'lg'), 
      borderColor: token('color', 'primaryBorder') 
    }}>
      {/* Mobile: Stack everything vertically with better spacing */}
      <div className="flex flex-col space-y-3 sm:hidden">
        {/* Mobile breadcrumb path */}
        <div className="flex items-center space-x-2 text-sm overflow-x-auto">
          <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded-md font-medium whitespace-nowrap text-xs">
            {selectedBucket}
          </span>
          {currentPrefix && (
            <>
              <span style={{ color: token('color', 'mutedText') }}>/</span>
              <span className="px-2 py-1 rounded-md whitespace-nowrap text-xs" style={{ 
                backgroundColor: token('color', 'secondaryBg'), 
                color: token('color', 'primaryText') 
              }}>
                {currentPrefix}
              </span>
            </>
          )}
        </div>

        {/* Mobile actions row */}
        <div className="flex items-center space-x-2">
          {currentPrefix && (
            <button
              onClick={onBackClick}
              className="flex items-center justify-center p-2 rounded-lg transition-colors duration-200 touch-manipulation min-w-[44px] min-h-[44px] flex-shrink-0"
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
              title="Go back"
              aria-label="Go back to parent folder"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
          )}

          <div className="relative flex-1">
            <input
              type="text"
              placeholder="Search files..."
              value={searchTerm}
              onChange={(e) => onSearchChange(e.target.value)}
              className="w-full pl-9 pr-3 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 text-sm"
              style={{ 
                backgroundColor: token('color', 'glassBg'), 
                borderColor: token('color', 'primaryBorder'), 
                color: token('color', 'primaryText') 
              }}
            />
            <svg
              className="w-4 h-4 absolute left-2.5 top-2.5"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              style={{ color: token('color', 'tertiaryText') }}
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>

          <button
            onClick={onCreateFolderClick}
            className="flex items-center justify-center p-2 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-lg hover:from-green-700 hover:to-emerald-700 transition-all duration-200 touch-manipulation min-w-[44px] min-h-[44px] flex-shrink-0"
            style={{ boxShadow: token('shadow', 'lg') }}
            onMouseEnter={(e) => {
              e.currentTarget.style.boxShadow = token('shadow', 'xl');
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.boxShadow = token('shadow', 'lg');
            }}
            title="Create folder"
            aria-label="Create new folder"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
            </svg>
          </button>
        </div>
      </div>

      {/* Tablet and Desktop: Horizontal layout */}
      <div className="hidden sm:flex items-center justify-between gap-4">
        {/* Breadcrumb path */}
        <div className="flex items-center space-x-2 text-sm overflow-x-auto flex-shrink-0">
          <span className="px-3 py-1.5 bg-blue-100 text-blue-800 rounded-lg font-medium whitespace-nowrap">
            {selectedBucket}
          </span>
          {currentPrefix && (
            <>
              <span style={{ color: token('color', 'mutedText') }}>/</span>
              <span className="px-3 py-1.5 rounded-lg whitespace-nowrap" style={{ 
                backgroundColor: token('color', 'secondaryBg'), 
                color: token('color', 'primaryText') 
              }}>
                {currentPrefix}
              </span>
            </>
          )}
        </div>

        {/* Actions */}
        <div className="flex items-center space-x-3 flex-shrink-0">
          {/* Back button */}
          {currentPrefix && (
            <button
              onClick={onBackClick}
              className="flex items-center justify-center p-2.5 rounded-lg transition-colors duration-200 touch-manipulation min-w-[40px] min-h-[40px]"
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
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" />
              </svg>
            </button>
          )}

          {/* Search input */}
          <div className="relative">
            <input
              type="text"
              placeholder="Search files..."
              value={searchTerm}
              onChange={(e) => onSearchChange(e.target.value)}
              className="w-48 lg:w-64 xl:w-80 pl-10 pr-4 py-2.5 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200"
              style={{ 
                backgroundColor: token('color', 'glassBg'), 
                borderColor: token('color', 'primaryBorder'), 
                color: token('color', 'primaryText') 
              }}
            />
            <svg
              className="w-4 h-4 absolute left-3 top-3"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
              style={{ color: token('color', 'tertiaryText') }}
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>

          {/* New Folder button */}
          <button
            onClick={onCreateFolderClick}
            className="flex items-center justify-center p-2.5 bg-gradient-to-r from-green-600 to-emerald-600 text-white rounded-lg hover:from-green-700 hover:to-emerald-700 transition-all duration-200 touch-manipulation min-w-[40px] min-h-[40px]"
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
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  );
}
