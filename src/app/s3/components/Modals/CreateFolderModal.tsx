"use client";

import { useTheme } from "../../../contexts/ThemeContext";

interface CreateFolderModalProps {
  isOpen: boolean;
  newFolderName: string;
  onFolderNameChange: (name: string) => void;
  onConfirm: () => void;
  onCancel: () => void;
}

export function CreateFolderModal({
  isOpen,
  newFolderName,
  onFolderNameChange,
  onConfirm,
  onCancel,
}: CreateFolderModalProps) {
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
          Create New Folder
        </h3>
        <input
          type="text"
          placeholder="Folder name"
          value={newFolderName}
          onChange={(e) => onFolderNameChange(e.target.value)}
          className="w-full px-4 py-3 border rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200"
          style={{ 
            backgroundColor: token('color', 'glassBg'), 
            borderColor: token('color', 'primaryBorder'), 
            color: token('color', 'primaryText') 
          }}
          onKeyDown={(e) => e.key === "Enter" && onConfirm()}
        />
        <div className="flex justify-end space-x-4 mt-6">
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
            className="px-4 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 text-white rounded-lg hover:from-blue-700 hover:to-indigo-700 transition-all duration-200"
            style={{ boxShadow: token('shadow', 'lg') }}
            onMouseEnter={(e) => {
              e.currentTarget.style.boxShadow = token('shadow', 'xl');
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.boxShadow = token('shadow', 'lg');
            }}
          >
            Create
          </button>
        </div>
      </div>
    </div>
  );
}
