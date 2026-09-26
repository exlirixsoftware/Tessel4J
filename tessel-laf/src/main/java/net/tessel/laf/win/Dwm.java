package net.tessel.laf.win;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import java.util.Arrays;
import java.util.List;

/** Minimal dwmapi.dll bindings; jna-platform ships no Dwmapi wrapper. */
interface Dwm extends StdCallLibrary {

    Dwm INSTANCE = Native.load("dwmapi", Dwm.class);

    int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
    int DWMWA_USE_IMMERSIVE_DARK_MODE_PRE_20H1 = 19;
    int DWMWA_WINDOW_CORNER_PREFERENCE = 33;
    int DWMWCP_ROUND = 2;

    int DwmExtendFrameIntoClientArea(HWND hwnd, Margins margins);

    int DwmSetWindowAttribute(HWND hwnd, int attribute, IntByReference value, int size);

    /**
     * dwmapi MARGINS. All-negative ("sheet of glass") extends composited glass across the entire
     * client area, which is the textbook way to get a shadow on a borderless window - but Java's
     * hardware-accelerated (Direct3D) Java2D pipeline visibly corrupts LCD-antialiased text once the
     * whole window is glass (stale/fragmented glyphs after any large repaint, e.g. a theme switch).
     * A single top pixel of glass is the documented minimal footprint that still makes DWM treat the
     * window as shadow-eligible, without extending glass compositing across the actual content.
     */
    class Margins extends Structure {
        public int cxLeftWidth;
        public int cxRightWidth;
        public int cyTopHeight = 1;
        public int cyBottomHeight;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight");
        }
    }
}
