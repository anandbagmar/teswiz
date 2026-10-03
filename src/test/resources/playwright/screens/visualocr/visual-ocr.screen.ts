import type { ScreenContext } from "../screen-context.ts";
import { stayOnCurrentScreen } from "../screen-route.ts";

async function toVisualElementJson(locator: any, label: string) {
  if (await locator.count() > 0) {
    const first = locator.first();
    const box = await first.boundingBox().catch(() => null);
    if (box) {
      return {
        x: Math.round(box.x),
        y: Math.round(box.y),
        width: Math.round(box.width),
        height: Math.round(box.height),
        label,
      };
    }
  }
  return { x: 0, y: 0, width: 100, height: 30, label };
}

async function toVisualElementJsonArray(locators: any[], label: string) {
  const results = [];
  for (const loc of locators) {
    const box = await loc.boundingBox().catch(() => null);
    if (box) {
      results.push({
        x: Math.round(box.x),
        y: Math.round(box.y),
        width: Math.round(box.width),
        height: Math.round(box.height),
        label,
      });
    }
  }
  return results.length > 0 ? results : [{ x: 0, y: 0, width: 100, height: 30, label }];
}

function getLocatorByImage(screen: ScreenContext, imagePaths: string[]) {
  return screen.page.locator(`img[src*="${imagePaths[0]}"]`);
}

async function resolveTextOrImageLocator(screen: ScreenContext, text: string, imagePaths: string[]) {
  const textLoc = screen.page.getByText(text);
  if (await textLoc.count() > 0) {
    return textLoc;
  }
  return getLocatorByImage(screen, imagePaths);
}

async function resolveImageOrTextLocator(screen: ScreenContext, imagePaths: string[], text: string) {
  const imgLoc = getLocatorByImage(screen, imagePaths);
  if (await imgLoc.count() > 0) {
    return imgLoc;
  }
  return screen.page.getByText(text);
}

function parsePositionIndex(positionText: string, listSize: number): number {
  if (!positionText) return 0;
  const normalized = positionText.trim().toLowerCase();
  if (normalized === "first" || normalized === "1st") return 0;
  if (normalized === "last") return Math.max(0, listSize - 1);
  if (normalized === "second" || normalized === "2nd") return 1;
  if (normalized === "third" || normalized === "3rd") return 2;
  const digits = normalized.replace(/[^0-9]/g, "");
  if (digits.length > 0) return parseInt(digits, 10) - 1;
  return 0;
}

// --- Query / Finder Functions ---

export async function findVisualElementByText(screen: ScreenContext, text: string): Promise<any> {
  return await toVisualElementJson(screen.page.getByText(text), text);
}

export async function findVisualElementByImage(screen: ScreenContext, imagePaths: string[]): Promise<any> {
  return await toVisualElementJson(getLocatorByImage(screen, imagePaths), imagePaths[0]);
}

export async function findVisualElementByTextOrImage(screen: ScreenContext, text: string, imagePaths: string[]): Promise<any> {
  const loc = await resolveTextOrImageLocator(screen, text, imagePaths);
  return await toVisualElementJson(loc, text);
}

export async function findVisualElementByImageOrText(screen: ScreenContext, imagePaths: string[], text: string): Promise<any> {
  const loc = await resolveImageOrTextLocator(screen, imagePaths, text);
  return await toVisualElementJson(loc, text);
}

export async function findAllVisualElementsByText(screen: ScreenContext, text: string): Promise<any[]> {
  const all = await screen.page.getByText(text).all();
  return await toVisualElementJsonArray(all, text);
}

export async function findAllVisualElementsByImage(screen: ScreenContext, imagePaths: string[]): Promise<any[]> {
  const all = await getLocatorByImage(screen, imagePaths).all();
  return await toVisualElementJsonArray(all, imagePaths[0]);
}

export async function findAllVisualElementsByTextOrImage(screen: ScreenContext, text: string, imagePaths: string[]): Promise<any[]> {
  const textMatches = await screen.page.getByText(text).all();
  if (textMatches.length > 0) {
    return await toVisualElementJsonArray(textMatches, text);
  }
  const imageMatches = await getLocatorByImage(screen, imagePaths).all();
  return await toVisualElementJsonArray(imageMatches, imagePaths[0]);
}

export async function findAllVisualElementsByImageOrText(screen: ScreenContext, imagePaths: string[], text: string): Promise<any[]> {
  const imageMatches = await getLocatorByImage(screen, imagePaths).all();
  if (imageMatches.length > 0) {
    return await toVisualElementJsonArray(imageMatches, imagePaths[0]);
  }
  const textMatches = await screen.page.getByText(text).all();
  return await toVisualElementJsonArray(textMatches, text);
}

export async function findVisualElementRelativeByText(screen: ScreenContext, targetText: string, direction: any, anchorText: string): Promise<any> {
  return await toVisualElementJson(screen.page.getByText(targetText), targetText);
}

export async function findVisualElementRelativeByImage(screen: ScreenContext, imagePaths: string[], direction: any, anchorText: string): Promise<any> {
  return await toVisualElementJson(getLocatorByImage(screen, imagePaths), imagePaths[0]);
}

