import React from "react";
import { useTheme } from "../../contexts/ThemeContext";
import { ThemeToggle } from "../../components/ThemeToggle";
import { LoadingSpinner } from "./LoadingSpinner";
import { formatFileSize } from "../utils";

interface HeaderProps {
  selectedBucket: string | null;
  storageUsage: number | null;
  loadingStorageUsage: boolean;
  onLogout: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  selectedBucket,
  storageUsage,
  loadingStorageUsage,
  onLogout,
}) => {
  const { token } = useTheme();

  return (
    <div
      className="sticky top-0 z-10 backdrop-blur-sm border-b"
      style={{
        backgroundColor: token("color", "glassBg"),
        borderColor: token("color", "primaryBorder"),
      }}
    >
      <div className="max-w-7xl mx-auto px-3 sm:px-4 lg:px-6 py-3 sm:py-4">
        {/* Mobile Layout */}
        <div className="flex sm:hidden items-center justify-between">
          <div className="flex items-center space-x-2">
            <div className="w-8 h-8 bg-gradient-to-r from-blue-600 to-indigo-600 rounded-lg flex items-center justify-center flex-shrink-0">
              <svg
                className="w-4 h-4 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2z"
                />
              </svg>
            </div>
            <div className="min-w-0">
              <h1
                className="text-lg font-bold truncate"
                style={{ color: token("color", "primaryText") }}
              >
                Swift S3 Explorer
              </h1>
            </div>
          </div>
          
          {selectedBucket && (
            <div className="flex items-center space-x-2 flex-shrink-0">
              <div
                className="text-xs px-2 py-1 rounded-md border"
                style={{
                  color: token("color", "secondaryText"),
                  backgroundColor: token("color", "overlayBg"),
                  borderColor: token("color", "primaryBorder"),
                }}
              >
                <span className="font-medium">Storage:</span>{" "}
                {loadingStorageUsage ? (
                  <LoadingSpinner size="sm" color="secondary" />
                ) : storageUsage !== null ? (
                  <span className="font-semibold text-blue-600">
                    {formatFileSize(storageUsage)}
                  </span>
                ) : (
                  "N/A"
                )}
              </div>
              <ThemeToggle />
              <button
                onClick={onLogout}
                className="flex items-center justify-center p-2 border rounded-lg transition-colors duration-200 touch-manipulation min-w-[40px] min-h-[40px]"
                style={{
                  backgroundColor: token("color", "overlayBg"),
                  color: token("color", "primaryText"),
                  borderColor: token("color", "primaryBorder"),
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = token("color", "hoverStrongBg");
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = token("color", "overlayBg");
                }}
                title="Logout"
                aria-label="Logout from S3 Explorer"
              >
                <svg
                  className="w-4 h-4"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
                  />
                </svg>
              </button>
            </div>
          )}
        </div>

        {/* Desktop/Tablet Layout */}
        <div className="hidden sm:flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 bg-gradient-to-r from-blue-600 to-indigo-600 rounded-xl flex items-center justify-center">
              <svg
                className="w-6 h-6 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2H5a2 2 0 00-2-2z"
                />
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M8 1v6m8-6v6"
                />
              </svg>
            </div>
            <div>
              <h1
                className="text-2xl font-bold"
                style={{ color: token("color", "primaryText") }}
              >
                Swift S3 Explorer
              </h1>
              <p
                className="text-sm"
                style={{ color: token("color", "mutedText") }}
              >
                Manage your AWS S3 buckets with ease
              </p>
            </div>
          </div>
          
          {selectedBucket && (
            <div className="flex items-center space-x-4">
              <div
                className="text-sm px-4 py-2 rounded-lg border"
                style={{
                  color: token("color", "secondaryText"),
                  backgroundColor: token("color", "overlayBg"),
                  borderColor: token("color", "primaryBorder"),
                }}
              >
                <span className="font-medium">Storage:</span>{" "}
                {loadingStorageUsage ? (
                  <div className="inline-flex items-center space-x-2">
                    <LoadingSpinner size="sm" color="secondary" />
                    <span>Loading...</span>
                  </div>
                ) : storageUsage !== null ? (
                  <span className="font-semibold text-blue-600">
                    {formatFileSize(storageUsage)}
                  </span>
                ) : (
                  "N/A"
                )}
              </div>
              <ThemeToggle />
              <button
                onClick={onLogout}
                className="flex items-center justify-center p-2.5 border rounded-lg transition-colors duration-200 hover:scale-105 transform touch-manipulation min-w-[40px] min-h-[40px]"
                style={{
                  backgroundColor: token("color", "overlayBg"),
                  color: token("color", "primaryText"),
                  borderColor: token("color", "primaryBorder"),
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.backgroundColor = token("color", "hoverStrongBg");
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = token("color", "overlayBg");
                }}
                title="Logout from S3 Explorer"
                aria-label="Logout from S3 Explorer"
              >
                <svg
                  className="w-5 h-5"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth="2"
                    d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013 3v1"
                  />
                </svg>
                </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
