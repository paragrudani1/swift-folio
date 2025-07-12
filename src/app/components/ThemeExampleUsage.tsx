"use client";

import { useTheme } from "../contexts/ThemeContext";

export function ThemeExampleUsage() {
  const { theme, toggle, isDark, classes, token } = useTheme();

  // Example usage of isDark helper
  const currentMode = isDark() ? "Dark Mode" : "Light Mode";

  // Example usage of classes helper
  const buttonClasses = classes({
    light: "bg-blue-500 text-white hover:bg-blue-600",
    dark: "bg-gray-700 text-gray-100 hover:bg-gray-600"
  });

  const containerClasses = classes({
    light: "bg-white text-gray-900 border-gray-200",
    dark: "bg-gray-800 text-gray-100 border-gray-700"
  });

  // Example usage of token helper with TypeScript inference
  const primaryBg = token("color", "primaryBg");
  const mutedText = token("color", "mutedText");
  const fontSize = token("typography", "fontSize");
  const padding = token("spacing", "padding");
  const borderRadius = token("radii", "borderRadius");

  return (
    <div 
      className={`p-8 rounded-lg border-2 ${containerClasses}`}
      style={{
        backgroundColor: primaryBg,
        borderRadius: borderRadius
      }}
    >
      <h2 className="text-2xl font-bold mb-4">Theme Hook Example</h2>
      
      <div className="space-y-4">
        <p>Current theme: <strong>{theme}</strong></p>
        <p>Mode: <strong>{currentMode}</strong></p>
        
        <div style={{ color: mutedText, fontSize: fontSize }}>
          This text uses theme tokens directly
        </div>

        <button
          onClick={toggle}
          className={`px-4 py-2 rounded transition-colors ${buttonClasses}`}
          style={{ padding: padding }}
        >
          Toggle Theme
        </button>

        <div className="mt-4 p-4 bg-gray-100 dark:bg-gray-700 rounded">
          <h3 className="font-semibold mb-2">Token Values:</h3>
          <ul className="space-y-1 text-sm">
            <li>Primary BG: {primaryBg}</li>
            <li>Text Muted: {mutedText}</li>
            <li>Font Size: {fontSize}</li>
            <li>Padding: {padding}</li>
            <li>Border Radius: {borderRadius}</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
