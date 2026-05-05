Build an Android app called "AutoTyper" with the following features:

## Core Purpose
The app maintains a numbered list of strings (e.g. phone numbers, codes, serial numbers) and can automatically type the selected item into any other Android app using the Accessibility Service — bypassing apps that block clipboard paste.

---

## Features Required

### 1. Main Screen (List Manager)
- A scrollable list where each item has a serial number (1, 2, 3…)
- Add items via a text input field + "Add" button
- Delete individual items with a swipe or delete icon
- Import a list by pasting multiple lines (one item per line) into a dialog
- Persist the list using Room database or SharedPreferences
- Highlight the "current" selected item

### 2. Floating Overlay (Always-on-Top Controls)
- A small draggable floating bubble/toolbar that stays visible over all apps
- Buttons: **◀ Prev**, **Type Now**, **Next ▶**
- Show the current serial number and a preview of the current value (truncated)
- Requires `SYSTEM_ALERT_WINDOW` permission (overlay permission)

### 3. Auto-Type Engine (Accessibility Service)
- Implement an `AccessibilityService` subclass
- When "Type Now" is pressed, use `performGlobalAction` or `AccessibilityNodeInfo.ACTION_SET_TEXT` to insert the current item's text into the focused text field of whatever app is in the foreground
- Fallback: simulate individual keystrokes using `dispatchGesture` or inject via `InputConnection` if direct set-text fails
- After typing, optionally auto-advance to next item (configurable toggle)

### 4. Settings Screen
- Toggle: Auto-advance to next after typing (yes/no)
- Toggle: Show floating overlay when app is in background
- Option to set a delay (ms) before typing starts (e.g. 500ms buffer)
- Clear all list items

### 5. Onboarding / Permission Flow
- On first launch, guide user step-by-step to enable:
  - a. Accessibility Service (deep link to Settings > Accessibility)
  - b. Display over other apps permission
- Show clear status indicators (enabled/disabled) for each permission on the main screen

---

## Tech Stack
- Language: **Kotlin**
- Min SDK: 26 (Android 8.0)
- Architecture: MVVM with ViewModel + LiveData
- Database: Room (for list persistence)
- UI: Material Design 3 components
- No external dependencies beyond AndroidX and Material

---

## Project Structure

```
app/
  src/main/
    java/com/autoTyper/
      MainActivity.kt
      FloatingOverlayService.kt
      AutoTyperAccessibilityService.kt
      ListViewModel.kt
      ItemDao.kt / ItemDatabase.kt
      SettingsActivity.kt
    res/layout/
      activity_main.xml
      overlay_controls.xml
    AndroidManifest.xml
```

---

## AndroidManifest Requirements

Include the following permissions and service declarations:

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE"/>

<service
    android:name=".AutoTyperAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService"/>
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config"/>
</service>

<service android:name=".FloatingOverlayService"/>
```

---

## Accessibility Config XML

Create `res/xml/accessibility_service_config.xml`:

```xml
<accessibility-service
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFlags="flagDefault"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_description" />
```

---

## Deliverable

Provide complete, compilable Kotlin source code for all files listed in the project structure. Include inline comments explaining how the Accessibility Service locates the focused node and types text into it.
