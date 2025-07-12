"use client";

import { useTheme } from "../../../contexts/ThemeContext";
import { LoadingButton } from "../LoadingButton";

interface CreateFolderModalProps {
  isOpen: boolean;
  newFolderName: string;
  onFolderNameChange: (name: string) => void;
  onConfirm: () => void;
  onCancel: () => void;
  isLoading?: boolean;
}

export function CreateFolderModal({
  isOpen,
  newFolderName,
  onFolderNameChange,
  onConfirm,
  onCancel,
  isLoading = false,
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
            variant="primary"
            size="md"
            loadingText="Creating..."
            disabled={!newFolderName.trim()}
          >
            Create
          </LoadingButton>
        </div>
      </div>
    </div>
  );
}
