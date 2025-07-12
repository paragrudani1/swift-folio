import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { SpeedInsights } from "@vercel/speed-insights/next";
import { ThemeProvider } from "./contexts/ThemeContext";
import { BodyWrapper } from "./components/BodyWrapper";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Swift S3 Explorer",
  description: "A simple and modern AWS S3 Explorer.",
  icons: {
    icon: "/favicon.ico",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="theme-light" data-theme="light">
      <head>
        <script
          dangerouslySetInnerHTML={{
            __html: `
              (function() {
                try {
                  const theme = localStorage.getItem('theme');
                  if (theme === 'dark' || theme === 'light') {
                    document.documentElement.className = theme === 'dark' ? 'theme-dark' : 'theme-light';
                    document.documentElement.setAttribute('data-theme', theme);
                  } else {
                    const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
                    const defaultTheme = systemPrefersDark ? 'dark' : 'light';
                    document.documentElement.className = defaultTheme === 'dark' ? 'theme-dark' : 'theme-light';
                    document.documentElement.setAttribute('data-theme', defaultTheme);
                  }
                } catch (e) {
                  // Fallback to light theme
                  document.documentElement.className = 'theme-light';
                  document.documentElement.setAttribute('data-theme', 'light');
                }
              })();
            `,
          }}
        />
      </head>
      <BodyWrapper fontVariables={`${geistSans.variable} ${geistMono.variable}`}>
        <ThemeProvider>
          <main>{children}</main>
          <SpeedInsights />
        </ThemeProvider>
      </BodyWrapper>
    </html>
  );
}
