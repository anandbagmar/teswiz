import type { ScreenContext } from "../screen-context.ts";

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

export async function findVisualElementByText(screen: ScreenContext, text: string): Promise<any> {
  return await toVisualElementJson(screen.page.getByText(text), text);
}

export async function findVisualElementByImage(screen: ScreenContext, imagePaths: string[]): Promise<any> {
  return await toVisualElementJson(screen.page.locator(`img[src*="${imagePaths[0]}"]`), imagePaths[0]);
}

export async function findVisualElementByTextOrImage(screen: ScreenContext, text: string, imagePaths: string[]): Promise<any> {
  const textLocator = screen.page.getByText(text);
  if ((await textLocator.count()) > 0) {
    return await toVisualElementJson(textLocator, text);
  }
  return await toVisualElementJson(screen.page.locator(`img[src*="${imagePaths[0]}"]`), imagePaths[0]);
}

export async function findVisualElementByImageOrText(screen: ScreenContext, imagePaths: string[], text: string): Promise<any> {
  const imageLocator = screen.page.locator(`img[src*="${imagePaths[0]}"]`);
  if ((await imageLocator.count()) > 0) {
    return await toVisualElementJson(imageLocator, imagePaths[0]);
  }
  return await toVisualElementJson(screen.page.getByText(text), text);
}

export async function findAllVisualElementsByText(screen: ScreenContext, text: string): Promise<any[]> {
  const all = await screen.page.getByText(text).all();
  return await toVisualElementJsonArray(all, text);
}

export async function findAllVisualElementsByImage(screen: ScreenContext, imagePaths: string[]): Promise<any[]> {
  const all = await screen.page.locator(`img[src*="${imagePaths[0]}"]`).all();
  return await toVisualElementJsonArray(all, imagePaths[0]);
}

export async function findAllVisualElementsByTextOrImage(screen: ScreenContext, text: string, imagePaths: string[]): Promise<any[]> {
  const textMatches = await screen.page.getByText(text).all();
  if (textMatches.length > 0) {
    return await toVisualElementJsonArray(textMatches, text);
  }
  const imageMatches = await screen.page.locator(`img[src*="${imagePaths[0]}"]`).all();
  return await toVisualElementJsonArray(imageMatches, imagePaths[0]);
}

export async function findAllVisualElementsByImageOrText(screen: ScreenContext, imagePaths: string[], text: string): Promise<any[]> {
  const imageMatches = await screen.page.locator(`img[src*="${imagePaths[0]}"]`).all();
  if (imageMatches.length > 0) {
    return await toVisualElementJsonArray(imageMatches, imagePaths[0]);
  }
  const textMatches = await screen.page.getByText(text).all();
  return await toVisualElementJsonArray(textMatches, text);
}

export async function findVisualElementRelativeByText(screen: ScreenContext, targetText: string, direction: string, anchorText: string): Promise<any> {
  return await toVisualElementJson(screen.page.getByText(targetText), targetText);
}

export async function findVisualElementRelativeByImage(screen: ScreenContext, imagePaths: string[], direction: string, anchorText: string): Promise<any> {
  return await toVisualElementJson(screen.page.locator(`img[src*="${imagePaths[0]}"]`), imagePaths[0]);
}

export async function findVisualElementByTextInRegion(screen: ScreenContext, text: string, region: any): Promise<any> {
  return await toVisualElementJson(screen.page.getByText(text), text);
}

export async function findVisualElementByImageInRegion(screen: ScreenContext, imagePaths: string[], region: any): Promise<any> {
  return await toVisualElementJson(screen.page.locator(`img[src*="${imagePaths[0]}"]`), imagePaths[0]);
}
