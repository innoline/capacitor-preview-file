import { WebPlugin } from '@capacitor/core';

import type { PreviewFilePlugin } from './definitions';

export class PreviewFileWeb extends WebPlugin implements PreviewFilePlugin {
  async previewByPath(options: { path: string; mimeType: string; name: string }): Promise<void> {
    console.log('ECHO', options);
    return Promise.resolve(undefined);
  }

  previewBase64(options: { base64: string; mimeType: string; name: string }): Promise<void> {
    console.log('ECHO', options);
    return Promise.resolve(undefined);
  }
}
