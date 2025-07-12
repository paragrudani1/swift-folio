"use client";

import { _Object } from "@aws-sdk/client-s3";
import { useTheme } from "../../../contexts/ThemeContext";
import { extractFileName, getFileIcon, isValidFile } from "../../utils";
import { IconButton } from "../IconButton";
import { Icons } from "../Icons";

interface FileCardProps {
  object: _Object;
  currentPrefix: string;
  isSelectMode: boolean;
  isSelected: boolean;
  onItemSelection: (key: string) => void;
  onDownload: (key: string) => void;
  onDelete: (key: string) => void;
}

export function FileCard({
  object,
  currentPrefix,
  isSelectMode,
  isSelected,
  onItemSelection,
  onDownload,
  onDelete,
}: FileCardProps) {
  const { token } = useTheme();

  if (!isValidFile(object.Key || "")) {
    return null;
  }

  const fileName = extractFileName(object.Key || "", currentPrefix);

  return (
    <div 
      key={object.Key} 
      className="p-4" 
      style={{ 
        borderBottomColor: token('color', 'primaryBorder'), 
        borderBottomWidth: '1px' 
      }}
    >
      <div className="flex items-center space-x-3">
        {isSelectMode && (
          <input
            type="checkbox"
            checked={isSelected}
            onChange={() => onItemSelection(object.Key!)}
            className="w-5 h-5 text-blue-600 rounded focus:ring-blue-500 flex-shrink-0 cursor-pointer"
            style={{ 
              backgroundColor: token('color', 'primaryBg'), 
              borderColor: token('color', 'primaryBorder') 
            }}
          />
        )}
        <div 
          className="w-12 h-12 rounded-lg flex items-center justify-center flex-shrink-0" 
          style={{ backgroundColor: token('color', 'secondaryBg') }}
        >
          <span
            className="text-xl"
            dangerouslySetInnerHTML={{
              __html: getFileIcon(object.Key!),
            }}
          ></span>
        </div>
        <div className="flex-1 min-w-0">
          <p
            className="font-medium truncate"
            title={fileName}
            style={{ color: token('color', 'primaryText') }}
          >
            {fileName}
          </p>
          <p className="text-sm" style={{ color: token('color', 'mutedText') }}>
            {object.LastModified?.toLocaleDateString()} &middot;{" "}
            {object.Size
              ? `${(object.Size / 1024).toFixed(1)} KB`
              : "—"}
          </p>
        </div>
        <div className="flex items-center space-x-2 flex-shrink-0">
          <IconButton
            icon={Icons.Download}
            onClick={() => onDownload(object.Key!)}
            variant="success"
            size="md"
            iconOnly={true}
            title="Download file"
            ariaLabel="Download file"
          />
          <IconButton
            icon={Icons.Delete}
            onClick={() => onDelete(object.Key!)}
            variant="danger"
            size="md"
            iconOnly={true}
            title="Delete file"
            ariaLabel="Delete file"
          />
        </div>
      </div>
    </div>
  );
}
