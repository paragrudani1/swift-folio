"use client";

import { useTheme } from "../../../contexts/ThemeContext";

interface DeleteModalProps {
  isOpen: boolean;
  itemToDelete: string | null;
  onConfirm: () => void;
  onCancel: () => void;
}

export function DeleteModal({
  isOpen,
  itemToDelete,
  onConfirm,
  onCancel,
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
          <p className="mb-3">Are you sure you want to delete:</p>
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
          <p className="mt-3 text-sm">This action cannot be undone.</p>
        </div>
        <div className="flex justify-end space-x-4">
          <button
            onClick={onCancel}
            className="px-4 py-2 rounded-lg transition-colors duration-200"
            style={{ 
              backgroundColor: token('color', 'secondaryBg'), 
              color: token('color', 'secondaryText') 
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
          <button
            onClick={onConfirm}
            className="px-4 py-2 bg-gradient-to-r from-red-600 to-rose-600 text-white rounded-lg hover:from-red-700 hover:to-rose-700 transition-all duration-200"
            style={{ boxShadow: token('shadow', 'lg') }}
            onMouseEnter={(e) => {
              e.currentTarget.style.boxShadow = token('shadow', 'xl');
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.boxShadow = token('shadow', 'lg');
            }}
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  );
}
