import type { ScreenContext } from "../screen-context.ts";

export async function navigateTo(screen: ScreenContext, url: string): Promise<void> {
  await screen.page.goto(url, { waitUntil: "domcontentloaded" });
}

export async function scrollToExploreRouteMap(screen: ScreenContext): Promise<void> {
  const exploreHeading = screen.page.getByText("Explore the new route").first();
  await exploreHeading.scrollIntoViewIfNeeded();
}

export async function clickElementByImage(screen: ScreenContext, elementName: string, imagePath: string): Promise<any> {
  const imageElement = screen.page.locator(`img[src*="${imagePath}"]`).first();
  await imageElement.click();
  const box = await imageElement.boundingBox();
  return {
    x: Math.round(box?.x ?? 0),
    y: Math.round(box?.y ?? 0),
    width: Math.round(box?.width ?? 100),
    height: Math.round(box?.height ?? 30),
    label: elementName,
  };
}

export async function clickElementByOcrText(screen: ScreenContext, elementName: string, ocrText: string): Promise<any> {
  const textElement = screen.page.getByText(ocrText).first();
  await textElement.click();
  const box = await textElement.boundingBox();
  return {
    x: Math.round(box?.x ?? 0),
    y: Math.round(box?.y ?? 0),
    width: Math.round(box?.width ?? 100),
    height: Math.round(box?.height ?? 30),
    label: elementName,
  };
}

export async function clickCalloutOptionByText(screen: ScreenContext, text: string): Promise<any> {
  const option = screen.page.getByText(text).first();
  await option.click();
  const box = await option.boundingBox();
  return {
    x: Math.round(box?.x ?? 0),
    y: Math.round(box?.y ?? 0),
    width: Math.round(box?.width ?? 100),
    height: Math.round(box?.height ?? 30),
    label: text,
  };
}

export async function clickStationNameOnMapByText(screen: ScreenContext, stationName: string): Promise<any> {
  const stationElement = screen.page.getByText(stationName).first();
  await stationElement.click();
  const box = await stationElement.boundingBox();
  return {
    x: Math.round(box?.x ?? 0),
    y: Math.round(box?.y ?? 0),
    width: Math.round(box?.width ?? 100),
    height: Math.round(box?.height ?? 30),
    label: stationName,
  };
}

export async function isStationPageDisplayedFor(screen: ScreenContext, stationName: string): Promise<boolean> {
  const textLocator = screen.page.getByText(stationName, { exact: false });
  return (await textLocator.count()) > 0;
}
