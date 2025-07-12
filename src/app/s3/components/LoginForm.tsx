import React from "react";
import { useTheme } from "../../contexts/ThemeContext";
import { ThemeToggle } from "../../components/ThemeToggle";
import { StoredCredentials } from "../utils";

interface LoginFormProps {
  credentials: StoredCredentials;
  onCredentialChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  bucketNameInput: string;
  setBucketNameInput: (value: string) => void;
  regionInput: string;
  setRegionInput: (value: string) => void;
  saveCredentials: boolean;
  setSaveCredentials: (value: boolean) => void;
  onLogin: () => void;
  error: string | null;
}

export const LoginForm: React.FC<LoginFormProps> = ({
  credentials,
  onCredentialChange,
  bucketNameInput,
  setBucketNameInput,
  regionInput,
  setRegionInput,
  saveCredentials,
  setSaveCredentials,
  onLogin,
  error,
}) => {
  const { token } = useTheme();

  return (
    <div className="flex items-center justify-center min-h-[70vh] px-4">
      <div className="w-full max-w-md">
        <div
          className="backdrop-blur-sm rounded-2xl border p-6 sm:p-8"
          style={{
            backgroundColor: token("color", "glassBg"),
            boxShadow: token("shadow", "2xl"),
            borderColor: token("color", "primaryBorder"),
          }}
        >
          <div className="text-center mb-6 sm:mb-8">
            <div className="w-12 h-12 sm:w-16 sm:h-16 bg-gradient-to-r from-blue-600 to-indigo-600 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <svg
                className="w-6 h-6 sm:w-8 sm:h-8 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth="2"
                  d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"
                />
              </svg>
            </div>
            <h2
              className="text-xl sm:text-2xl font-bold mb-2"
              style={{ color: token("color", "primaryText") }}
            >
              Connect to AWS S3
            </h2>
            <p
              className="text-sm sm:text-base"
              style={{ color: token("color", "secondaryText") }}
            >
              Enter your credentials to get started
            </p>
            <div className="flex justify-center mt-4">
              <ThemeToggle />
            </div>
          </div>

          <form
            className="space-y-4 sm:space-y-6"
            onSubmit={(e) => {
              e.preventDefault();
              onLogin();
            }}
          >
            <div className="space-y-2">
              <label
                className="text-sm font-medium"
                style={{ color: token("color", "secondaryText") }}
              >
                Access Key ID
              </label>
              <input
                type="text"
                name="accessKeyId"
                placeholder="AKIA..."
                value={credentials.accessKeyId}
                onChange={onCredentialChange}
                className="w-full px-4 py-3 border rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200"
                style={{
                  backgroundColor: token("color", "glassBg"),
                  borderColor: token("color", "primaryBorder"),
                  color: token("color", "primaryText"),
                }}
                required
              />
            </div>

            <div className="space-y-2">
              <label
                className="text-sm font-medium"
                style={{ color: token("color", "secondaryText") }}
              >
                Secret Access Key
              </label>
              <input
                type="password"
                name="secretAccessKey"
                placeholder="••••••••••••••••••••••••••••••••••••••••"
                value={credentials.secretAccessKey}
                onChange={onCredentialChange}
                className="w-full px-4 py-3 border rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200"
                style={{
                  backgroundColor: token("color", "glassBg"),
                  borderColor: token("color", "primaryBorder"),
                  color: token("color", "primaryText"),
                }}
                required
              />
            </div>

            <div className="space-y-2">
              <label
                className="text-sm font-medium"
                style={{ color: token("color", "secondaryText") }}
              >
                Bucket Name (Optional)
              </label>
              <input
                type="text"
                name="bucketName"
                placeholder="my-s3-bucket"
                value={bucketNameInput}
                onChange={(e) => setBucketNameInput(e.target.value)}
                className="w-full px-4 py-3 border rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200"
                style={{
                  backgroundColor: token("color", "glassBg"),
                  borderColor: token("color", "primaryBorder"),
                  color: token("color", "primaryText"),
                }}
              />
            </div>

            <div className="space-y-2">
              <label
                className="text-sm font-medium"
                style={{ color: token("color", "secondaryText") }}
              >
                AWS Region
              </label>
              <input
                type="text"
                name="region"
                placeholder="us-east-1"
                value={regionInput}
                onChange={(e) => setRegionInput(e.target.value)}
                className="w-full px-4 py-3 border rounded-xl focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all duration-200"
                style={{
                  backgroundColor: token("color", "glassBg"),
                  borderColor: token("color", "primaryBorder"),
                  color: token("color", "primaryText"),
                }}
              />
            </div>

            <div className="flex items-center space-x-3">
              <input
                type="checkbox"
                id="saveCredentials"
                checked={saveCredentials}
                onChange={(e) => setSaveCredentials(e.target.checked)}
                className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
                style={{
                  backgroundColor: token("color", "secondaryBg"),
                  borderColor: token("color", "primaryBorder"),
                }}
              />
              <label
                htmlFor="saveCredentials"
                className="text-sm font-medium"
                style={{ color: token("color", "secondaryText") }}
              >
                Remember my credentials securely
              </label>
            </div>

            <button
              type="submit"
              className="w-full bg-gradient-to-r from-blue-600 to-indigo-600 text-white py-3 px-6 rounded-xl font-medium hover:from-blue-700 hover:to-indigo-700 transform hover:scale-105 transition-all duration-200"
              style={{ boxShadow: token("shadow", "lg") }}
              onMouseEnter={(e) => {
                e.currentTarget.style.boxShadow = token("shadow", "xl");
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.boxShadow = token("shadow", "lg");
              }}
            >
              Connect to S3
            </button>

            {error && (
              <div
                className="border rounded-xl p-4"
                style={{
                  backgroundColor: token("color", "tertiaryBg"),
                  borderColor: "#ef4444",
                }}
              >
                <p className="text-red-600 text-sm">{error}</p>
              </div>
            )}
          </form>
        </div>
      </div>
    </div>
  );
};
