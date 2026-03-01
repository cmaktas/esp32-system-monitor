# 📊 ESP32 System Monitor

This project is a hardware monitoring solution that tracks system performance (CPU, RAM, GPU, etc.) and visualizes the data on an external ESP32-based TFT display via USB serial communication.

## 🏗️ System Architecture

The project consists of two main components acting as a monorepo:

1. **Host Service (`/host-service`)**: A lightweight Java Spring Boot background daemon. It uses the `OSHI` library to read cross-platform system metrics and `jSerialComm` to stream this data over USB.
2. **Device Firmware (`/device-firmware`)**: C++ firmware written using PlatformIO for an ESP32-32E module. It receives the serial data string and renders the metrics on a 4.0-inch ST7796S TFT display.

## 🚀 Hardware Requirements
* ESP32-32E Module
* 4.0-inch TFT Display (ST7796S Driver)
* Type-C USB Cable
* Mac/Windows PC (Host)

## 🛠️ Tech Stack
* **Backend:** Java 21, Spring Boot, OSHI, jSerialComm
* **Embedded:** C++, PlatformIO, TFT_eSPI