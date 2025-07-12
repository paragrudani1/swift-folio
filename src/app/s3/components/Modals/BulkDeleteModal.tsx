"use client";

import { useTheme } from "../../../contexts/ThemeContext";
import { IconButton } from "../IconButton";
import { Icons } from "../Icons";

interface BulkDeleteModalProps {
  isOpen: boolean;
  selectedItemsCount: number;
  onConfirm: () => void;
  onCancel: () => void;
  isLoading?: boolean;
}

export function BulkDeleteModal({
  isOpen,
  selectedItemsCount,
  onConfirm,
  onCancel,
  isLoading = false,
}: BulkDeleteModalProps) {
  const { token } = useTheme();

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div 
        className="backdrop-blur-sm rounded-2xl border p-4 sm:p-6 w-full max-w-sm sm:max-w-lg mx-4 sm:mx-0" 
        style={{ 
          backgroundColor: token('color', 'glassBg'), 
          boxShadow: token('shadow', '2xl'), 
          borderColor: token('color', 'primaryBorder') 
        }}
      >
        <h3 className="text-lg sm:text-xl font-semibold mb-4 sm:mb-6" style={{ color: token('color', 'primaryText') }}>
          Confirm Bulk Deletion
        </h3>
        <p className="mb-4 sm:mb-6 text-sm sm:text-base" style={{ color: token('color', 'secondaryText') }}>
          Are you sure you want to delete {selectedItemsCount} selected item{selectedItemsCount !== 1 ? 's' : ''}? This action cannot be undone.
        </p>
        <div className="flex flex-col sm:flex-row justify-end space-y-2 sm:space-y-0 sm:space-x-4">
          <IconButton
            icon={Icons.Clear}
            onClick={onCancel}
            variant="secondary"
            size="md"
            className="w-full sm:w-auto order-2 sm:order-1"
            title="Cancel bulk deletion"
            ariaLabel="Cancel bulk deletion"
          >
            Cancel
          </IconButton>
          <IconButton
            icon={Icons.Delete}
            isLoading={isLoading}
            onClick={onConfirm}
            variant="danger"
            size="md"
            loadingText={`Deleting ${selectedItemsCount} items...`}
            className="w-full sm:w-auto order-1 sm:order-2"
            title={`Delete ${selectedItemsCount} selected item${selectedItemsCount !== 1 ? 's' : ''}`}
            ariaLabel={`Delete ${selectedItemsCount} selected item${selectedItemsCount !== 1 ? 's' : ''}`}
          >
            <span className="hidden sm:inline">Delete {selectedItemsCount} Item{selectedItemsCount !== 1 ? 's' : ''}</span>
            <span className="sm:hidden">Delete ({selectedItemsCount})</span>
          </IconButton>
        </div>
      </div>
    </div>
  );
}
