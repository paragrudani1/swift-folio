"use client";

import { useTheme } from "../contexts/ThemeContext";

// CSS Variable names type
type CSSVarName = 
  | '--bg-primary' | '--bg-secondary' | '--bg-tertiary' | '--bg-overlay' | '--bg-glass' 
  | '--bg-hover' | '--bg-hover-strong'
  | '--text-primary' | '--text-secondary' | '--text-tertiary' | '--text-muted'
  | '--border-primary' | '--border-secondary' | '--border-focus'
  | '--shadow-sm' | '--shadow-md' | '--shadow-lg' | '--shadow-xl' | '--shadow-2xl';

// Extended useTheme hook that also provides CSS variable access
export function useThemeWithCSSVars() {
  const themeContext = useTheme();

  // Helper to get CSS variable value
  const cssVar = (varName: CSSVarName): string => {
    if (typeof window === 'undefined') return '';
    return getComputedStyle(document.documentElement).getPropertyValue(varName).trim();
  };

  // Helper to create inline styles with CSS variables
  const varStyles = (vars: Partial<Record<CSSVarName, string>>) => {
    return Object.entries(vars).reduce((acc, [key, value]) => {
      acc[key as string] = `var(${key})`;
      return acc;
    }, {} as Record<string, string>);
  };

  return {
    ...themeContext,
    cssVar,
    varStyles,
  };
}
