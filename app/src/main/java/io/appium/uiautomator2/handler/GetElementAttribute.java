package io.appium.uiautomator2.handler;

import android.support.test.uiautomator.UiObjectNotFoundException;

import org.json.JSONObject;

import io.appium.uiautomator2.handler.request.SafeRequestHandler;
import io.appium.uiautomator2.http.AppiumResponse;
import io.appium.uiautomator2.http.IHttpRequest;
import io.appium.uiautomator2.model.AndroidElement;
import io.appium.uiautomator2.model.KnownElements;
import io.appium.uiautomator2.server.WDStatus;
import io.appium.uiautomator2.utils.Logger;

public class GetElementAttribute extends SafeRequestHandler {

    public GetElementAttribute(String mappedUri) {
        super(mappedUri);
    }

    @Override
    public AppiumResponse safeHandle(IHttpRequest request) {
        Logger.info("get attribute of element command");
        String id = getElementId(request);
        String attributeName = getNameAttribute(request);
        AndroidElement element = KnownElements.getElementFromCache(id);
        if (element == null) {
            return new AppiumResponse(getSessionId(request), WDStatus.NO_SUCH_ELEMENT, "Element Not found");
        }
        Object text = JSONObject.NULL;
        try {
            text = element.getStringAttribute(attributeName);
        } catch (UiObjectNotFoundException e) {
            Logger.error("Element not found: ", e);
            return new AppiumResponse(getSessionId(request), WDStatus.NO_SUCH_ELEMENT, e);
        }
        return new AppiumResponse(getSessionId(request), WDStatus.SUCCESS, text);
    }
}
