"use client";

import { useTheme } from "../../contexts/ThemeContext";

interface FileExplorerToolbarProps {
  isSelectMode: boolean;
  selectedItemsCount: number;
  onToggleSelectMode: () => void;
  onSelectAll: () => void;
  onClearSelection: () => void;
  onBulkDelete: () => void;
}

export function FileExplorerToolbar({
  isSelectMode,
  selectedItemsCount,
  onToggleSelectMode,
  onSelectAll,
  onClearSelection,
  onBulkDelete,
}: FileExplorerToolbarProps) {
  const { token } = useTheme();

  return (
    <div className="p-4 sm:p-6 border-b" style={{ borderColor: token('color', 'primaryBorder') }}>
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 sm:gap-0">
        <h3 className="text-lg sm:text-xl font-semibold" style={{ color: token('color', 'primaryText') }}>
          Files & Folders
        </h3>
        <div className="flex items-center space-x-2 sm:space-x-3 w-full sm:w-auto">
          {!isSelectMode ? (
            <button
              onClick={onToggleSelectMode}
              className="inline-flex items-center justify-center p-2 sm:p-2.5 text-sm font-medium rounded-lg transition-colors w-full sm:w-auto touch-manipulation min-w-[44px] min-h-[44px] sm:min-w-[40px] sm:min-h-[40px]"
              style={{ 
                color: token('color', 'secondaryText'), 
                backgroundColor: token('color', 'secondaryBg') 
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = token('color', 'hoverBg');
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = token('color', 'secondaryBg');
              }}
              title="Select files and folders"
              aria-label="Enable selection mode"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </button>
          ) : (
            <div className="flex flex-col sm:flex-row items-stretch sm:items-center space-y-2 sm:space-y-0 sm:space-x-2 w-full sm:w-auto">
              <div className="flex items-center justify-between sm:justify-start space-x-2 order-2 sm:order-1">
                <span className="text-sm" style={{ color: token('color', 'secondaryText') }}>
                  {selectedItemsCount} selected
                </span>
                <div className="flex items-center space-x-1">
                  <button
                    onClick={onSelectAll}
                    className="inline-flex items-center justify-center p-1.5 text-xs transition-colors rounded-md touch-manipulation min-w-[36px] min-h-[36px]"
                    style={{ color: token('color', 'focusBorder') }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.color = token('color', 'primaryText');
                      e.currentTarget.style.backgroundColor = token('color', 'hoverBg');
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.color = token('color', 'focusBorder');
                      e.currentTarget.style.backgroundColor = 'transparent';
                    }}
                    title="Select all items"
                    aria-label="Select all items"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                  </button>
                  <button
                    onClick={onClearSelection}
                    className="inline-flex items-center justify-center p-1.5 text-xs transition-colors rounded-md touch-manipulation min-w-[36px] min-h-[36px]"
                    style={{ color: token('color', 'secondaryText') }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.color = token('color', 'primaryText');
                      e.currentTarget.style.backgroundColor = token('color', 'hoverBg');
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.color = token('color', 'secondaryText');
                      e.currentTarget.style.backgroundColor = 'transparent';
                    }}
                    title="Clear selection"
                    aria-label="Clear selection"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                  <button
                    onClick={onToggleSelectMode}
                    className="inline-flex items-center justify-center p-1.5 sm:p-2 text-xs sm:text-sm font-medium rounded-lg transition-colors touch-manipulation min-w-[36px] min-h-[36px]"
                    style={{ 
                      color: token('color', 'secondaryText'), 
                      backgroundColor: token('color', 'secondaryBg') 
                    }}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.backgroundColor = token('color', 'hoverBg');
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.backgroundColor = token('color', 'secondaryBg');
                    }}
                    title="Exit selection mode"
                    aria-label="Exit selection mode"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>
              </div>
              <button
                onClick={onBulkDelete}
                disabled={selectedItemsCount === 0}
                className="inline-flex items-center justify-center p-2.5 sm:p-3 text-sm font-medium text-white bg-red-600 hover:bg-red-700 disabled:bg-red-300 rounded-lg transition-colors w-full sm:w-auto order-1 sm:order-2 touch-manipulation min-h-[44px]"
                title={`Delete ${selectedItemsCount} selected item${selectedItemsCount !== 1 ? 's' : ''}`}
                aria-label={`Delete ${selectedItemsCount} selected item${selectedItemsCount !== 1 ? 's' : ''}`}
              >
                <svg className="w-5 h-5 sm:mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
                <span className="hidden sm:inline ml-2">Delete ({selectedItemsCount})</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
