# Shri Ji Service Desk 📱

A modern Android application for managing customer details, electronic appliance repairs, service progress, and payments for **Shri Ji Electronics**.

Built with **Kotlin, Jetpack Compose, MVVM, and Room Database**, Shri Ji Service Desk replaces manual repair tracking with a simple, organized digital workflow.

---

## ✨ Features

### 🛠️ Repair Management
- Register new repair requests.
- Store appliance, brand, model, and reported problem details.
- Track repair progress through six predefined stages:
  - Received
  - Under Inspection
  - Problem Identified
  - Repair in Progress
  - Ready for Pickup
  - Completed
- View individual repair details and status history.
- Set estimated and final repair costs.
- Search and filter repair records.

### 👥 Customer Management
- Maintain customer profiles and contact details.
- View customer repair history.
- Quickly call customers using the device dialer.
- Open WhatsApp conversations directly from customer profiles.

### 💬 WhatsApp Notifications
- Prepare customer notifications when repair statuses change.
- Generate pre-filled WhatsApp messages containing customer and repair information.
- Open WhatsApp with the message ready for review.
- Send messages manually after confirmation.

### 💰 Payment Management
- Record payments using UPI, cash, card, or bank transfer.
- Track partial payments and outstanding balances.
- Maintain payment records for individual repairs.
- Generate UPI QR codes with the payment amount pre-filled.
- Verify received payments manually before counting them as collected.
- Track pending and verified payments.

### 📊 Dashboard
- View total repairs and active jobs.
- Monitor repairs ready for pickup.
- Track verified collections and outstanding payments.
- Access recent repair activity and quick actions.

### 🎨 User Interface
- Premium, modern interface built with Jetpack Compose.
- Vibrant purple, violet, mint, coral, and blue colour palette.
- Animated bottom navigation and screen transitions.
- Interactive cards and bottom sheets.
- Swipe-based navigation between application sections.

---

## 🧰 Tech Stack

| Technology | Purpose |
|---|---|
| Kotlin | Primary programming language |
| Jetpack Compose | Declarative UI development |
| Material 3 | UI components and design system |
| MVVM | Application architecture |
| Room Database | Local data persistence |
| Kotlin Coroutines & Flow | Asynchronous operations and reactive data |
| ViewModel | UI state management |
| ZXing | QR code generation |
| Android Intents | Calling, WhatsApp, and UPI interactions |

---

## 🏗️ Architecture

The application follows the **Model–View–ViewModel (MVVM)** architecture.

```text
com.example.shrijiservicedesk
│
├── data/
│   ├── Entity classes
│   ├── DAO interfaces
│   ├── Room Database
│   └── Repository
│
├── viewmodel/
│   └── RepairViewModel
│
├── ui/
│   ├── components/
│   │   └── UpiQrCode
│   └── theme/
│
└── MainActivity.kt
```

### Application Flow

```text
Jetpack Compose UI
        |
        v
    ViewModel
        |
        v
    Repository
        |
        v
    Room Database
        |
        v
   Local Storage
```

The UI observes application data through Kotlin Flow, while the ViewModel coordinates UI operations and data access.

---

## 💳 Payment Workflow

1. Enter the payment amount.
2. Select the payment method.
3. For UPI, display a QR code containing the shop's UPI details and amount.
4. The customer scans the QR code using their UPI application.
5. Check the actual credit in the shop's bank or UPI account.
6. Manually verify the payment in the application.

**Important:** Displaying a QR code or initiating a UPI payment does not confirm that money has been received. Payment verification is intentionally manual.

---

## 🚀 Getting Started

### Prerequisites

- Android Studio
- Android SDK
- JDK compatible with the project's Gradle configuration
- Android device or emulator

### Installation

1. Clone the repository:

```bash
git clone https://github.com/YOUR_USERNAME/shri-ji-service-desk.git
```

2. Open the cloned project in Android Studio.

3. Allow Gradle to sync and download the required dependencies.

4. Configure the shop's UPI ID if necessary.

5. Run the application on an Android emulator or physical device.

---

## ⚙️ Configuration

The shop details are currently defined in the application source:

```kotlin
private const val SHOP_NAME = "Shri Ji Electronics"
private const val SHOP_UPI_ID = "YOUR_UPI_ID"
```

Replace the UPI ID with the actual receiving UPI ID before using the payment QR feature.

---

## 🔒 Data & Privacy

- Application records are stored locally using Room Database.
- No cloud synchronization or online account system is currently implemented.
- WhatsApp communication is initiated through Android intents.
- Payments are not automatically verified by the application.

Keep regular backups of important repair and customer records.

---

## 🛣️ Future Improvements

Potential enhancements include:

- Cloud backup and synchronization.
- Automated customer notification workflows.
- Printable repair receipts and invoices.
- PDF generation for payment and repair reports.
- Advanced business analytics.
- Role-based access for multiple employees.
- Database export and restore functionality.

---

## 🎯 Project Objective

The goal of Shri Ji Service Desk is to simplify everyday operations at a local electronics repair shop by bringing customer management, repair tracking, and payment records into one application.

It demonstrates the practical application of modern Android development concepts, including Jetpack Compose, MVVM, Room Database, Kotlin Flow, and Android intents.

---

## 👨‍💻 Developer

**Developed for Shri Ji Electronics**

Built with ❤️ using Kotlin and Jetpack Compose.

---

*If you find this project useful or interesting, consider giving the repository a ⭐ on GitHub.*
