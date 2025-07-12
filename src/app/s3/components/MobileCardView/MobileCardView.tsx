"use client";

import { _Object, CommonPrefix } from "@aws-sdk/client-s3";
import { useTheme } from "../../../contexts/ThemeContext";
import { FolderCard } from "./FolderCard";
import { FileCard } from "./FileCard";

interface MobileCardViewProps {
  filteredObjects: _Object[];
  filteredPrefixes: Array<CommonPrefix & { folderName: string }>;
  isSelectMode: boolean;
  selectedItems: Set<string>;
  currentPrefix: string;
  onItemSelection: (key: string) => void;
  onPrefixClick: (prefix: string) => void;
  onDownload: (key: string) => void;
  onDelete: (key: string) => void;
}

export function MobileCardView({
  filteredObjects,
  filteredPrefixes,
  isSelectMode,
  selectedItems,
  currentPrefix,
  onItemSelection,
  onPrefixClick,
  onDownload,
  onDelete,
}: MobileCardViewProps) {
  const { token } = useTheme();

  return (
    <div
      className="sm:hidden"
      style={{ borderColor: token("color", "primaryBorder") }}
    >
      {/* Folders */}
      {filteredPrefixes.map((item) => (
        <FolderCard
          key={item.Prefix}
          prefix={item}
          folderName={item.folderName}
          isSelectMode={isSelectMode}
          isSelected={selectedItems.has(item.Prefix!)}
          onItemSelection={onItemSelection}
          onPrefixClick={onPrefixClick}
        />
      ))}

      {/* Files */}
      {filteredObjects.map((object) => (
        <FileCard
          key={object.Key}
          object={object}
          currentPrefix={currentPrefix}
          isSelectMode={isSelectMode}
          isSelected={selectedItems.has(object.Key!)}
          onItemSelection={onItemSelection}
          onDownload={onDownload}
          onDelete={onDelete}
        />
      ))}
    </div>
  );
}
