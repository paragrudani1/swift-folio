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
      <div className="flex items-center justify-between">
        <h3 className="text-lg font-semibold" style={{ color: token('color', 'primaryText') }}>
          Files & Folders
        </h3>
        <div className="flex items-center space-x-3">
          {!isSelectMode ? (
            <button
              onClick={onToggleSelectMode}
              className="inline-flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors"
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
            >
              <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Select
            </button>
          ) : (
            <div className="flex items-center space-x-2">
              <span className="text-sm" style={{ color: token('color', 'secondaryText') }}>
                {selectedItemsCount} selected
              </span>
              <button
                onClick={onSelectAll}
                className="px-2 py-1 text-xs text-blue-600 hover:text-blue-800 transition-colors"
              >
                All
              </button>
              <button
                onClick={onClearSelection}
                className="px-2 py-1 text-xs transition-colors"
                style={{ color: token('color', 'secondaryText') }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.color = token('color', 'primaryText');
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.color = token('color', 'secondaryText');
                }}
              >
                None
              </button>
              <button
                onClick={onBulkDelete}
                disabled={selectedItemsCount === 0}
                className="inline-flex items-center px-3 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 disabled:bg-red-300 rounded-lg transition-colors"
              >
                <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
                Delete ({selectedItemsCount})
              </button>
              <button
                onClick={onToggleSelectMode}
                className="inline-flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors"
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
              >
                Cancel
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
