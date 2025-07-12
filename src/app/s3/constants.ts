/**
 * Constants for the S3 Explorer application
 */

// Security warning: In production, this should be managed through environment variables
export const ENCRYPTION_KEY = "your-super-secret-key";

export const DEFAULT_REGION = "us-east-1";

export const FILE_ICONS = {
  image: "&#128248;", // 📸
  pdf: "&#128443;",   // 📃
  text: "&#128441;",  // 📄
  default: "&#128440;", // 📄
  folder: "&#128447;", // 📁
} as const;

export const FILE_EXTENSIONS = {
  image: ["jpg", "jpeg", "png", "gif", "svg", "webp", "bmp", "ico"],
  pdf: ["pdf"],
  text: ["txt", "md", "markdown", "log", "json", "xml", "csv", "yaml", "yml"],
  code: ["js", "ts", "jsx", "tsx", "html", "css", "scss", "sass", "less", "php", "py", "java", "cpp", "c", "cs", "rb", "go", "rs", "swift", "kt"],
  archive: ["zip", "rar", "7z", "tar", "gz", "bz2"],
  video: ["mp4", "avi", "mov", "wmv", "flv", "webm"],
  audio: ["mp3", "wav", "flac", "aac", "ogg"],
  document: ["doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp"],
} as const;

export const STORAGE_KEYS = {
  credentials: "aws_credentials",
  bucketName: "aws_bucket_name",
  region: "aws_region",
} as const;

export const UI_CONFIG = {
  itemsPerPage: 50,
  maxFileNameLength: 50,
  uploadChunkSize: 5 * 1024 * 1024, // 5MB
  searchDebounceMs: 300,
  defaultSortDirection: 'ascending' as const,
} as const;

export const S3_CONFIG = {
  delimiter: "/",
  maxKeys: 1000,
  timeout: 30000, // 30 seconds
} as const;

export const MODAL_TYPES = {
  CREATE_FOLDER: 'createFolder',
  DELETE_ITEM: 'deleteItem',
  BULK_DELETE: 'bulkDelete',
} as const;
