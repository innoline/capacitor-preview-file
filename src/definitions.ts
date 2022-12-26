export interface PreviewFilePlugin {
  previewByPath(options: { path: string, mimeType: string, name: string }): Promise<void>;
  previewBase64(options: { base64: string, mimeType: string, name: string }): Promise<void>;
}
