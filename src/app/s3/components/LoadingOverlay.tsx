"use client";

import { useTheme } from "../../contexts/ThemeContext";
import { LoadingSpinner } from "./LoadingSpinner";

interface LoadingOverlayProps {
  isVisible: boolean;
  message?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  children?: React.ReactNode;
}

export function LoadingOverlay({ 
  isVisible, 
  message = 'Loading...', 
  size = 'lg',
  children 
}: LoadingOverlayProps) {
  const { token } = useTheme();

  if (!isVisible) return <>{children}</>;

  return (
    <div className="relative">
      {children && (
        <div className="opacity-50 pointer-events-none">
          {children}
        </div>
      )}
      
      <div 
        className="absolute inset-0 flex flex-col items-center justify-center backdrop-blur-sm rounded-xl z-10"
        style={{ 
          backgroundColor: token('color', 'overlayBg') + '80' // 50% opacity
        }}
      >
        <LoadingSpinner size={size} color="primary" />
        {message && (
          <p 
            className="mt-3 text-sm font-medium"
            style={{ color: token('color', 'primaryText') }}
          >
            {message}
          </p>
        )}
      </div>
    </div>
  );
}
