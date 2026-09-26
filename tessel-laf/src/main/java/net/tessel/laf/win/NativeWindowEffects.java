package net.tessel.laf.win;

import com.sun.jna.CallbackReference;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;

/**
 * Gives an undecorated Swing window real Windows chrome: a DWM drop shadow, Windows 11 rounded
 * corners, a theme-aware system menu, and native non-client hit-testing so dragging the title bar,
 * resizing from the edges and Win+Arrow / drag-to-edge snap all behave like a native app instead of
 * being faked with {@code setBounds}.
 *
 * <p>The window keeps painting its own title bar and buttons; this only takes over the invisible
 * hit-testing (and the DWM attributes that depend on real caption/frame styles existing). The
 * maximize/minimize/close buttons are deliberately kept as plain client ({@code HTCLIENT}) content,
 * handled by ordinary Swing clicks - an earlier version reported {@code HTMAXBUTTON} for the maximize
 * button to opt into Windows 11's Snap Layout hit-test protocol, but that consistently caused trouble
 * that outweighed the flyout-on-hover nicety it bought: DWM takes over pressing/releasing/toggling
 * maximize for that region itself once you report it (racing this code's own click handling so clicks
 * appeared to do nothing), and independently draws its own native button glyph as an overlay during
 * the press (a stray icon with no clean way found to suppress it). Drag-to-edge and Win+Arrow snap
 * both still work without it, since they only depend on the window having WS_MAXIMIZEBOX/WS_THICKFRAME.
 *
 * <p>No-op on non-Windows platforms, or if anything about the install fails - callers keep whatever
 * pure-Java drag/resize behavior they had before.
 */
public final class NativeWindowEffects {

    public enum Hit { CAPTION, MAX_BUTTON, MIN_BUTTON, CLOSE_BUTTON, LEFT, RIGHT, TOP, BOTTOM, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    /**
     * Supplies everything the native message-pump thread needs to know about the window.
     *
     * <p><b>Every method here is called from the native window-procedure thread, not the EDT.</b>
     * Windows sends {@code WM_NCHITTEST} on essentially every mouse move (over client content too,
     * not just non-client - it's the first step of hit-testing for any mouse move at all), so
     * implementations must answer from plain, pre-cached fields only - never call a live Swing/AWT
     * getter (bounds, minimum size, component state, coordinate conversion, ...) from these methods.
     * Doing so risks contending with the EDT for AWT's tree lock and hanging the whole application;
     * cache the values on the EDT instead (e.g. from a layout or state-change listener) and read the
     * cache here.
     */
    public interface FrameModel {
        /**
         * Classifies a window-local point. Return {@link Hit#MAX_BUTTON} / {@link Hit#MIN_BUTTON} /
         * {@link Hit#CLOSE_BUTTON} for those buttons (they still get {@code HTCLIENT} and ordinary
         * Swing clicks - this is only used to drive their hover visual, see {@link #onButtonHover})
         * and null for other client content.
         */
        Hit hitTest(int x, int y);

        /** The frame's current Swing minimum size, or a non-positive {@link Dimension} for "no limit". */
        Dimension resizeMinimumSize();

        /**
         * The currently-hovered title bar button changed: one of {@code MAX_BUTTON}/{@code MIN_BUTTON}/
         * {@code CLOSE_BUTTON}, or null for none. Fired on essentially every mouse move (see the class
         * javadoc), so implementations should resync the button's actual Swing hover state (e.g.
         * {@code button.getModel().setRollover(...)}) rather than layer another flag on top - all three
         * buttons are plain {@code HTCLIENT} and also receive ordinary Swing mouse events, but under
         * fast real mouse movement Swing's own rollover tracking can occasionally desync (miss a leave);
         * this is a continuous ground-truth resync that self-corrects that regardless of cause, rather
         * than one more signal that would need reconciling against Swing's.
         */
        void onButtonHover(Hit hoveredButton);
    }

    private static final int GWLP_WNDPROC = -4;
    private static final int WM_NCCALCSIZE = 0x0083;
    private static final int WM_NCHITTEST = 0x0084;
    private static final int WM_NCPAINT = 0x0085;
    private static final int WM_NCMOUSELEAVE = 0x02A2;
    private static final int WM_NCUAHDRAWCAPTION = 0x00AE;
    private static final int WM_NCUAHDRAWFRAME = 0x00AF;
    private static final int WM_GETMINMAXINFO = 0x0024;

    private static final int HTCLIENT = 1;
    private static final int HTCAPTION = 2;
    private static final int HTLEFT = 10;
    private static final int HTRIGHT = 11;
    private static final int HTTOP = 12;
    private static final int HTTOPLEFT = 13;
    private static final int HTTOPRIGHT = 14;
    private static final int HTBOTTOM = 15;
    private static final int HTBOTTOMLEFT = 16;
    private static final int HTBOTTOMRIGHT = 17;

