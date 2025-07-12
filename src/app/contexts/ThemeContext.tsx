"use client";

import { createContext, useContext, useEffect, useState, ReactNode } from "react";
import { ThemeName, ThemeTokens, themes } from "../../../theme/tokens";

type ThemeContextValue = {
  theme: ThemeName;
  setTheme: (t: ThemeName) => void;
  toggle: () => void;
};

const ThemeContext = createContext<ThemeContextValue | undefined>(undefined);

export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<ThemeName>("light");
  const [isHydrated, setIsHydrated] = useState(false);

  useEffect(() => {
    // Get the theme that was set by the script tag
    const initialTheme = document.documentElement.getAttribute("data-theme") as ThemeName;
    
    if (initialTheme && (initialTheme === "light" || initialTheme === "dark")) {
      setThemeState(initialTheme);
    } else {
      // Fallback: Check if user has a saved preference
      const savedTheme = localStorage.getItem("theme") as ThemeName;
      if (savedTheme && (savedTheme === "light" || savedTheme === "dark")) {
        setThemeState(savedTheme);
      } else {
        // Check system preference
        const systemPrefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches;
        setThemeState(systemPrefersDark ? "dark" : "light");
      }
    }
    
    // Mark as hydrated after first effect
    setIsHydrated(true);
  }, []);

  const setTheme = (newTheme: ThemeName) => {
    setThemeState(newTheme);
    localStorage.setItem("theme", newTheme);
    document.documentElement.className = newTheme === "dark" ? "theme-dark" : "theme-light";
    document.documentElement.setAttribute("data-theme", newTheme);
  };

  const toggle = () => {
    setTheme(theme === "light" ? "dark" : "light");
  };

  const value: ThemeContextValue = {
    theme,
    setTheme,
    toggle,
  };

  // Only render children after hydration to avoid mismatch
  if (!isHydrated) {
    return null;
  }

  return (
    <ThemeContext.Provider value={value}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error("useTheme must be used within a ThemeProvider");
  }

  const isDark = () => context.theme === 'dark';

  const classes = ({ light, dark }: { light: string; dark: string }): string => {
    return context.theme === 'dark' ? dark : light;
  };

  const token = <K extends keyof ThemeTokens, T extends keyof ThemeTokens[K]>(
    category: K,
    name: T
  ): string => {
    return themes[context.theme][category][name] as string;
  };

  return {
    ...context,
    isDark,
    classes,
    token,
  };
}
