"use client";

import { useTheme } from "../../contexts/ThemeContext";
import { LoadingSpinner } from "./LoadingSpinner";

interface LoadingButtonProps {
  isLoading: boolean;
  disabled?: boolean;
  children: React.ReactNode;
  loadingText?: string;
  size?: 'sm' | 'md' | 'lg';
  variant?: 'primary' | 'secondary' | 'danger';
  className?: string;
  onClick?: () => void;
  type?: 'button' | 'submit' | 'reset';
}

export function LoadingButton({ 
  isLoading, 
  disabled, 
  children, 
  loadingText,
  size = 'md',
  variant = 'primary',
  className = '',
  onClick,
  type = 'button'
}: LoadingButtonProps) {
  const { token } = useTheme();
  
  const isDisabled = isLoading || disabled;
  
  const sizeClasses = {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2 text-sm',
    lg: 'px-6 py-3 text-base'
  };
  
  const variantClasses = {
    primary: 'bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white',
    secondary: 'border hover:scale-105',
    danger: 'bg-gradient-to-r from-red-600 to-rose-600 hover:from-red-700 hover:to-rose-700 text-white'
  };

  const getButtonStyle = () => {
    if (variant === 'secondary') {
      return {
        backgroundColor: token('color', 'secondaryBg'),
        color: token('color', 'secondaryText'),
        borderColor: token('color', 'primaryBorder'),
        boxShadow: token('shadow', 'lg')
      };
    }
    return {
      boxShadow: token('shadow', 'lg')
    };
  };

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={isDisabled}
      className={`
        ${sizeClasses[size]} 
        ${variantClasses[variant]}
        rounded-lg font-medium transition-all duration-200 
        flex items-center justify-center space-x-2 
        ${isDisabled ? 'opacity-50 cursor-not-allowed' : 'transform hover:scale-105'}
        ${className}
      `}
      style={getButtonStyle()}
      onMouseEnter={(e) => {
        if (!isDisabled && variant !== 'primary' && variant !== 'danger') {
          e.currentTarget.style.backgroundColor = token('color', 'hoverBg');
        }
        if (!isDisabled) {
          e.currentTarget.style.boxShadow = token('shadow', 'xl');
        }
      }}
      onMouseLeave={(e) => {
        if (variant === 'secondary') {
          e.currentTarget.style.backgroundColor = token('color', 'secondaryBg');
        }
        e.currentTarget.style.boxShadow = token('shadow', 'lg');
      }}
    >
      {isLoading && (
        <LoadingSpinner 
          size={size === 'lg' ? 'md' : 'sm'} 
          color="white" 
        />
      )}
      <span>
        {isLoading && loadingText ? loadingText : children}
      </span>
    </button>
  );
}
