"use client";

import { useTheme } from "../../contexts/ThemeContext";
import { LoadingSpinner } from "./LoadingSpinner";

interface IconButtonProps {
  icon: React.ReactNode;
  children?: React.ReactNode;
  isLoading?: boolean;
  disabled?: boolean;
  loadingText?: string;
  size?: 'sm' | 'md' | 'lg';
  variant?: 'primary' | 'secondary' | 'danger' | 'success';
  className?: string;
  onClick?: () => void;
  type?: 'button' | 'submit' | 'reset';
  title?: string;
  ariaLabel?: string;
  iconOnly?: boolean;
}

export function IconButton({ 
  icon,
  children,
  isLoading = false, 
  disabled, 
  loadingText,
  size = 'md',
  variant = 'primary',
  className = '',
  onClick,
  type = 'button',
  title,
  ariaLabel,
  iconOnly = false
}: IconButtonProps) {
  const { token } = useTheme();
  
  const isDisabled = isLoading || disabled;
  
  const sizeClasses = {
    sm: iconOnly ? 'p-1.5' : 'px-3 py-1.5 text-sm',
    md: iconOnly ? 'p-2' : 'px-4 py-2 text-sm',
    lg: iconOnly ? 'p-3' : 'px-6 py-3 text-base'
  };
  
  const iconSizes = {
    sm: 'w-4 h-4',
    md: 'w-5 h-5',
    lg: 'w-6 h-6'
  };

  const variantStyles = {
    primary: {
      backgroundColor: 'rgb(37 99 235)', // blue-600
      color: 'white',
      hoverBg: 'rgb(29 78 216)', // blue-700
      border: 'none'
    },
    secondary: {
      backgroundColor: token('color', 'secondaryBg'),
      color: token('color', 'secondaryText'),
      hoverBg: token('color', 'hoverBg'),
      border: `1px solid ${token('color', 'primaryBorder')}`
    },
    danger: {
      backgroundColor: 'rgb(220 38 38)', // red-600
      color: 'white',
      hoverBg: 'rgb(185 28 28)', // red-700
      border: 'none'
    },
    success: {
      backgroundColor: 'rgb(22 163 74)', // green-600
      color: 'white',
      hoverBg: 'rgb(21 128 61)', // green-700
      border: 'none'
    }
  };

  const currentVariant = variantStyles[variant];

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={isDisabled}
      className={`
        ${sizeClasses[size]} 
        rounded-lg font-medium transition-all duration-200 
        flex items-center justify-center
        ${iconOnly ? 'aspect-square' : 'space-x-2'}
        ${isDisabled ? 'opacity-50 cursor-not-allowed' : 'transform hover:scale-105 active:scale-95'}
        touch-manipulation min-w-[44px] min-h-[44px] sm:min-w-[40px] sm:min-h-[40px]
        ${className}
      `}
      style={{
        backgroundColor: currentVariant.backgroundColor,
        color: currentVariant.color,
        border: currentVariant.border,
        boxShadow: token('shadow', 'md')
      }}
      onMouseEnter={(e) => {
        if (!isDisabled) {
          e.currentTarget.style.backgroundColor = currentVariant.hoverBg;
          e.currentTarget.style.boxShadow = token('shadow', 'lg');
        }
      }}
      onMouseLeave={(e) => {
        if (!isDisabled) {
          e.currentTarget.style.backgroundColor = currentVariant.backgroundColor;
          e.currentTarget.style.boxShadow = token('shadow', 'md');
        }
      }}
      title={title}
      aria-label={ariaLabel || title}
    >
      {isLoading ? (
        <>
          <LoadingSpinner size={size} />
          {loadingText && !iconOnly && <span>{loadingText}</span>}
        </>
      ) : (
        <>
          <div className={iconSizes[size]}>
            {icon}
          </div>
          {children && !iconOnly && <span>{children}</span>}
        </>
      )}
    </button>
  );
}
