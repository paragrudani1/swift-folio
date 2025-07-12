import { useState } from "react";

/**
 * Custom hook for managing item selection in the S3 Explorer
 */
export const useSelection = () => {
  const [selectedItems, setSelectedItems] = useState<Set<string>>(new Set());
  const [isSelectMode, setIsSelectMode] = useState(false);

  const toggleSelectMode = () => {
    setIsSelectMode(!isSelectMode);
    setSelectedItems(new Set());
  };

  const toggleItemSelection = (key: string) => {
    const newSelectedItems = new Set(selectedItems);
    if (newSelectedItems.has(key)) {
      newSelectedItems.delete(key);
    } else {
      newSelectedItems.add(key);
    }
    setSelectedItems(newSelectedItems);
  };

  const selectAllItems = (allKeys: string[]) => {
    setSelectedItems(new Set(allKeys));
  };

  const clearSelection = () => {
    setSelectedItems(new Set());
  };

  const exitSelectMode = () => {
    setIsSelectMode(false);
    setSelectedItems(new Set());
  };

  return {
    selectedItems,
    isSelectMode,
    toggleSelectMode,
    toggleItemSelection,
    selectAllItems,
    clearSelection,
    exitSelectMode,
  };
};
