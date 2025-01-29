# SingleViewApp

A productivity app that uses **Accessibility Services** to monitor and control app usage. The app helps users block distracting apps based on customizable timers and provides visual indicators for usage attempts and blocked apps.

## Features

- Monitor foreground apps using **Accessibility Services**.
- Block distracting apps by sending the user to the home screen.
- Configure timers (1, 5, or 10 minutes) to temporarily allow app usage.
- Usage count for each button to manage app access efficiently.
- Visual feedback for blocked app attempts.
- A dropdown menu to select apps to block.
- One simple screen layout for intuitive usage.
- Counter for attempts to open blocked apps.
- The app does not collect any information, ensuring complete privacy.
- Highly efficient and lightweight, using minimal resources.

---

## How It Works

### Accessibility Service

- Monitors `TYPE_WINDOW_STATE_CHANGED` events to detect the currently foregrounded app.
  - This is achieved using the attribute `android:accessibilityEventTypes="typeWindowStateChanged"`, which specifies that the service will listen to events related to window state changes, such as when an app is opened or switched. This is crucial for identifying the currently active app and determining if it is in the blocklist.
- Checks if the app is in the user-defined blocklist.
- If the app is blocked and the global timer is inactive, it sends the user to the home screen using `performGlobalAction(GLOBAL_ACTION_HOME)`.

### UI Features

- **Block One**: Displays a counter for attempts to open blocked apps and an active timer countdown.
- **Block Two**: Allows users to select apps to block and manage the blocklist via dropdown menus.
- **Dynamic Layouts**: Adjusts seamlessly between portrait and landscape modes.

---

## Getting Started

1. [Download the APK here](https://github.com/stupakzm/SingleViewApp/blob/main/singleviewapp.apk) and install it on your device.
2. Enable the Accessibility Service for the app (see below).

---

## Usage

1. **Enable Accessibility Service**:

   - Go to **Settings → Accessibility → Installed Services**.
   - Select **SingleViewApp** and enable it.

2. **If Service is Disabled Due to Restricted Settings**:

   - Some devices may restrict enabling Accessibility Services for apps installed from external sources.
   - To resolve this:
     1. Go to **Settings → Apps → SingleViewApp → Special Access**.
     2. Enable **Allow Restricted Settings**.

3. **Select Apps to Block**:

   - Use the dropdown menu in the app to select apps you want to block.

4. **Set Timers**:

   - Tap one of the timer buttons (1 minute, 5 minutes, or 10 minutes) to temporarily allow app usage.

5. **Visual Feedback**:

   - View blocked attempts and the countdown timer in the app's interface.

---

## License

This project is licensed under the [Apache License 2.0](LICENSE).

