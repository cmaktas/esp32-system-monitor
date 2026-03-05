#include <Arduino.h>
#include <TFT_eSPI.h>
#include <math.h>
#include "bitmaps.h"

#define BACKLIGHT_PIN 27
#define RGB_HEX(hex) ((((hex >> 16) & 0xF8) << 8) | (((hex >> 8) & 0xFC) << 3) | ((hex & 0xF8) >> 3))

// THEME CHOICE: CASIO (GREEN) OR BIOS (BLUE)
enum Theme { CASIO, BIOS };
Theme currentTheme = BIOS; 

// --- CASIO COLOR PALETTE ---
#define CASIO_BG    RGB_HEX(0x22743F)
#define CASIO_ON    RGB_HEX(0x1E231E)
#define CASIO_OFF   RGB_HEX(0x19552D)

// --- BIOS COLOR PALETTE ---
#define BIOS_BLUE   RGB_HEX(0x0000AA) 
#define BIOS_WHITE  RGB_HEX(0xFFFFFF) 
#define BIOS_YELLOW RGB_HEX(0xFFFF55) 
#define BIOS_GRAY   RGB_HEX(0xAAAAAA) 

TFT_eSPI tft = TFT_eSPI(); 

unsigned long lastDataTime = 0;
const unsigned long timeoutMs = 2000; 
int animFrame = 0;
bool isIdle = true;

// ----------------------------------------------------------------------------
// --- HELPER FUNCTIONS ---
// ----------------------------------------------------------------------------
String getValue(String data, String key) {
  int keyIndex = data.indexOf(key);
  if (keyIndex == -1) return "0"; 
  int valStart = keyIndex + key.length();
  int valEnd = data.indexOf('|', valStart);
  if (valEnd == -1) valEnd = data.length(); 
  return data.substring(valStart, valEnd);
}

int getActiveCores(String clData) {
  int activeCount = 0;
  int startIdx = 0;
  int commaIdx = clData.indexOf(',');
  while (commaIdx != -1) {
    if (clData.substring(startIdx, commaIdx).toFloat() > 5.0) activeCount++;
    startIdx = commaIdx + 1;
    commaIdx = clData.indexOf(',', startIdx);
  }
  if (startIdx < clData.length() && clData.substring(startIdx).toFloat() > 5.0) activeCount++;
  return activeCount;
}

// ----------------------------------------------------------------------------
// --- BAR DRAWING MODULES ---
// ----------------------------------------------------------------------------
void drawCasioBar(int x, int y, int w, int h, float percent) {
  int totalSegments = 14; 
  if (percent > 100.0) percent = 100.0; 
  int activeSegments = (percent / 100.0) * totalSegments;
  int segmentWidth = (w / totalSegments) - 2;

  for(int i = 0; i < totalSegments; i++) {
     int segX = x + i * (segmentWidth + 2);
     tft.drawRect(segX, y, segmentWidth, h, CASIO_OFF); 
     if(i < activeSegments) {
        tft.fillRect(segX, y, segmentWidth, h, CASIO_ON);
     }
  }
}

void drawBiosBar(int x, int y, int w, int h, float percent) {
    if (percent > 100.0) percent = 100.0;
    int activeWidth = (percent / 100.0) * w;
    
    tft.drawRect(x, y, w, h, BIOS_WHITE);
    
    for (int i = 2; i < w - 2; i += 6) {
        if (i < activeWidth) {
            tft.fillRect(x + i, y + 2, 4, h - 4, BIOS_YELLOW);
        } else {
            tft.fillRect(x + i, y + 2, 4, h - 4, RGB_HEX(0x000055));
        }
    }
}

// ----------------------------------------------------------------------------
// --- IDLE SCREENS ---
// ----------------------------------------------------------------------------
void showCasioIdle() {
  tft.fillScreen(CASIO_BG);
  tft.setTextColor(CASIO_ON);
  
  int centerX = 240; 
  int centerY = 120; 

  tft.setTextSize(6);
  const char* frames[] = { "[ - _ - ]", "[ o _ o ]", "[ - _ - ]", "[ . _ . ]" };
  tft.setCursor(centerX - 135, centerY); 
  tft.print(frames[animFrame % 4]);
  
  tft.setTextSize(2);
  tft.setCursor(centerX - 65, centerY + 100); 
  tft.print("NO SIGNAL...");
}

void showBiosIdle() {
  tft.fillScreen(BIOS_BLUE);
  tft.setTextColor(BIOS_WHITE);
  
  tft.setTextSize(3);
  tft.setCursor(50, 50);
  tft.print("CMOS Checksum Error");
  
  tft.setCursor(50, 90);
  tft.print("Wait for Host Service...");
  
  if (animFrame % 2 == 0) {
      tft.fillRect(50, 130, 20, 30, BIOS_WHITE);
  }
  
  tft.setTextSize(2);
  tft.setCursor(50, 280);
  tft.print("F1 to continue, DEL to enter Setup");
}

