import { registerPlugin } from '@capacitor/core';

import type { PreviewFilePlugin } from './definitions';

const PreviewFile = registerPlugin<PreviewFilePlugin>('PreviewFile', {
  web: () => import('./web').then(m => new m.PreviewFileWeb()),
});

export * from './definitions';
export { PreviewFile };
