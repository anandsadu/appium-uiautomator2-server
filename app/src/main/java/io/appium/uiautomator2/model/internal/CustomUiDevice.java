package io.appium.uiautomator2.model.internal;

import android.app.Instrumentation;
import android.os.Build;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject2;
import android.support.test.uiautomator.UiSelector;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.appium.uiautomator2.common.exceptions.ElementNotFoundException;
import io.appium.uiautomator2.common.exceptions.InvalidSelectorException;
import io.appium.uiautomator2.common.exceptions.UiAutomator2Exception;
import io.appium.uiautomator2.utils.Device;
import io.appium.uiautomator2.utils.Logger;
import io.appium.uiautomator2.utils.ReflectionUtils;

import static io.appium.uiautomator2.utils.Device.getUiDevice;
import static io.appium.uiautomator2.utils.ReflectionUtils.getField;
import static io.appium.uiautomator2.utils.ReflectionUtils.invoke;
import static io.appium.uiautomator2.utils.ReflectionUtils.method;

public class CustomUiDevice {

    private static final String FIELD_M_INSTRUMENTATION = "mInstrumentation";
    private static final String FIELD_API_LEVEL_ACTUAL = "API_LEVEL_ACTUAL";

    private static CustomUiDevice INSTANCE = new CustomUiDevice();
    private final Method METHOD_FIND_MATCH;
    private final Method METHOD_FIND_MATCHS;
    private final Class ByMatcher;
    private final Instrumentation mInstrumentation;
    private final Object API_LEVEL_ACTUAL;
    private final UiDevice device;

    /**
     * UiDevice in android open source project will Support multi-window searches for API level 21,
     * which has not been implemented in UiAutomatorViewer capture layout hierarchy, to be in sync
     * with UiAutomatorViewer customizing getWindowRoots() method to skip the multi-window search
     * based user passed property
     */
    public CustomUiDevice() {
        try {
            device = Device.getUiDevice();

            this.mInstrumentation = (Instrumentation) getField(UiDevice.class, FIELD_M_INSTRUMENTATION, device);
            this.API_LEVEL_ACTUAL = getField(UiDevice.class, FIELD_API_LEVEL_ACTUAL, device);
            METHOD_FIND_MATCH = method("android.support.test.uiautomator.ByMatcher", "findMatch", UiDevice.class, BySelector.class, AccessibilityNodeInfo[].class);
            METHOD_FIND_MATCHS = method("android.support.test.uiautomator.ByMatcher", "findMatches", UiDevice.class, BySelector.class, AccessibilityNodeInfo[].class);

            ByMatcher = ReflectionUtils.getClass("android.support.test.uiautomator" + ".ByMatcher");
        } catch (Error error) {
            Logger.error("ERROR", "error", error);
            throw error;
        } catch (UiAutomator2Exception error) {
            Logger.error("ERROR", "error", error);
            throw new Error(error);
        }
    }

    public static CustomUiDevice getInstance() {
        return INSTANCE;
    }


    /**
     * Returns the first object to match the {@code selector} criteria.
     */
    public Object findObject(Object selector) throws ClassNotFoundException, ElementNotFoundException, InvalidSelectorException, UiAutomator2Exception {

        if (selector instanceof BySelector) {
            AccessibilityNodeInfo node = (AccessibilityNodeInfo) invoke(METHOD_FIND_MATCH, ByMatcher, device, selector, getWindowRoots(false));
            if (node != null) {
                try {
                    Class uiObject2 = Class.forName("android.support.test.uiautomator" + ".UiObject2");
                    Constructor cons = uiObject2.getDeclaredConstructors()[0];
                    cons.setAccessible(true);
                    Object[] constructorParams = {device, selector, node};
                    return (UiObject2) cons.newInstance(constructorParams);
                } catch (InvocationTargetException e) {
                    final String msg = String.format("error while creating  UiObject2 object");
                    Logger.error(msg + " " + e);
                    throw new UiAutomator2Exception(msg, e);
                } catch (InstantiationException e) {
                    final String msg = String.format("error while creating  UiObject2 object");
                    Logger.error(msg + " " + e);
                    throw new UiAutomator2Exception(msg, e);
                } catch (IllegalAccessException e) {
                    final String msg = String.format("error while creating  UiObject2 object");
                    Logger.error(msg + " " + e);
                    throw new UiAutomator2Exception(msg, e);
                }
            } else {
                return null;
            }
        } else if (selector instanceof UiSelector) {
            return getUiDevice().findObject((UiSelector) selector);
        } else {
            throw new InvalidSelectorException("Selector of type " + selector.getClass().getName() + " not supported");
        }
    }

