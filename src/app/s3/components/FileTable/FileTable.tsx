"use client";

import { _Object, CommonPrefix } from "@aws-sdk/client-s3";
import { useTheme } from "../../../contexts/ThemeContext";
import { FileTableHeader } from "./FileTableHeader";
import { FolderRow } from "./FolderRow";
import { FileRow } from "./FileRow";

interface FileTableProps {
  objects: _Object[];
  prefixes: CommonPrefix[];
  filteredObjects: _Object[];
  filteredPrefixes: Array<CommonPrefix & { folderName: string }>;
  isSelectMode: boolean;
  selectedItems: Set<string>;
  currentPrefix: string;
  onItemSelection: (key: string) => void;
  onPrefixClick: (prefix: string) => void;
  onRequestSort: (key: keyof _Object) => void;
  onSelectAll: () => void;
  onClearSelection: () => void;
  onDownload: (key: string) => void;
  onDelete: (key: string) => void;
}

export function FileTable({
  objects,
  prefixes,
  filteredObjects,
  filteredPrefixes,
  isSelectMode,
  selectedItems,
  currentPrefix,
  onItemSelection,
  onPrefixClick,
  onRequestSort,
  onSelectAll,
  onClearSelection,
  onDownload,
  onDelete,
}: FileTableProps) {
  const { token } = useTheme();

  return (
    <div className="hidden sm:block overflow-x-auto border rounded-lg" style={{ borderColor: token('color', 'primaryBorder') }}>
      <div className="min-w-full md:min-w-0">
        <table className="w-full min-w-[640px] md:min-w-full" style={{ backgroundColor: token('color', 'primaryBg') }}>
          <FileTableHeader
            isSelectMode={isSelectMode}
            selectedItems={selectedItems}
            totalItems={objects.length + prefixes.length}
            onRequestSort={onRequestSort}
            onSelectAll={onSelectAll}
            onClearSelection={onClearSelection}
          />
        <tbody className="divide-y" style={{ borderColor: token('color', 'primaryBorder') }}>
          {/* Folders */}
          {filteredPrefixes.map((item) => (
            <FolderRow
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
            <FileRow
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
        </tbody>
        </table>
      </div>
    </div>
  );
}
