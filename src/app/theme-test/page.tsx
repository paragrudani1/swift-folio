"use client";

import { useTheme } from "../contexts/ThemeContext";
import { ThemeToggle } from "../components/ThemeToggle";

export default function ThemeTestPage() {
  const { theme, setTheme, toggle } = useTheme();

  return (
    <div className="min-h-screen bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100 p-8">
      <h1 className="text-3xl font-bold mb-8">Theme Context Test</h1>
      
      <div className="space-y-4">
        <div>
          <p className="mb-2">Current theme: <strong>{theme}</strong></p>
        </div>
        
        <div className="flex gap-4">
          <button
            onClick={() => setTheme("light")}
            className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
          >
            Set Light Theme
          </button>
          
          <button
            onClick={() => setTheme("dark")}
            className="px-4 py-2 bg-gray-700 text-white rounded hover:bg-gray-800"
          >
            Set Dark Theme
          </button>
          
          <button
            onClick={toggle}
            className="px-4 py-2 bg-purple-500 text-white rounded hover:bg-purple-600"
          >
            Toggle Theme
          </button>
        </div>
        
        <div className="mt-8">
          <p className="mb-2">ThemeToggle Component:</p>
          <ThemeToggle />
        </div>
      </div>
    </div>
  );
}
