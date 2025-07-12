"use client";

import { useTheme } from "../../../contexts/ThemeContext";
import { IconButton } from "../IconButton";
import { Icons } from "../Icons";

interface DeleteModalProps {
  isOpen: boolean;
  itemToDelete: string | null;
  onConfirm: () => void;
  onCancel: () => void;
  isLoading?: boolean;
}

export function DeleteModal({
  isOpen,
  itemToDelete,
  onConfirm,
  onCancel,
  isLoading = false,
}: DeleteModalProps) {
  const { token } = useTheme();

  if (!isOpen || !itemToDelete) return null;

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
          Confirm Deletion
        </h3>
        <div className="mb-4 sm:mb-6" style={{ color: token('color', 'secondaryText') }}>
          <p className="mb-3 text-sm sm:text-base">
            Are you sure you want to delete this {itemToDelete?.endsWith('/') ? 'folder and all its contents' : 'file'}:
          </p>
          <div 
            className="p-2 sm:p-3 rounded-lg break-all text-xs sm:text-sm font-mono max-h-16 sm:max-h-20 overflow-y-auto"
            style={{ 
              backgroundColor: token('color', 'secondaryBg'),
              color: token('color', 'primaryText'),
              border: `1px solid ${token('color', 'primaryBorder')}`
            }}
          >
            {itemToDelete}
          </div>
          <p className="mt-3 text-xs sm:text-sm">
            {itemToDelete?.endsWith('/') 
              ? 'This will permanently delete the folder and all files within it. This action cannot be undone.'
              : 'This action cannot be undone.'
            }
          </p>
        </div>
        <div className="flex flex-col sm:flex-row justify-end space-y-2 sm:space-y-0 sm:space-x-4">
          <IconButton
            icon={Icons.Clear}
            onClick={onCancel}
            variant="secondary"
            size="md"
            className="w-full sm:w-auto order-2 sm:order-1"
            title="Cancel deletion"
            ariaLabel="Cancel deletion"
          >
            Cancel
          </IconButton>
          <IconButton
            icon={Icons.Delete}
            isLoading={isLoading}
            onClick={onConfirm}
            variant="danger"
            size="md"
            loadingText="Deleting..."
            className="w-full sm:w-auto order-1 sm:order-2"
            title="Confirm deletion"
            ariaLabel="Confirm deletion"
          >
            Delete
          </IconButton>
        </div>
      </div>
    </div>
  );
}
