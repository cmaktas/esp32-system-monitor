# 📊 ESP32 System Monitor

This project is a hardware monitoring solution that tracks system performance (CPU, RAM, GPU, etc.) and visualizes the data on an external ESP32-based TFT display via USB serial communication.

Featuring a highly decoupled Spring Boot backend daemon and a beautifully crafted C++ firmware, it offers real-time tracking with distinct retro UI themes.

## ✨ Key Features
* **Dual Retro Themes:** Toggle between a vintage **CASIO-style** green LCD interface and a classic **Award BIOS** blue screen.
* **Smart Idle Animations:** Features non-blocking, state-machine driven POST screen animations and "NO SIGNAL" modes when the host is disconnected.
* **True Windows Daemon:** Runs completely in the background as a native Windows Service (`LocalSystem`) using WinSW.
* **Deep Sensor Integration:** Bypasses standard limitations by fusing `OSHI` (RAM/Load), `nvidia-smi` (GPU), and `LibreHardwareMonitor` WMI (precise CPU die temperatures).
* **Auto-Discovery:** Concurrent serial port handshake protocol to automatically find and connect to the ESP32.

## 🏗️ System Architecture

The project consists of two main components acting as a monorepo:

1. **Host Service (`/host-service`)**: A Java Spring Boot application built with SOLID principles. It utilizes an isolated Provider architecture (`CpuMetricsProvider`, `GpuMetricsProvider`, etc.) to gather cross-platform and Windows-specific metrics. It uses `jSerialComm` to stream formatted payloads over USB and is wrapped as a Windows Service.
2. **Device Firmware (`/device-firmware`)**: C++ firmware written using PlatformIO for an ESP32-32E module. It parses incoming serial data and renders the metrics on a 4.0-inch ST7796S TFT display using `TFT_eSPI`.

## 🚀 Hardware Requirements
* ESP32-32E Module
* 4.0-inch TFT Display (ST7796S Driver)
* Type-C USB Cable
* Windows PC (Host)

## 🛠️ Tech Stack
* **Backend:** Java 21, Spring Boot, OSHI, jSerialComm, WinSW, PowerShell (WMI)
* **Embedded:** C++, PlatformIO, TFT_eSPI
* **External Tools:** LibreHardwareMonitor, NVIDIA System Management Interface (`nvidia-smi`)

## ⚙️ Prerequisites & Setup (Windows)

To get accurate hardware readings (especially modern CPU core temperatures and GPU stats), the host relies on standard system tools:

1. **NVIDIA GPU:** Ensure NVIDIA drivers are installed (adds `nvidia-smi` to your system PATH).
2. **LibreHardwareMonitor (LHM):** * Install via winget: `winget install LibreHardwareMonitor.LibreHardwareMonitor`
    * Open LHM as **Administrator**.
    * Go to Options and enable: **Run on Windows Startup**, **Start Minimized**, and **Run WMI Server**.

## 📦 Deployment

The project includes a 1-click deployment script that safely stops the old service, performs a clean Maven build, and installs the fresh daemon.

1. Navigate to the `/host-service` directory.
2. Right-click `run.bat` and select **Run as Administrator**.
3. The service `ESP32MonitorHost` will now run in the background automatically, even after reboots.

**Logs:**
* Service Logs: `/host-service/logs/winsw-logs/`
* Application Logs: `/host-service/logs/hardware-monitor.log`