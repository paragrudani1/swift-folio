// This file demonstrates TypeScript inference for the useTheme hook

import { useTheme } from "../contexts/ThemeContext";

// Type test: Verify that token() method provides proper type inference
function typeTests() {
  const { token } = useTheme();

  // These should all have proper type inference and autocomplete
  const color1 = token("color", "primaryBg"); // Should infer string
  const color2 = token("color", "textMuted"); // Should infer string
  
  const typography1 = token("typography", "fontSize"); // Should infer string
  const typography2 = token("typography", "fontFamily"); // Should infer string
  
  const spacing1 = token("spacing", "padding"); // Should infer string
  const spacing2 = token("spacing", "margin"); // Should infer string
  
  const radii1 = token("radii", "borderRadius"); // Should infer string

  // TypeScript should error on these (uncomment to test):
  // const error1 = token("color", "nonExistent"); // Error: Argument of type '"nonExistent"' is not assignable
  // const error2 = token("invalid", "primaryBg"); // Error: Argument of type '"invalid"' is not assignable
  // const error3 = token("color"); // Error: Expected 2 arguments, but got 1

  // Return type should always be string
  const assertString: string = color1;
}

// Type test: Verify isDark return type
function isDarkTest() {
  const { isDark } = useTheme();
  const result: boolean = isDark(); // Should be boolean
}

// Type test: Verify classes helper
function classesTest() {
  const { classes } = useTheme();
  
  // Should require both light and dark properties
  const result1: string = classes({
    light: "light-classes",
    dark: "dark-classes"
  });

  // TypeScript should error on these (uncomment to test):
  // const error1 = classes({ light: "only-light" }); // Error: Property 'dark' is missing
  // const error2 = classes({ dark: "only-dark" }); // Error: Property 'light' is missing
  // const error3 = classes({}); // Error: Properties 'light' and 'dark' are missing
}

export { typeTests, isDarkTest, classesTest };
