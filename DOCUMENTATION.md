# Project Documentation: Swift S3 Explorer

This document provides a comprehensive overview of the Swift S3 Explorer project, detailing its purpose, features, technical stack, architecture, and key functionalities.

## 1. Project Overview

**Project Name:** Swift S3 Explorer (also referred to as Swift Folio in `package.json` and `README.md`)

**Description:** Swift S3 Explorer is a modern, user-friendly web application designed to simplify the management of AWS S3 buckets. It offers an intuitive interface for users to securely connect to their S3 accounts, browse bucket contents, upload and download files, create folders, and perform basic file management operations. The application emphasizes ease of use, responsiveness, and secure handling of AWS credentials.

## 2. Features

*   **Secure Authentication:** Users can securely connect to their AWS S3 account using their Access Key ID and Secret Access Key. Credentials can be optionally saved in the browser's local storage, encrypted using `crypto-js`.
*   **Bucket Exploration:** Navigate through S3 buckets and their hierarchical folder structures.
*   **File & Folder Management:**
    *   **Upload:** Drag-and-drop functionality for easy file uploads, with real-time progress tracking.
    *   **Download:** Download individual files from S3.
    *   **Delete:** Delete individual files or perform bulk deletions of selected items.
    *   **Create Folder:** Create new folders within the S3 bucket.
*   **User Interface & Experience:**
    *   **Responsive Design:** Optimized for various screen sizes using Tailwind CSS.
    *   **Search & Sort:** Filter files and folders by name and sort them by last modified date or size.
    *   **Storage Usage:** Displays the total storage consumed by the connected S3 bucket.
    *   **Clear Navigation:** Breadcrumb-like display of the current S3 path and a "Back" button for easy navigation.
    *   **Modals:** Confirmation modals for sensitive actions like deletion and input modals for creating new folders.

## 3. Technical Stack

The application is built using a modern web development stack:

