"use client";

import { useTheme } from "../../../contexts/ThemeContext";
import { IconButton } from "../IconButton";
import { Icons } from "../Icons";

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
      <div 
        className="backdrop-blur-sm rounded-2xl border p-4 sm:p-6 w-full max-w-sm sm:max-w-lg mx-4 sm:mx-0" 
        style={{ 
          backgroundColor: token('color', 'glassBg'), 
          boxShadow: token('shadow', '2xl'), 
          borderColor: token('color', 'primaryBorder') 
        }}
      >
        <h3 className="text-lg sm:text-xl font-semibold mb-4 sm:mb-6" style={{ color: token('color', 'primaryText') }}>
          Create New Folder
        </h3>
        <input
          type="text"
          placeholder="Folder name"
          value={newFolderName}
          onChange={(e) => onFolderNameChange(e.target.value)}
          className="w-full px-3 sm:px-4 py-2.5 sm:py-3 border rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200 text-sm sm:text-base"
          style={{ 
            backgroundColor: token('color', 'glassBg'), 
            borderColor: token('color', 'primaryBorder'), 
            color: token('color', 'primaryText') 
          }}
          onKeyDown={(e) => e.key === "Enter" && onConfirm()}
          autoFocus
        />
        <div className="flex flex-col sm:flex-row justify-end space-y-2 sm:space-y-0 sm:space-x-4 mt-4 sm:mt-6">
          <IconButton
            icon={Icons.Clear}
            onClick={onCancel}
            variant="secondary"
            size="md"
            className="w-full sm:w-auto order-2 sm:order-1"
            title="Cancel folder creation"
            ariaLabel="Cancel folder creation"
          >
            Cancel
          </IconButton>
          <IconButton
            icon={Icons.Create}
            isLoading={isLoading}
            onClick={onConfirm}
            variant="primary"
            size="md"
            loadingText="Creating..."
            disabled={!newFolderName.trim()}
            className="w-full sm:w-auto order-1 sm:order-2"
            title="Create new folder"
            ariaLabel="Create new folder"
          >
            Create Folder
          </IconButton>
        </div>
      </div>
    </div>
  );
}
