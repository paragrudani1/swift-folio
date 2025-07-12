"use client";

import { _Object, CommonPrefix } from "@aws-sdk/client-s3";
import { useTheme } from "../../contexts/ThemeContext";
import { FileExplorerToolbar } from "./FileExplorerToolbar";
import { FileTable } from "./FileTable";
import { MobileCardView } from "./MobileCardView";

interface FileExplorerProps {
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
  onToggleSelectMode: () => void;
  onSelectAll: () => void;
  onClearSelection: () => void;
  onBulkDelete: () => void;
  onDownload: (key: string) => void;
  onDelete: (key: string) => void;
}

export function FileExplorer({
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
  onToggleSelectMode,
  onSelectAll,
  onClearSelection,
  onBulkDelete,
  onDownload,
  onDelete,
}: FileExplorerProps) {
  const { token } = useTheme();

  return (
    <div className="backdrop-blur-sm rounded-2xl border overflow-hidden" style={{ 
      backgroundColor: token('color', 'overlayBg'), 
      boxShadow: token('shadow', 'lg'), 
      borderColor: token('color', 'primaryBorder') 
    }}>
      {/* Toolbar */}
      <FileExplorerToolbar
        isSelectMode={isSelectMode}
        selectedItemsCount={selectedItems.size}
        onToggleSelectMode={onToggleSelectMode}
        onSelectAll={onSelectAll}
        onClearSelection={onClearSelection}
        onBulkDelete={onBulkDelete}
      />

      {/* Desktop Table View */}
      <FileTable
        objects={objects}
        prefixes={prefixes}
        filteredObjects={filteredObjects}
        filteredPrefixes={filteredPrefixes}
        isSelectMode={isSelectMode}
        selectedItems={selectedItems}
        currentPrefix={currentPrefix}
        onItemSelection={onItemSelection}
        onPrefixClick={onPrefixClick}
        onRequestSort={onRequestSort}
        onSelectAll={onSelectAll}
        onClearSelection={onClearSelection}
        onDownload={onDownload}
        onDelete={onDelete}
      />

      {/* Mobile Card View */}
      <MobileCardView
        filteredObjects={filteredObjects}
        filteredPrefixes={filteredPrefixes}
        isSelectMode={isSelectMode}
        selectedItems={selectedItems}
        currentPrefix={currentPrefix}
        onItemSelection={onItemSelection}
        onPrefixClick={onPrefixClick}
        onDownload={onDownload}
        onDelete={onDelete}
      />
    </div>
  );
}