    private final Window window;
    private final FrameModel model;
    private final User32 user32 = User32.INSTANCE;

    private HWND hwnd;
    private Pointer originalProc;
    private WinUser.WindowProc proc;
    private Hit hoverButton;

    private NativeWindowEffects(Window window, FrameModel model) {
        this.window = window;
        this.model = model;
    }

    /**
     * Installs native chrome on {@code window}, which must already be displayable (call from
     * {@code addNotify()}, after {@code super.addNotify()}). Returns null on non-Windows platforms
     * or if the native hook could not be installed.
     */
    public static NativeWindowEffects install(Window window, FrameModel model) {
        if (!Platform.isWindows()) return null;
        NativeWindowEffects fx = new NativeWindowEffects(window, model);
        try {
            fx.attach();
            return fx;
        } catch (Throwable t) {
            return null;
        }
    }

    private void attach() {
        Pointer p = Native.getWindowPointer(window);
        if (p == null) throw new IllegalStateException("no native window handle yet");
        hwnd = new HWND(p);

        // Install the hook BEFORE changing styles / forcing a frame change: SWP_FRAMECHANGED below
        // triggers a synchronous WM_NCCALCSIZE, and whichever proc is current at that moment decides
        // the window's non-client geometry. Doing this the other way around leaves a real native
        // caption painted (from AWT's original proc, which knows nothing about our zero-inset scheme)
        // until something else happens to force a second recalculation, e.g. the user maximizing.
        proc = this::windowProc;
        originalProc = user32.SetWindowLongPtr(hwnd, GWLP_WNDPROC, CallbackReference.getFunctionPointer(proc));

        boolean resizable = !(window instanceof Frame) || ((Frame) window).isResizable();
        int style = user32.GetWindowLong(hwnd, WinUser.GWL_STYLE);
        style |= WinUser.WS_CAPTION | WinUser.WS_SYSMENU;
        if (resizable) style |= WinUser.WS_THICKFRAME | WinUser.WS_MAXIMIZEBOX | WinUser.WS_MINIMIZEBOX;
        user32.SetWindowLong(hwnd, WinUser.GWL_STYLE, style);
        user32.SetWindowPos(hwnd, null, 0, 0, 0, 0, WinUser.SWP_NOMOVE | WinUser.SWP_NOSIZE
                | WinUser.SWP_NOZORDER | WinUser.SWP_NOACTIVATE | WinUser.SWP_FRAMECHANGED);

        Dwm.INSTANCE.DwmExtendFrameIntoClientArea(hwnd, new Dwm.Margins());
        Dwm.INSTANCE.DwmSetWindowAttribute(hwnd, Dwm.DWMWA_WINDOW_CORNER_PREFERENCE, new IntByReference(Dwm.DWMWCP_ROUND), 4);
    }

    /** Call after a theme switch so the system menu / caption chrome matches light or dark. */
    public void setDarkMode(boolean dark) {
        if (hwnd == null) return;
        IntByReference value = new IntByReference(dark ? 1 : 0);
        if (Dwm.INSTANCE.DwmSetWindowAttribute(hwnd, Dwm.DWMWA_USE_IMMERSIVE_DARK_MODE, value, 4) != 0) {
            Dwm.INSTANCE.DwmSetWindowAttribute(hwnd, Dwm.DWMWA_USE_IMMERSIVE_DARK_MODE_PRE_20H1, value, 4);
        }
    }

    /**
     * Forces the OS to recomposite the entire window from scratch. Because the window is extended
     * into full "sheet of glass" ({@link #attach}), DWM's composition surface can fall out of sync
     * with Swing's own incremental (dirty-region) repaints after a large all-at-once appearance
     * change - e.g. a theme switch - leaving stale pixels from the previous paint behind. A plain
     * {@code Component.repaint()} doesn't fix this since it's still just an incremental repaint from
     * Swing's perspective; this bypasses that and asks Windows to redraw the whole window immediately.
     */
    public void forceFullRepaint() {
        if (hwnd == null) return;
        user32.RedrawWindow(hwnd, null, null, new DWORD(WinUser.RDW_INVALIDATE | WinUser.RDW_ERASE
                | WinUser.RDW_FRAME | WinUser.RDW_ALLCHILDREN | WinUser.RDW_UPDATENOW));
    }

    /** Restores the original window procedure; call once, from the window's dispose path. */
    public void uninstall() {
        if (hwnd != null && originalProc != null) {
            user32.SetWindowLongPtr(hwnd, GWLP_WNDPROC, originalProc);
            originalProc = null;
        }
    }

