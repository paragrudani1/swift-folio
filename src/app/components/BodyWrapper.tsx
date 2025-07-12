"use client";

import { ReactNode } from "react";
import clsx from "clsx";
import { usePrefersReducedMotion } from "../hooks/usePrefersReducedMotion";

interface BodyWrapperProps {
  children: ReactNode;
  fontVariables: string;
}

export function BodyWrapper({ children, fontVariables }: BodyWrapperProps) {
  const prefersReducedMotion = usePrefersReducedMotion();

  return (
    <body
      className={clsx(
        fontVariables,
        "antialiased",
        !prefersReducedMotion && "theme-transition"
      )}
    >
      {children}
    </body>
  );
}
