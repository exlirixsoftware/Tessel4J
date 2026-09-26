package net.tessel.laf.win;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;

/** The handful of user32.dll functions jna-platform's {@code User32} doesn't declare. */
interface Win32Ext extends StdCallLibrary {

    Win32Ext INSTANCE = Native.load("user32", Win32Ext.class);

    boolean IsZoomed(HWND hWnd);

    HWND SetCapture(HWND hWnd);

    boolean ReleaseCapture();
}