    private LRESULT windowProc(HWND hWnd, int msg, WPARAM wParam, LPARAM lParam) {
        try {
            switch (msg) {
                // Legacy "user API hook" non-client drawing; suppressed defensively so nothing ever
                // paints a native caption/frame fragment over our own chrome.
                case WM_NCPAINT:
                case WM_NCUAHDRAWCAPTION:
                case WM_NCUAHDRAWFRAME:
                    return new LRESULT(0);
                case WM_NCCALCSIZE:
                    if (wParam.intValue() != 0) {
                        if (Win32Ext.INSTANCE.IsZoomed(hWnd)) deflateForMaximized(lParam);
                        return new LRESULT(0);
                    }
                    break;
                case WM_NCHITTEST:
                    return new LRESULT(hitTest(lParam));
                case WM_NCMOUSELEAVE:
                    setHoverButton(null);
                    break;
                case WM_GETMINMAXINFO: {
                    LRESULT r = user32.CallWindowProc(originalProc, hWnd, msg, wParam, lParam);
                    applyMinTrackSize(lParam);
                    return r;
                }
                default:
                    break;
            }
        } catch (Throwable ignored) {
            // never let a Java exception escape into the native message loop
        }
        return user32.CallWindowProc(originalProc, hWnd, msg, wParam, lParam);
    }

    /** Resolves the hit-test point purely via Win32 (no Swing/AWT calls - this runs off the EDT). */
    private int hitTest(LPARAM lParam) {
        long v = lParam.longValue();
        int screenX = (short) (v & 0xFFFF);
        int screenY = (short) ((v >> 16) & 0xFFFF);

        RECT windowRect = new RECT();
        user32.GetWindowRect(hwnd, windowRect);
        int x = screenX - windowRect.left;
        int y = screenY - windowRect.top;

        Hit hit = model.hitTest(x, y);
        // Moving within the window (even between non-client and client regions, or between two
        // client buttons) always re-triggers WM_NCHITTEST - since it's the first hit-testing step for
        // literally any mouse move - which is what keeps the hover resync below current. No separate
        // leave notification is needed for in-window transitions; WM_NCMOUSELEAVE (below) only covers
        // the cursor leaving the window entirely while still directly over the caption/resize edges.
        boolean specialButton = hit == Hit.MAX_BUTTON || hit == Hit.MIN_BUTTON || hit == Hit.CLOSE_BUTTON;
        setHoverButton(specialButton ? hit : null);
        // All three buttons are plain HTCLIENT - see the class javadoc for why the maximize button
        // isn't reported as HTMAXBUTTON despite that being the "normal" way to do this.
        if (hit == null || specialButton) return HTCLIENT;
        switch (hit) {
            case CAPTION: return HTCAPTION;
            case LEFT: return HTLEFT;
            case RIGHT: return HTRIGHT;
            case TOP: return HTTOP;
            case BOTTOM: return HTBOTTOM;
            case TOP_LEFT: return HTTOPLEFT;
            case TOP_RIGHT: return HTTOPRIGHT;
            case BOTTOM_LEFT: return HTBOTTOMLEFT;
            case BOTTOM_RIGHT: return HTBOTTOMRIGHT;
            default: return HTCLIENT;
        }
    }

    private void setHoverButton(Hit hit) {
        if (hit == hoverButton) return;
        hoverButton = hit;
        model.onButtonHover(hit);
    }

    /** Fixes the well-known "maximized window overhangs the monitor" artifact of a zero non-client area. */
    private void deflateForMaximized(LPARAM lParam) {
        RECT rect = Structure.newInstance(RECT.class, new Pointer(lParam.longValue()));
        rect.read();
        int fx = user32.GetSystemMetrics(WinUser.SM_CXSIZEFRAME) + user32.GetSystemMetrics(WinUser.SM_CXPADDEDBORDER);
        int fy = user32.GetSystemMetrics(WinUser.SM_CYSIZEFRAME) + user32.GetSystemMetrics(WinUser.SM_CXPADDEDBORDER);
        rect.left += fx;
        rect.right -= fx;
        rect.top += fy;
        rect.bottom -= fy;
        rect.write();
    }

    /** Keeps native edge-resize from shrinking the window past its (cached) Swing minimum size. */
    private void applyMinTrackSize(LPARAM lParam) {
        Dimension min = model.resizeMinimumSize();
        if (min.width <= 0 && min.height <= 0) return;
        MinMaxInfo info = Structure.newInstance(MinMaxInfo.class, new Pointer(lParam.longValue()));
        info.read();
        if (min.width > 0) info.ptMinTrackSize.x = min.width;
        if (min.height > 0) info.ptMinTrackSize.y = min.height;
        info.write();
    }

    /** Layout-compatible prefix of Win32's MINMAXINFO. */
    public static class MinMaxInfo extends Structure {
        public POINT ptReserved;
        public POINT ptMaxSize;
        public POINT ptMaxPosition;
        public POINT ptMinTrackSize;
        public POINT ptMaxTrackSize;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("ptReserved", "ptMaxSize", "ptMaxPosition", "ptMinTrackSize", "ptMaxTrackSize");
        }
    }
}