    /**
     * Returns List<object> to match the {@code selector} criteria.
     */
    public List<Object> findObjects(Object selector) throws ClassNotFoundException, InvalidSelectorException, UiAutomator2Exception {

        List<Object> ret = new ArrayList<>();
        if (selector instanceof BySelector) {
            ReflectionUtils.getClass("android.support.test.uiautomator.ByMatcher");

            Object nodes = invoke(METHOD_FIND_MATCHS, ByMatcher, device, selector, getWindowRoots(false));

            ArrayList<AccessibilityNodeInfo> list = (ArrayList) nodes;
            for (Object node : list) {
                try {
                    Class uiObject2 = Class.forName("android.support.test.uiautomator" + ".UiObject2");
                    Constructor cons = uiObject2.getDeclaredConstructors()[0];
                    cons.setAccessible(true);
                    Object[] constructorParams = {device, selector, node};
                    ret.add((UiObject2) cons.newInstance(constructorParams));
                } catch (InvocationTargetException e) {
                    final String msg = String.format("error while creating  UiObject2 object:");
                    Logger.error(msg + " " + e);
                    throw new UiAutomator2Exception(msg, e);
                } catch (InstantiationException e) {
                    final String msg = String.format("error while creating  UiObject2 object");
                    Logger.error(msg + " " + e);
                    throw new UiAutomator2Exception(msg, e);
                } catch (IllegalAccessException e) {
                    final String msg = String.format("error while creating  UiObject2 object");
                    Logger.error(msg + " " + e);
                    throw new UiAutomator2Exception(msg, e);
                }
            }
        } else {
            throw new InvalidSelectorException("Selector of type " + selector.getClass().getName() + " not supported");
        }
        return ret;
    }

    /**
     * Returns a list containing the root {@link AccessibilityNodeInfo}s for each active window
     */
    AccessibilityNodeInfo[] getWindowRoots(boolean multiWindow) throws UiAutomator2Exception {
        device.waitForIdle();
        ArrayList<AccessibilityNodeInfo> ret = new ArrayList<>();
        // Support multi-window searches for API level 21 and up
        //TODO: need to handle multiWindows param better way
        if ((Integer) API_LEVEL_ACTUAL >= Build.VERSION_CODES.LOLLIPOP && multiWindow) {
            for (AccessibilityWindowInfo window : mInstrumentation.getUiAutomation().getWindows()) {
                AccessibilityNodeInfo root = window.getRoot();

                if (root == null) {
                    Logger.debug(String.format("Skipping null root node for " + "window: %s", window.toString()));
                    continue;
                }
                ret.add(root);
            }
            // Prior to API level 21 we can only access the active window
        } else {
            AccessibilityNodeInfo node = mInstrumentation.getUiAutomation().getRootInActiveWindow();
            if (node != null) {
                ret.add(node);
            } else {
                /*TODO: As we can't proceed to find element with out root node,
                 TODO: retrying for 5 times to get the root node if UiTestAutomationBridge reruns null
                 TODO: need to handle gracefully*/
                //AccessibilityNodeInfo should not be null.
                int retryCount = 0;
                while (node == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                    device.waitForIdle();
                    Logger.debug(" ERROR: null root node returned by UiTestAutomationBridge, retrying: " + retryCount);
                    node = mInstrumentation.getUiAutomation().getRootInActiveWindow();
                    retryCount++;
                    if (node != null) {
                        ret.add(node);
                        break;
                    } else if (retryCount > 5) {
                        throw new UiAutomator2Exception("Unable to get Root in Active window," +
                                " ERROR: null root node returned by UiTestAutomationBridge.");
                    }
                }
            }
        }
        return ret.toArray(new AccessibilityNodeInfo[ret.size()]);
    }
}