// ----------------------------------------------------------------------------
// --- RENDER MODULES (SCREENS DRAWN WHEN DATA ARRIVES) ---
// ----------------------------------------------------------------------------

// 1. CASIO THEME
void renderCasio(float cpuLoad, int activeCores, String coreCount, int cpuMhz, float cpuWatts, float cpuTemp,
                 float gpuLoad, String usedGpuVram, String totalGpuVram, int gpuMhz, float gpuWatts, float gpuTemp,
                 float ramLoad, String usedRam, int roundedTotalRam) {
                     
  tft.setTextColor(CASIO_ON, CASIO_BG);
  char buf[64];

  // --- MODULE 1: CPU UNIT ---
  tft.setTextSize(4);
  tft.setCursor(20, 35); 
  tft.print("CPU");
  tft.setTextSize(2);
  sprintf(buf, "%4.1f%% (%d/%s)     ", cpuLoad, activeCores, coreCount.c_str());
  tft.setCursor(105, 30); tft.print(buf);
  sprintf(buf, "%dMHz %2.0fW     ", cpuMhz, cpuWatts);
  tft.setCursor(105, 55); tft.print(buf);
  tft.setTextSize(4);
  sprintf(buf, "%2.0f", cpuTemp);
  tft.drawString(buf, 350, 35);
  tft.setTextSize(2);
  tft.drawString("C", 410, 35);
  drawCasioBar(20, 85, 440, 15, cpuLoad);

  // --- MODULE 2: GPU UNIT ---
  tft.setTextSize(4);
  tft.setCursor(20, 135); 
  tft.print("GPU");
  tft.setTextSize(2);
  sprintf(buf, "%4.1f%% (%s/%sG)     ", gpuLoad, usedGpuVram.c_str(), totalGpuVram.c_str());
  tft.setCursor(105, 130); tft.print(buf);
  sprintf(buf, "%dMHz %2.0fW     ", gpuMhz, gpuWatts);
  tft.setCursor(105, 155); tft.print(buf);
  tft.setTextSize(4);
  sprintf(buf, "%2.0f", gpuTemp);
  tft.drawString(buf, 350, 135);
  tft.setTextSize(2);
  tft.drawString("C", 410, 135);
  drawCasioBar(20, 185, 440, 15, gpuLoad);

  // --- MODULE 3: RAM UNIT ---
  tft.setTextSize(4);
  tft.setCursor(20, 235); 
  tft.print("RAM");
  tft.setTextSize(2);
  sprintf(buf, "%4.1f%% (%s/%dG)          ", ramLoad, usedRam.c_str(), roundedTotalRam);
  tft.setCursor(105, 240); 
  tft.print(buf);
  drawCasioBar(20, 285, 440, 15, ramLoad);
}

// 2. BIOS THEME
void renderBios(float cpuLoad, int activeCores, String coreCount, int cpuMhz, float cpuWatts, float cpuTemp,
                float gpuLoad, String usedGpuVram, String totalGpuVram, int gpuMhz, float gpuWatts, float gpuTemp,
                float ramLoad, String usedRam, int roundedTotalRam) {
                    
  tft.setTextColor(BIOS_WHITE, BIOS_BLUE);
  char buf[64];

  // --- MODULE 1: CPU UNIT ---
  tft.setTextSize(4);
  tft.setCursor(20, 35); 
  tft.print("CPU");
  tft.setTextSize(2);
  sprintf(buf, "%4.1f%% (%d/%s)     ", cpuLoad, activeCores, coreCount.c_str());
  tft.setCursor(105, 30); tft.print(buf);
  sprintf(buf, "%dMHz %2.0fW     ", cpuMhz, cpuWatts);
  tft.setCursor(105, 55); tft.print(buf);
  
  tft.setTextColor(BIOS_YELLOW, BIOS_BLUE);
  tft.setTextSize(4);
  sprintf(buf, "%2.0f", cpuTemp);
  tft.drawString(buf, 350, 35);
  tft.setTextSize(2);
  tft.drawString("C", 410, 35);
  
  tft.setTextColor(BIOS_WHITE, BIOS_BLUE);
  drawBiosBar(20, 85, 440, 15, cpuLoad);

  // --- MODULE 2: GPU UNIT ---
  tft.setTextSize(4);
  tft.setCursor(20, 135); 
  tft.print("GPU");
  tft.setTextSize(2);
  sprintf(buf, "%4.1f%% (%s/%sG)     ", gpuLoad, usedGpuVram.c_str(), totalGpuVram.c_str());
  tft.setCursor(105, 130); tft.print(buf);
  sprintf(buf, "%dMHz %2.0fW     ", gpuMhz, gpuWatts);
  tft.setCursor(105, 155); tft.print(buf);
  
  tft.setTextColor(BIOS_YELLOW, BIOS_BLUE);
  tft.setTextSize(4);
  sprintf(buf, "%2.0f", gpuTemp);
  tft.drawString(buf, 350, 135);
  tft.setTextSize(2);
  tft.drawString("C", 410, 135);
  
  tft.setTextColor(BIOS_WHITE, BIOS_BLUE);
  drawBiosBar(20, 185, 440, 15, gpuLoad);

  // --- MODULE 3: RAM UNIT ---
  tft.setTextSize(4);
  tft.setCursor(20, 235); 
  tft.print("RAM");
  tft.setTextSize(2);
  sprintf(buf, "%4.1f%% (%s/%dG)          ", ramLoad, usedRam.c_str(), roundedTotalRam);
  tft.setCursor(105, 240); 
  tft.print(buf);
  
  drawBiosBar(20, 285, 440, 15, ramLoad);
}

