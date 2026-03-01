#include <Arduino.h>
#include <TFT_eSPI.h>

#define BACKLIGHT_PIN 27

// Casio retro color palette
#define CASIO_BG tft.color565(143, 188, 143) // Classic green-yellow LCD background
#define LCD_ON TFT_BLACK                     // Active pixel color (Solid Black)
#define LCD_OFF tft.color565(125, 170, 125)  // Faint pixel color (Ghosting effect)

TFT_eSPI tft = TFT_eSPI(); 

// Helper function to draw segmented Casio-style progress bar
void drawCasioBar(int x, int y, int w, int h, float percent) {
  int totalSegments = 20;
  // Cap the percentage at 100 to prevent drawing outside the bar
  if (percent > 100.0) percent = 100.0; 
  
  int activeSegments = (percent / 100.0) * totalSegments;
  int segmentWidth = (w / totalSegments) - 2;

  for(int i = 0; i < totalSegments; i++) {
     int segX = x + i * (segmentWidth + 2);
     if(i < activeSegments) {
        // Draw active segment (Black)
        tft.fillRect(segX, y, segmentWidth, h, LCD_ON);
     } else {
        // Draw inactive segment (Faint ghost green)
        tft.fillRect(segX, y, segmentWidth, h, LCD_OFF);
     }
  }
}

// Smart parser to extract value by key from pipe-separated string
String getValue(String data, String key) {
  int keyIndex = data.indexOf(key);
  if (keyIndex == -1) return "0"; 
  
  int valStart = keyIndex + key.length();
  int valEnd = data.indexOf('|', valStart);
  if (valEnd == -1) valEnd = data.length(); 
  
  return data.substring(valStart, valEnd);
}

void setup() {
  Serial.begin(115200);

  pinMode(BACKLIGHT_PIN, OUTPUT);
  digitalWrite(BACKLIGHT_PIN, HIGH);

  tft.init();
  tft.setRotation(1); 
  
  // Paint the entire screen with the Casio background color
  tft.fillScreen(CASIO_BG);

  // Draw a retro double-border around the screen
  tft.drawRect(5, 5, 470, 310, LCD_ON);
  tft.drawRect(7, 7, 466, 306, LCD_ON);

  // Setup text rendering for retro style
  tft.setTextColor(LCD_ON, CASIO_BG);
  tft.setTextSize(2);

  // Layout headers
  tft.setCursor(20, 20);
  tft.println("CPU :");

  tft.setCursor(20, 95);
  tft.println("RAM :");

  tft.setCursor(20, 170);
  tft.println("TEMP:");

  tft.setCursor(20, 245);
  tft.println("GPU :");
}

void loop() {
  if (Serial.available()) {
    String data = Serial.readStringUntil('\n');
    data.trim();

    // HANDSHAKE CHECK
    if (data == "PING_MONITOR") {
      Serial.println("ACK_MONITOR");
      return; 
    }

    // NORMAL DATA READING CHECK
    if (data.startsWith("C:")) {
      
      // Extract data using the smart parser
      float cpuLoad = getValue(data, "C:").toFloat();
      float ramLoad = getValue(data, "R:").toFloat();
      String totalRam = getValue(data, "TR:");
      String usedRam = getValue(data, "UR:");
      float tempValue = getValue(data, "T:").toFloat(); 
      String gpuName = getValue(data, "GN:");

      tft.setTextSize(2);
      char buf[32];

      // CPU Update
      sprintf(buf, "%5.1f %%", cpuLoad);
      tft.drawString(buf, 100, 20); 
      drawCasioBar(20, 45, 430, 25, cpuLoad);

      // RAM Update
      sprintf(buf, "%5.1f %%  (%s GB / %s GB)", ramLoad, usedRam.c_str(), totalRam.c_str());
      tft.drawString(buf, 100, 95);
      drawCasioBar(20, 120, 430, 25, ramLoad);

      // TEMP Update (Treats 100 Celsius as a full 100% bar)
      sprintf(buf, "%5.1f C", tempValue);
      tft.drawString(buf, 100, 170);
      drawCasioBar(20, 195, 430, 25, tempValue); 

      // GPU Update
      sprintf(buf, "%s                ", gpuName.c_str()); 
      tft.drawString(buf, 100, 245);
    }
  }
}