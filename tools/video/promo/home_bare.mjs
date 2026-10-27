import { createRequire } from 'node:module';
import * as scenes from '../../store/scenes.mjs';
const { chromium } = createRequire(import.meta.url)('playwright');
const out = process.argv[2];
const W = 560, H = 1253;
const html = `<body style="margin:0;width:${W}px;height:${H}px;overflow:hidden">${scenes.homeScreen({ width: W, widgets: false })}</body>`;
const b = await chromium.launch(); const p = await b.newPage({ viewport: { width: W, height: H }, deviceScaleFactor: 2 });
await p.setContent(html); await p.waitForTimeout(300); await p.screenshot({ path: out }); await b.close();
