export type ThemeName = 'light' | 'dark';

interface ColorTokens {
  // Background colors
  primaryBg: string;
  secondaryBg: string;
  tertiaryBg: string;
  overlayBg: string;
  glassBg: string;
  hoverBg: string;
  hoverStrongBg: string;
  
  // Text colors
  primaryText: string;
  secondaryText: string;
  tertiaryText: string;
  mutedText: string;
  
  // Border colors
  primaryBorder: string;
  secondaryBorder: string;
  focusBorder: string;
}

interface TypographyTokens {
  fontSize: string;
  fontFamily: string;
  // Add more typography tokens as needed
}

interface SpacingTokens {
  padding: string;
  margin: string;
  // Add more spacing tokens as needed
}

interface RadiiTokens {
  borderRadius: string;
  // Add more radii tokens as needed
}

interface ShadowTokens {
  sm: string;
  md: string;
  lg: string;
  xl: string;
  '2xl': string;
}

export interface ThemeTokens {
  color: ColorTokens;
  typography: TypographyTokens;
  spacing: SpacingTokens;
  radii: RadiiTokens;
  shadow: ShadowTokens;
}

const lightTheme: ThemeTokens = {
  color: {
    // Background colors
    primaryBg: '#ffffff',
    secondaryBg: '#f8fafc',
    tertiaryBg: '#f1f5f9',
    overlayBg: 'rgba(255, 255, 255, 0.9)',
    glassBg: 'rgba(255, 255, 255, 0.8)',
    hoverBg: 'rgba(0, 0, 0, 0.02)',
    hoverStrongBg: 'rgba(0, 0, 0, 0.05)',
    
    // Text colors
    primaryText: '#0f172a',
    secondaryText: '#475569',
    tertiaryText: '#64748b',
    mutedText: '#94a3b8',
    
    // Border colors
    primaryBorder: '#e2e8f0',
    secondaryBorder: '#cbd5e1',
    focusBorder: '#3b82f6',
  },
  typography: {
    fontSize: '16px',
    fontFamily: 'Arial, sans-serif',
  },
  spacing: {
    padding: '8px',
    margin: '8px',
  },
  radii: {
    borderRadius: '4px',
  },
  shadow: {
    sm: '0 1px 2px 0 rgb(0 0 0 / 0.05)',
    md: '0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1)',
    lg: '0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1)',
    xl: '0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1)',
    '2xl': '0 25px 50px -12px rgb(0 0 0 / 0.25)',
  },
};

const darkTheme: ThemeTokens = {
  color: {
    // Background colors
    primaryBg: '#0f172a',
    secondaryBg: '#1e293b',
    tertiaryBg: '#334155',
    overlayBg: 'rgba(15, 23, 42, 0.95)',
    glassBg: 'rgba(15, 23, 42, 0.8)',
    hoverBg: 'rgba(255, 255, 255, 0.05)',
    hoverStrongBg: 'rgba(255, 255, 255, 0.1)',
    
    // Text colors
    primaryText: '#f8fafc',
    secondaryText: '#e2e8f0',
    tertiaryText: '#cbd5e1',
    mutedText: '#94a3b8',
    
    // Border colors
    primaryBorder: '#334155',
    secondaryBorder: '#475569',
    focusBorder: '#3b82f6',
  },
  typography: {
    fontSize: '16px',
    fontFamily: 'Arial, sans-serif',
  },
  spacing: {
    padding: '8px',
    margin: '8px',
  },
  radii: {
    borderRadius: '4px',
  },
  shadow: {
    sm: '0 1px 2px 0 rgb(0 0 0 / 0.3)',
    md: '0 4px 6px -1px rgb(0 0 0 / 0.4), 0 2px 4px -2px rgb(0 0 0 / 0.4)',
    lg: '0 10px 15px -3px rgb(0 0 0 / 0.4), 0 4px 6px -4px rgb(0 0 0 / 0.4)',
    xl: '0 20px 25px -5px rgb(0 0 0 / 0.4), 0 8px 10px -6px rgb(0 0 0 / 0.4)',
    '2xl': '0 25px 50px -12px rgb(0 0 0 / 0.6)',
  },
};

export const themes = {
  light: lightTheme,
  dark: darkTheme,
};

