package com.znsio.teswiz.web.playwright.screen;

import org.json.JSONArray;
import org.json.JSONObject;

import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Visual;
import com.znsio.teswiz.web.playwright.PlaywrightWebDriver;

import java.lang.reflect.Method;
import java.util.List;

public final class PlaywrightTsScreenActionExecutor {
    private static final String SCREEN_ROUTE = "teswizScreenRoute";
    private static final String CURRENT_SCREEN = "current";
    private static final String NO_SCREEN = "none";
    private static final String UNSUPPORTED_SCREEN = "unsupported";
    private static final String MESSAGE = "message";

    private final Class<?> screenContract;
    private final Driver driver;
    private final Visual visually;
    private final String screenModule;

    public PlaywrightTsScreenActionExecutor(Class<?> screenContract, Driver driver, Visual visually,
            String screenModule) {
        this.screenContract = screenContract;
        this.driver = driver;
        this.visually = visually;
        this.screenModule = screenModule;
    }

    @net.bytebuddy.implementation.bind.annotation.RuntimeType
    public Object intercept(
            @net.bytebuddy.implementation.bind.annotation.This Object currentScreen,
            @net.bytebuddy.implementation.bind.annotation.Origin Method method,
            @net.bytebuddy.implementation.bind.annotation.AllArguments Object[] arguments) {
        Object actionResult = playwrightDriver().runScreenAction(screenModule, method.getName(), toJsonArray(arguments));
        visually.checkWindow(screenContract.getSimpleName(), method.getName());
        return adaptResult(currentScreen, method.getReturnType(), actionResult);
    }

    private PlaywrightWebDriver playwrightDriver() {
        return (PlaywrightWebDriver) driver.getInnerDriver();
    }

    private JSONArray toJsonArray(Object[] arguments) {
        JSONArray values = new JSONArray();
        if (null == arguments) {
            return values;
        }
        for (Object argument : arguments) {
            values.put(argument);
        }
        return values;
    }

    private Object adaptResult(Object currentScreen, Class<?> returnType, Object actionResult) {
        if (Void.TYPE.equals(returnType)) {
            return null;
        }
        if (isScreenContract(returnType)) {
            return adaptScreenResult(currentScreen, returnType, actionResult);
        }
        if (com.znsio.teswiz.runner.VisualElement.class.isAssignableFrom(returnType)) {
            return adaptVisualElement(actionResult);
        }
        if (List.class.isAssignableFrom(returnType) && actionResult instanceof JSONArray jsonArray) {
            List<?> rawList = jsonArray.toList();
            if (!rawList.isEmpty() && rawList.get(0) instanceof java.util.Map<?, ?>) {
                return rawList.stream()
                        .map(item -> adaptVisualElement(new JSONObject((java.util.Map<?, ?>) item)))
                        .toList();
            }
            return rawList;
        }
        return actionResult;
    }

    private com.znsio.teswiz.runner.VisualElement adaptVisualElement(Object actionResult) {
        if (actionResult instanceof JSONObject json) {
            int x = json.optInt("x", 0);
            int y = json.optInt("y", 0);
            int width = json.optInt("width", 100);
            int height = json.optInt("height", 30);
            String label = json.optString("label", json.optString("text", "visual-element"));
            return new com.znsio.teswiz.runner.VisualElement(x, y, width, height, label, driver);
        }
        return new com.znsio.teswiz.runner.VisualElement(0, 0, 100, 30, "visual-element", driver);
    }

    private Object adaptScreenResult(Object currentScreen, Class<?> returnType, Object actionResult) {
        if (actionResult instanceof JSONObject routeInstruction) {
            String route = routeInstruction.optString(SCREEN_ROUTE);
            if (UNSUPPORTED_SCREEN.equals(route)) {
                throw new UnsupportedOperationException(routeInstruction.optString(MESSAGE,
                        "Unsupported playwright-ts screen action for " + screenContract.getSimpleName()));
            }
            if (NO_SCREEN.equals(route)) {
                return null;
            }
            if (CURRENT_SCREEN.equals(route)) {
                return currentScreen;
            }
        }
        if (returnType.isInstance(currentScreen)) {
            return currentScreen;
        }
        return resolveScreen(returnType);
    }

    private boolean isScreenContract(Class<?> returnType) {
        return returnType.getPackageName().startsWith("com.znsio.teswiz.screen");
    }

    private Object resolveScreen(Class<?> returnType) {
        try {
            Class<?> screenRegistry = Class.forName("com.znsio.teswiz.screen.ScreenRegistry");
            return screenRegistry.getMethod("getScreen", Class.class).invoke(null, returnType);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to resolve screen contract: " + returnType.getName(), exception);
        }
    }
}
