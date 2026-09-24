import type { ScreenContext } from "../screen-context.ts";

export async function addCookie(
  screen: ScreenContext,
  key: string,
  value: string,
): Promise<void> {
  const url = screen.page.url() && screen.page.url() !== "about:blank"
    ? screen.page.url()
    : "http://localhost";
  await screen.context.addCookies([{ name: key, value: value, url }]);
}

export async function isCookiePresent(
  screen: ScreenContext,
  key: string,
): Promise<boolean> {
  const cookies = await screen.context.cookies();
  return cookies.some((c) => c.name === key);
}

export async function deleteCookie(
  screen: ScreenContext,
  key: string,
): Promise<void> {
  const cookies = await screen.context.cookies();
  const remaining = cookies.filter((c) => c.name !== key);
  await screen.context.clearCookies();
  if (remaining.length > 0) {
    await screen.context.addCookies(remaining);
  }
}

export async function setViewport(
  screen: ScreenContext,
  width: number,
  height: number,
): Promise<void> {
  await screen.page.setViewportSize({ width, height });
}

export async function getViewportSize(
  screen: ScreenContext,
): Promise<number[]> {
  const size = screen.page.viewportSize();
  return size ? [size.width, size.height] : [0, 0];
}

export async function executeAsyncScript(
  screen: ScreenContext,
  delayMs: number,
  returnVal: string,
): Promise<unknown> {
  return await screen.page.evaluate(
    ({ delay, val }) =>
      new Promise((resolve) => setTimeout(() => resolve(val), delay)),
    { delay: delayMs, val: returnVal },
  );
}
