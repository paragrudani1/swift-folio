"use client";

import { useTheme } from "../../../contexts/ThemeContext";
import { LoadingButton } from "../LoadingButton";

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
      <div className="backdrop-blur-sm rounded-2xl border p-6 w-full max-w-lg" style={{ 
        backgroundColor: token('color', 'glassBg'), 
        boxShadow: token('shadow', '2xl'), 
        borderColor: token('color', 'primaryBorder') 
      }}>
        <h3 className="text-lg font-semibold mb-4" style={{ color: token('color', 'primaryText') }}>
          Confirm Deletion
        </h3>
        <div className="mb-6" style={{ color: token('color', 'secondaryText') }}>
          <p className="mb-3">
            Are you sure you want to delete this {itemToDelete?.endsWith('/') ? 'folder and all its contents' : 'file'}:
          </p>
          <div 
            className="p-3 rounded-lg break-all text-sm font-mono max-h-20 overflow-y-auto"
            style={{ 
              backgroundColor: token('color', 'secondaryBg'),
              color: token('color', 'primaryText'),
              border: `1px solid ${token('color', 'primaryBorder')}`
            }}
          >
            {itemToDelete}
          </div>
          <p className="mt-3 text-sm">
            {itemToDelete?.endsWith('/') 
              ? 'This will permanently delete the folder and all files within it. This action cannot be undone.'
              : 'This action cannot be undone.'
            }
          </p>
        </div>
        <div className="flex justify-end space-x-4">
          <LoadingButton
            isLoading={false}
            onClick={onCancel}
            variant="secondary"
            size="md"
          >
            Cancel
          </LoadingButton>
          <LoadingButton
            isLoading={isLoading}
            onClick={onConfirm}
            variant="danger"
            size="md"
            loadingText="Deleting..."
          >
            Delete
          </LoadingButton>
        </div>
      </div>
    </div>
  );
}
