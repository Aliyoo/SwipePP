/**
 * 摩登画报 APP 图标渲染：deco-icon.html 三块画板 → PNG 母版（带 alpha）。
 * 用法: NODE_PATH=$(npm root -g) node render-icon.js
 */
const path = require('path');
const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({
    executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    headless: 'new',
    args: ['--no-sandbox', '--disable-gpu'],
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 560, height: 1700, deviceScaleFactor: 1 });
  const cdp = await page.createCDPSession();
  await cdp.send('Emulation.setDefaultBackgroundColorOverride', { color: { r: 0, g: 0, b: 0, a: 0 } });
  await page.goto('file://' + path.resolve(__dirname, 'deco-icon.html'), { waitUntil: 'networkidle0' });
  await new Promise(r => setTimeout(r, 600)); // 字体就位

  for (const id of ['legacy', 'round', 'fg']) {
    const el = await page.$('#' + id);
    const box = await el.boundingBox();
    await page.screenshot({
      path: path.join(__dirname, id + '_512.png'),
      clip: { x: box.x, y: box.y, width: Math.round(box.width), height: Math.round(box.height) },
      omitBackground: true,
    });
    console.log(id + '_512.png done');
  }
  await browser.close();
})();