// ----------------------------------------------------------------------------
// --- SETUP & LOOP ---
// ----------------------------------------------------------------------------
void setup() {
  Serial.begin(115200);
  
  // v2.x PWM SETUP FOR BACKLIGHT CONTROL
  ledcSetup(0, 5000, 8);
  ledcAttachPin(BACKLIGHT_PIN, 0); 
  ledcWrite(0, 255); 
  
  tft.init(); 
  tft.setRotation(3); 
  tft.fillScreen(currentTheme == BIOS ? BIOS_BLUE : CASIO_BG);
  lastDataTime = millis();
}

void loop() {
  if (Serial.available()) {
    String data = Serial.readStringUntil('\n');
    data.trim();

    if (data == "REQ_ESP32_SYSMON_v1_0x7A9B") {
      Serial.println("ACK_ESP32_SYSMON_v1_0x7A9B");
      return; 
    }

    if (data.startsWith("C:")) {
      if (isIdle) {
        tft.fillScreen(currentTheme == BIOS ? BIOS_BLUE : CASIO_BG);
        
        if(currentTheme == BIOS) {
            tft.drawRect(5, 5, 470, 310, BIOS_WHITE);
            tft.drawRect(7, 7, 466, 306, BIOS_WHITE);
        }
        isIdle = false;
      }
      
      lastDataTime = millis();
      
      // COMMON DATA PARSING
      float cpuLoad = getValue(data, "C:").toFloat();
      float ramLoad = getValue(data, "R:").toFloat();
      String usedRam = getValue(data, "UR:");
      float cpuTemp = getValue(data, "T:").toFloat();
      float gpuTemp = getValue(data, "GT:").toFloat();
      String totalGpuVram = getValue(data, "GV:");
      float gpuLoad = getValue(data, "GL:").toFloat();
      String usedGpuVram = getValue(data, "GU:");
      float cpuWatts = getValue(data, "CP:").toFloat();
      float gpuWatts = getValue(data, "GP:").toFloat();
      int cpuMhz = getValue(data, "CF:").toInt();
      int gpuMhz = getValue(data, "GF:").toInt();
      String coreCount = getValue(data, "N:");
      int activeCores = getActiveCores(getValue(data, "CL:"));
      int rawTotalRam = getValue(data, "TR:").toInt();
      int roundedTotalRam = ceil(rawTotalRam / 8.0) * 8; 

      // SELECTED THEME BASED RENDERING
      if (currentTheme == BIOS) {
          renderBios(cpuLoad, activeCores, coreCount, cpuMhz, cpuWatts, cpuTemp,
                     gpuLoad, usedGpuVram, totalGpuVram, gpuMhz, gpuWatts, gpuTemp,
                     ramLoad, usedRam, roundedTotalRam);
      } else {
          renderCasio(cpuLoad, activeCores, coreCount, cpuMhz, cpuWatts, cpuTemp,
                      gpuLoad, usedGpuVram, totalGpuVram, gpuMhz, gpuWatts, gpuTemp,
                      ramLoad, usedRam, roundedTotalRam);
      }
    }
  }

  // IDLE (TIMEOUT) STATE
  if (millis() - lastDataTime > timeoutMs) {
    isIdle = true;
    
    // Animation frame updater (every 500ms)
    static unsigned long lastAnimUpdate = 0;
    if (millis() - lastAnimUpdate > 500) {
        if (currentTheme == BIOS) {
            showBiosIdle();
        } else {
            showCasioIdle();
        }
        animFrame++;
        lastAnimUpdate = millis();
    }
  }
}