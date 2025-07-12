"use client";

import { useTheme } from "../../contexts/ThemeContext";

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg' | 'xl';
  color?: 'primary' | 'secondary' | 'white' | 'blue';
  className?: string;
  label?: string;
}

const sizeClasses = {
  sm: 'w-4 h-4',
  md: 'w-6 h-6', 
  lg: 'w-8 h-8',
  xl: 'w-12 h-12'
};

const colorClasses = {
  primary: 'text-blue-600',
  secondary: 'text-gray-500',
  white: 'text-white',
  blue: 'text-blue-600'
};

export function LoadingSpinner({ 
  size = 'md', 
  color = 'primary', 
  className = '', 
  label = 'Loading...' 
}: LoadingSpinnerProps) {
  const { token } = useTheme();
  
  const spinnerColor = color === 'primary' 
    ? token('color', 'primaryText')
    : color === 'secondary'
    ? token('color', 'secondaryText')
    : colorClasses[color];

  return (
    <div 
      className={`inline-flex items-center justify-center ${className}`}
      role="status"
      aria-label={label}
    >
      <svg
        className={`${sizeClasses[size]} animate-spin`}
        style={{ color: spinnerColor }}
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
      >
        <circle
          className="opacity-25"
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          strokeWidth="4"
        />
        <path
          className="opacity-75"
          fill="currentColor"
          d="m4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
        />
      </svg>
      <span className="sr-only">{label}</span>
    </div>
  );
}