*   **Frontend Framework:** [Next.js](https://nextjs.org/) (React) - Provides server-side rendering, routing, and a robust development environment.
*   **Styling:** [Tailwind CSS](https://tailwindcss.com/) - A utility-first CSS framework for rapid UI development.
*   **AWS Interaction:**
    *   [`@aws-sdk/client-s3`](https://docs.aws.amazon.com/AWSJavaScriptSDK/v3/latest/clients/client-s3/index.html) - AWS SDK for JavaScript (v3) for direct interaction with S3 services (listing, getting, deleting, putting objects).
    *   [`@aws-sdk/lib-storage`](https://docs.aws.amazon.com/AWSJavaScriptSDK/v3/latest/modules/_aws_sdk_lib_storage.html) - Provides higher-level abstractions for S3 operations, specifically used for managing file uploads with progress.
*   **File Uploads:** [React Dropzone](https://react-dropzone.js.org/) - A flexible React hook for creating drag-and-drop file upload areas.
*   **Cryptography:** [CryptoJS](https://cryptojs.gitbook.io/docs/) - Used for encrypting and decrypting sensitive AWS credentials stored in local storage.
*   **Fonts:** [Next.js Font](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) (Geist, Geist Mono) - Optimizes font loading for performance.
*   **Performance Monitoring:** [Vercel Speed Insights](https://vercel.com/docs/speed-insights) - Integrated for monitoring and improving application performance.
*   **Language:** TypeScript - Enhances code quality and maintainability through static typing.

## 4. Architecture and Code Structure

The project follows a standard Next.js application structure with a focus on component-based development and clear separation of concerns.

```
/
├── .next/                  # Next.js build output
├── node_modules/           # Project dependencies
├── public/                 # Static assets (images, favicons)
├── src/
│   └── app/                # Main application source code (App Router)
│       ├── favicon.ico     # Application favicon
│       ├── globals.css     # Global CSS styles (includes Tailwind imports)
│       ├── layout.tsx      # Root layout component (defines <html>, <body>, global fonts, Speed Insights)
│       ├── page.tsx        # Root page, redirects to /s3
│       ├── components/     # (Currently empty) Intended for reusable UI components
│       ├── context/        # (Currently empty) Intended for React Context APIs
│       └── s3/             # S3-specific components and logic
│           ├── Dropzone.tsx  # React component for drag-and-drop file uploads
│           └── page.tsx      # Core S3 Explorer interface, handles S3 logic, state, and UI
├── .gitignore              # Git ignore rules
├── eslint.config.mjs       # ESLint configuration
├── next.config.ts          # Next.js configuration
├── package-lock.json       # npm dependency lock file
├── package.json            # Project metadata and scripts
├── postcss.config.mjs      # PostCSS configuration (for Tailwind CSS)
├── README.md               # Project README (user-facing documentation)
├── tsconfig.json           # TypeScript configuration
└── DOCUMENTATION.md        # This documentation file
```

### Key Components:

*   **`src/app/layout.tsx`**: This is the root layout for the entire application. It sets up the basic HTML structure, imports global CSS, configures custom fonts (`Geist`, `Geist_Mono`), and integrates Vercel Speed Insights.
*   **`src/app/page.tsx`**: This serves as the initial entry point for the application. It immediately redirects users to the `/s3` route, which is the main S3 Explorer interface.
*   **`src/app/s3/page.tsx`**: This is the central component of the S3 Explorer. It manages the application's state related to S3 operations (credentials, selected bucket, current prefix, objects, upload progress, sorting, search, modals). It contains the core logic for:
    *   Connecting to AWS S3 using provided credentials.
    *   Listing objects and common prefixes (folders).
    *   Handling file downloads and deletions.
    *   Creating new folders.
    *   Managing bulk selection and deletion.
    *   Calculating and displaying bucket storage usage.
    *   Rendering the main UI, including the login form, file/folder display, and action buttons.
*   **`src/app/s3/Dropzone.tsx`**: A dedicated React component that encapsulates the drag-and-drop file upload functionality. It utilizes `react-dropzone` for the UI and `@aws-sdk/lib-storage` for handling the actual S3 upload process, including progress reporting.

## 5. Core Functionality Details

### 5.1. Authentication and Credential Management

*   The `S3Explorer` component (`src/app/s3/page.tsx`) handles user input for AWS Access Key ID, Secret Access Key, Bucket Name, and Region.
*   Credentials can be saved to `localStorage` by encrypting them with `CryptoJS.AES.encrypt`.
*   An `useEffect` hook attempts to auto-login using saved credentials on component mount.
*   **Security Note:** The `ENCRYPTION_KEY` is currently hardcoded in `src/app/s3/page.tsx`. **This is a significant security vulnerability and must be addressed in a production environment.** A more secure approach would involve using environment variables, a secure key management service, or a backend API to handle credentials.

### 5.2. S3 Operations

All S3 interactions are performed using the AWS SDK for JavaScript (v3).

*   **`S3Client` Initialization:** An `S3Client` instance is created with the provided region and credentials.
*   **Listing Objects (`listObjects` function):**
    *   Uses `ListObjectsV2Command` to fetch objects and common prefixes (representing folders) within the current bucket and prefix.
    *   Filters out directory markers and ensures only direct child folders are displayed.
*   **Uploading Files:**
    *   The `Dropzone` component handles the file selection and drag-and-drop events.
    *   It uses `new Upload()` from `@aws-sdk/lib-storage` to perform multi-part uploads, providing progress updates via the `onProgress` callback.
    *   File keys are constructed using the `prefix` (current folder path) and the file's relative path.
*   **Downloading Files (`handleDownload` function):**
    *   Uses `GetObjectCommand` to retrieve the file content.
    *   The content is converted to a `Blob` and a temporary URL is created to trigger a browser download.
*   **Deleting Files (`handleDelete`, `handleConfirmDelete` functions):**
    *   Uses `DeleteObjectCommand` to remove a specified object from S3.
    *   A confirmation modal is used to prevent accidental deletions.
*   **Creating Folders (`handleCreateFolderClick`, `handleConfirmCreateFolder` functions):**
    *   Folders in S3 are represented by objects with a trailing slash (e.g., `myfolder/`).
    *   A new folder is created by sending a `PutObjectCommand` with an empty body and the folder key ending in `/`.
*   **Bulk Deletion (`handleBulkDelete`, `handleConfirmBulkDelete` functions):**
    *   Allows users to select multiple files/folders.
    *   Iterates through the selected items and performs individual `DeleteObjectCommand` operations.

### 5.3. State Management

React's `useState` and `useEffect` hooks are extensively used for managing the component's local state, including:

*   `error`: Stores any error messages.
*   `credentials`, `bucketNameInput`, `regionInput`: For login form data.
*   `s3Client`, `selectedBucket`: S3 client instance and currently selected bucket.
*   `objects`, `prefixes`: Lists of files and folders in the current view.
*   `currentPrefix`: The current path within the S3 bucket.
*   `uploadProgress`: Tracks the progress of file uploads.
*   `sortConfig`, `searchTerm`: For sorting and searching functionality.
*   `storageUsage`, `loadingStorageUsage`: For displaying bucket storage.
*   `isCreateFolderModalOpen`, `newFolderName`: For the create folder modal.
*   `isDeleteModalOpen`, `itemToDelete`: For the single item delete confirmation modal.
*   `isSelectMode`, `selectedItems`, `isBulkDeleteModalOpen`: For bulk selection and deletion.

## 6. Development and Setup

Refer to the `README.md` file for instructions on how to set up and run the project locally.

## 7. Future Enhancements (Potential Areas for Improvement)

*   **Enhanced Security:**
    *   Implement a secure method for managing the `ENCRYPTION_KEY` (e.g., environment variables, server-side key management).
    *   Consider using AWS Cognito or other identity providers for more robust user authentication instead of direct credential input.
    *   Implement proper CORS configurations on the S3 bucket to restrict access.
*   **Error Handling & User Feedback:**
    *   More granular error messages for S3 operations.
    *   Visual feedback for ongoing operations (loading states for all S3 actions).
    *   Toast notifications for success/failure messages.
*   **Advanced S3 Features:**
    *   Support for multi-part downloads.
    *   Version control for objects.
    *   Setting object ACLs or metadata.
    *   Copying/moving files.
    *   Renaming files/folders.
    *   Support for larger file uploads/downloads.
*   **User Interface Improvements:**
    *   Pagination for large lists of objects.
    *   Improved search capabilities (e.g., fuzzy search).
    *   Drag-and-drop reordering of files (if applicable).
    *   More detailed file information (e.g., content type).
    *   The `components/` and `context/` directories are currently empty. Extract reusable UI components and context providers to these directories for better organization and reusability.
*   **Testing:** Implement unit and integration tests for critical functionalities.
*   **CI/CD:** Set up continuous integration and deployment pipelines.
*   **Backend Integration:** For a more robust and secure application, consider adding a backend API to proxy S3 requests, manage credentials, and implement more complex business logic.
