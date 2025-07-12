"use client";

import { useTheme } from "../../../contexts/ThemeContext";
import { LoadingButton } from "../LoadingButton";

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
      <div className="backdrop-blur-sm rounded-2xl border p-6 w-full max-w-lg" style={{ 
        backgroundColor: token('color', 'glassBg'), 
        boxShadow: token('shadow', '2xl'), 
        borderColor: token('color', 'primaryBorder') 
      }}>
        <h3 className="text-lg font-semibold mb-4" style={{ color: token('color', 'primaryText') }}>
          Confirm Bulk Deletion
        </h3>
        <p className="mb-6" style={{ color: token('color', 'secondaryText') }}>
          Are you sure you want to delete {selectedItemsCount} selected item{selectedItemsCount !== 1 ? 's' : ''}? This action cannot be undone.
        </p>
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
            loadingText={`Deleting ${selectedItemsCount} items...`}
          >
            Delete {selectedItemsCount} Item{selectedItemsCount !== 1 ? 's' : ''}
          </LoadingButton>
        </div>
      </div>
    </div>
  );
}
