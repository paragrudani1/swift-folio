import plugin from 'tailwindcss/plugin';
import { themes, ThemeTokens } from './tokens';

export default plugin(function({ addBase, addUtilities, addComponents }) {
  const lightTheme = themes.light;
  const darkTheme = themes.dark;

  // Helper function to convert nested objects to CSS variables
  const toCSSVariables = (tokens: ThemeTokens, prefix = ''): Record<string, string> => {
    return Object.entries(tokens).reduce((vars, [key, value]) => {
      const varName = `--${prefix}${key}`;
      if (typeof value === 'object') {
        return { ...vars, ...toCSSVariables(value, `${prefix}${key}-`) };
      }
      return { ...vars, [varName]: value };
    }, {});
  };

  // Add CSS variables to :root and .theme-dark
  addBase({
    ':root': toCSSVariables(lightTheme),
    '.theme-dark': toCSSVariables(darkTheme),
  });

  // Map Tailwind color utilities to CSS variables
  addUtilities({
    // Background utilities
    '.bg-theme-primary': {
      backgroundColor: 'var(--color-primaryBg)',
    },
    '.bg-theme-secondary': {
      backgroundColor: 'var(--color-secondaryBg)',
    },
    '.bg-theme-tertiary': {
      backgroundColor: 'var(--color-tertiaryBg)',
    },
    '.bg-theme-overlay': {
      backgroundColor: 'var(--color-overlayBg)',
    },
    '.bg-theme-glass': {
      backgroundColor: 'var(--color-glassBg)',
    },
    '.hover\:bg-theme-hover:hover': {
      backgroundColor: 'var(--color-hoverBg)',
    },
    '.hover\:bg-theme-hover-strong:hover': {
      backgroundColor: 'var(--color-hoverStrongBg)',
    },
    
    // Text utilities
    '.text-theme-primary': {
      color: 'var(--color-primaryText)',
    },
    '.text-theme-secondary': {
      color: 'var(--color-secondaryText)',
    },
    '.text-theme-tertiary': {
      color: 'var(--color-tertiaryText)',
    },
    '.text-theme-muted': {
      color: 'var(--color-mutedText)',
    },
    
    // Border utilities
    '.border-theme-primary': {
      borderColor: 'var(--color-primaryBorder)',
    },
    '.border-theme-secondary': {
      borderColor: 'var(--color-secondaryBorder)',
    },
    '.border-theme-focus': {
      borderColor: 'var(--color-focusBorder)',
    },
    
    // Shadow utilities
    '.shadow-theme-sm': {
      boxShadow: 'var(--shadow-sm)',
    },
    '.shadow-theme-md': {
      boxShadow: 'var(--shadow-md)',
    },
    '.shadow-theme-lg': {
      boxShadow: 'var(--shadow-lg)',
    },
    '.shadow-theme-xl': {
      boxShadow: 'var(--shadow-xl)',
    },
    '.shadow-theme-2xl': {
      boxShadow: 'var(--shadow-2xl)',
    },
  });

  // Add utility patterns as components
  addComponents({
    '.glass-panel': {
      backgroundColor: 'var(--color-glassBg)',
      backdropFilter: 'blur(10px)',
      WebkitBackdropFilter: 'blur(10px)',
      border: '1px solid var(--color-primaryBorder)',
      boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
    },
    '.card': {
      backgroundColor: 'var(--color-primaryBg)',
      padding: 'var(--spacing-padding)',
      borderRadius: 'var(--radii-borderRadius)',
      boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
    },
    '.elevated-border': {
      border: '1px solid var(--color-primaryBorder)',
      boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
    },
  });
});