export async function findVisualElementByTextInRegion(screen: ScreenContext, text: string, region: any): Promise<any> {
  return await toVisualElementJson(screen.page.getByText(text), text);
}

export async function findVisualElementByImageInRegion(screen: ScreenContext, imagePaths: string[], region: any): Promise<any> {
  return await toVisualElementJson(getLocatorByImage(screen, imagePaths), imagePaths[0]);
}

// --- Action Functions ---

export async function clickVisualElementByText(screen: ScreenContext, elementName: string, ocrText: string) {
  await screen.page.getByText(ocrText).click();
  return stayOnCurrentScreen();
}

export async function clickVisualElementByImage(screen: ScreenContext, elementName: string, imagePaths: string[]) {
  await getLocatorByImage(screen, imagePaths).click();
  return stayOnCurrentScreen();
}

export async function clickVisualElementByTextOrImage(screen: ScreenContext, elementName: string, ocrText: string, imagePaths: string[]) {
  const loc = await resolveTextOrImageLocator(screen, ocrText, imagePaths);
  await loc.click();
  return stayOnCurrentScreen();
}

export async function clickVisualElementByImageOrText(screen: ScreenContext, elementName: string, imagePaths: string[], ocrText: string) {
  const loc = await resolveImageOrTextLocator(screen, imagePaths, ocrText);
  await loc.click();
  return stayOnCurrentScreen();
}

export async function enterTextIntoVisualElementByText(screen: ScreenContext, elementName: string, textToEnter: string, ocrText: string) {
  await screen.page.getByText(ocrText).fill(textToEnter);
  return stayOnCurrentScreen();
}

export async function inspectVisualElementByText(screen: ScreenContext, elementName: string, ocrText: string) {
  return stayOnCurrentScreen();
}

export async function inspectVisualElementByImage(screen: ScreenContext, elementName: string, imagePaths: string[]) {
  return stayOnCurrentScreen();
}

export async function doubleClickVisualElementByText(screen: ScreenContext, elementName: string, ocrText: string) {
  await screen.page.getByText(ocrText).dblclick();
  return stayOnCurrentScreen();
}

export async function hoverVisualElementByText(screen: ScreenContext, elementName: string, ocrText: string) {
  await screen.page.getByText(ocrText).hover();
  return stayOnCurrentScreen();
}

export async function longPressVisualElementByText(screen: ScreenContext, elementName: string, ocrText: string) {
  await screen.page.getByText(ocrText).click({ delay: 1000 });
  return stayOnCurrentScreen();
}

export async function swipeOnVisualElementByText(screen: ScreenContext, elementName: string, direction: any, ocrText: string) {
  return stayOnCurrentScreen();
}

export async function clickVisualElementAtIndexByText(screen: ScreenContext, elementName: string, index: number, ocrText: string) {
  const all = screen.page.getByText(ocrText);
  await all.nth(index).click();
  return stayOnCurrentScreen();
}

export async function clickVisualElementByPositionByText(screen: ScreenContext, elementName: string, positionText: string, ocrText: string) {
  const all = await screen.page.getByText(ocrText).all();
  const index = parsePositionIndex(positionText, all.length);
  await all[index].click();
  return stayOnCurrentScreen();
}

export async function clickVisualElementByPositionByImage(screen: ScreenContext, elementName: string, positionText: string, imagePaths: string[]) {
  const all = await getLocatorByImage(screen, imagePaths).all();
  const index = parsePositionIndex(positionText, all.length);
  await all[index].click();
  return stayOnCurrentScreen();
}

export async function clickVisualElementRelativeByText(screen: ScreenContext, elementName: string, targetText: string, direction: any, anchorText: string) {
  await screen.page.getByText(targetText).click();
  return stayOnCurrentScreen();
}

// --- Try Click Functions ---

export async function tryClickVisualElementByText(screen: ScreenContext, elementName: string, ocrText: string): Promise<boolean> {
  const loc = screen.page.getByText(ocrText);
  if (await loc.count() > 0) {
    await loc.click();
    return true;
  }
  return false;
}

export async function tryClickVisualElementByImage(screen: ScreenContext, elementName: string, imagePaths: string[]): Promise<boolean> {
  const loc = getLocatorByImage(screen, imagePaths);
  if (await loc.count() > 0) {
    await loc.click();
    return true;
  }
  return false;
}

export async function tryClickVisualElementByTextOrImage(screen: ScreenContext, elementName: string, ocrText: string, imagePaths: string[]): Promise<boolean> {
  const loc = await resolveTextOrImageLocator(screen, ocrText, imagePaths);
  if (await loc.count() > 0) {
    await loc.click();
    return true;
  }
  return false;
}

export async function tryClickVisualElementByImageOrText(screen: ScreenContext, elementName: string, imagePaths: string[], ocrText: string): Promise<boolean> {
  const loc = await resolveImageOrTextLocator(screen, imagePaths, ocrText);
  if (await loc.count() > 0) {
    await loc.click();
    return true;
  }
  return false;
}

export async function tryClickVisualElementRelativeByText(screen: ScreenContext, elementName: string, targetText: string, direction: any, anchorText: string): Promise<boolean> {
  const loc = screen.page.getByText(targetText);
  if (await loc.count() > 0) {
    await loc.click();
    return true;
  }
  return false;
}
